package iudx.rs.proxy.authenticator;

import static iudx.rs.proxy.authenticator.Constants.JSON_EXPIRY;
import static iudx.rs.proxy.authenticator.Constants.JSON_IID;
import static iudx.rs.proxy.authenticator.Constants.JSON_USERID;
import static iudx.rs.proxy.authenticator.Constants.OPEN_ENDPOINTS;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import iudx.rs.proxy.authenticator.authorization.Api;
import iudx.rs.proxy.authenticator.authorization.AuthorizationContextFactory;
import iudx.rs.proxy.authenticator.authorization.AuthorizationRequest;
import iudx.rs.proxy.authenticator.authorization.AuthorizationStrategy;
import iudx.rs.proxy.authenticator.authorization.IudxRole;
import iudx.rs.proxy.authenticator.authorization.JwtAuthorization;
import iudx.rs.proxy.authenticator.authorization.Method;
import iudx.rs.proxy.authenticator.model.JwtData;
import iudx.rs.proxy.cache.CacheService;
import iudx.rs.proxy.cache.cacheImpl.CacheType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JwtAuthenticationServiceImpl implements AuthenticationService {

  private static final Logger LOGGER = LogManager.getLogger(JwtAuthenticationServiceImpl.class);

  final JWTAuth jwtAuth;
  final String audience;
  final CacheService cache;

  JwtAuthenticationServiceImpl(
      Vertx vertx,
      final JWTAuth jwtAuth,
      final JsonObject config,
      final CacheService cacheService) {
    this.jwtAuth = jwtAuth;
    this.audience = config.getString("host");
    this.cache = cacheService;
  }

  @Override
  public AuthenticationService tokenIntrospect(
      JsonObject request, JsonObject authenticationInfo, Handler<AsyncResult<JsonObject>> handler) {
  LOGGER.info("AUTH started.");
    String id = authenticationInfo.getString("id");
    String token = authenticationInfo.getString("token");

    Future<JwtData> jwtDecodeFuture = decodeJwt(token);

    ResultContainer result = new ResultContainer();

    jwtDecodeFuture
        .compose(
            decodeHandler -> {
              result.jwtData = decodeHandler;
              return isValidAudienceValue(result.jwtData);
            })
        .compose(
            audienceHandler -> {
              if (!result.jwtData.getIss().equals(result.jwtData.getSub())) {
                return isOpenResource(id);
              } else {
                return Future.succeededFuture("OPEN");
              }
            })
        .compose(
            openResourceHandler -> {
              result.isOpen = openResourceHandler.equalsIgnoreCase("OPEN");
              if (result.jwtData.getIss().equals(result.jwtData.getSub())) {
                JsonObject jsonResponse = new JsonObject();
                jsonResponse.put(JSON_USERID, result.jwtData.getSub());
                jsonResponse.put(
                    JSON_EXPIRY,
                    (LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(
                                Long.parseLong(result.jwtData.getExp().toString())),
                            ZoneId.systemDefault()))
                        .toString());
                return Future.succeededFuture(jsonResponse);
              } else {
                return validateAccess(result.jwtData, result.isOpen, authenticationInfo);
              }
            })
        .onSuccess(
            successHandler -> {
              handler.handle(Future.succeededFuture(successHandler));
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error("error : " + failureHandler.getMessage());
              handler.handle(Future.failedFuture(failureHandler.getMessage()));
            });
    return this;
  }

  Future<JwtData> decodeJwt(String jwtToken) {
    Promise<JwtData> promise = Promise.promise();
    TokenCredentials creds = new TokenCredentials(jwtToken);

    jwtAuth
        .authenticate(creds)
        .onSuccess(
            user -> {
              JwtData jwtData = new JwtData(user.principal());
              jwtData.setExp(user.get("exp"));
              jwtData.setIat(user.get("iat"));
              promise.complete(jwtData);
            })
        .onFailure(
            err -> {
              LOGGER.error("failed to decode/validate jwt token : " + err.getMessage());
              promise.fail("failed");
            });

    return promise.future();
  }

  private Future<String> isOpenResource(String id) {
    String[] idComponents = id.split("/");
    String groupId =
        (idComponents.length == 4) ? id : String.join("/", Arrays.copyOfRange(idComponents, 0, 4));
    LOGGER.trace("isOpenResource() started");
    Promise<String> promise = Promise.promise();
    CacheType cacheType = CacheType.RESOURCE_ID;
    JsonObject requestJson = new JsonObject().put("type", cacheType).put("key", groupId);

    cache.get(
        requestJson,
        checkResourceIdHandler -> {
          if (checkResourceIdHandler.succeeded()) {
            LOGGER.debug("Cache Hit");
            JsonObject responseJson = checkResourceIdHandler.result();
            LOGGER.info("cache hit "+responseJson);
            String ACL = responseJson.getString("value");
            LOGGER.info("ACL "+ACL);
            if (ACL != null) promise.complete(ACL);
          }

        });
    // cache miss
    LOGGER.debug("Cache miss calling cat server");
    idComponents = id.split("/");
    if (idComponents.length < 4) {
      promise.fail("Not Found " + id);
    }
     groupId =
        (idComponents.length == 4) ? id : String.join("/", Arrays.copyOfRange(idComponents, 0, 4));
    // 1. check group accessPolicy.
    // 2. check resource exist, if exist set accessPolicy to group accessPolicy. else fail
    Future<String> groupACLFuture = getGroupAccessPolicy(groupId);
    groupACLFuture
        .compose(
            groupACLResult -> {
              String groupPolicy = groupACLResult;
              return isResourceExist(id, groupPolicy);
            })
        .onSuccess(
            handler -> {
              cache.get(
                  requestJson,
                  checkResourceIdHandler -> {
                    if (checkResourceIdHandler.succeeded()) {
                      JsonObject responseJson = checkResourceIdHandler.result();
                      String ACL = responseJson.getString("value");
                      if (ACL != null) promise.complete(ACL);
                    }
                  });
            })
        .onFailure(
            handler -> {
              LOGGER.error("cat response failed for Id : (" + id + ")" + handler.getCause());
              promise.fail("Not Found " + id);
            });

    return promise.future();
  }

  public Future<JsonObject> validateAccess(
      JwtData jwtData, boolean openResource, JsonObject authInfo) {
    LOGGER.trace("validateAccess() started");
    Promise<JsonObject> promise = Promise.promise();
    String jwtId = jwtData.getIid().split(":")[1];

    if (openResource && OPEN_ENDPOINTS.contains(authInfo.getString("apiEndpoint"))) {
      LOGGER.info("User access is allowed.");
      JsonObject jsonResponse = new JsonObject();
      jsonResponse.put(JSON_IID, jwtId);
      jsonResponse.put(JSON_USERID, jwtData.getSub());
      return Future.succeededFuture(jsonResponse);
    }

    Method method = Method.valueOf(authInfo.getString("method"));
    Api api = Api.fromEndpoint(authInfo.getString("apiEndpoint"));
    AuthorizationRequest authRequest = new AuthorizationRequest(method, api);

    IudxRole role = IudxRole.fromRole(jwtData.getRole());
    AuthorizationStrategy authStrategy = AuthorizationContextFactory.create(role);
    LOGGER.info("strategy : " + authStrategy.getClass().getSimpleName());
    JwtAuthorization jwtAuthStrategy = new JwtAuthorization(authStrategy);
    LOGGER.info("auth strategy "+jwtAuthStrategy);
    LOGGER.info("endPoint : " + authInfo.getString("apiEndpoint"));
    if (jwtAuthStrategy.isAuthorized(authRequest, jwtData)) {
      LOGGER.info("User access is allowed.");
      JsonObject jsonResponse = new JsonObject();
      jsonResponse.put(JSON_USERID, jwtData.getSub());
      jsonResponse.put(JSON_IID, jwtId);
      jsonResponse.put(
          JSON_EXPIRY,
          (LocalDateTime.ofInstant(
                  Instant.ofEpochSecond(Long.parseLong(jwtData.getExp().toString())),
                  ZoneId.systemDefault()))
              .toString());
      promise.complete(jsonResponse);
    } else {
      LOGGER.error("failed - no access provided to endpoint");
      JsonObject result = new JsonObject().put("401", "no access provided to endpoint");
      promise.fail(result.toString());
    }
    return promise.future();
  }

  Future<Boolean> isValidAudienceValue(JwtData jwtData) {
    Promise<Boolean> promise = Promise.promise();
    if (audience != null && audience.equalsIgnoreCase(jwtData.getAud())) {
      promise.complete(true);
    } else {
      LOGGER.error("Incorrect audience value in jwt");
      promise.fail("Incorrect audience value in jwt");
    }
    return promise.future();
  }

  Future<Boolean> isValidId(JwtData jwtData, String id) {
    Promise<Boolean> promise = Promise.promise();
    String jwtId = jwtData.getIid().split(":")[1];
    if (id.equalsIgnoreCase(jwtId)) {
      promise.complete(true);
    } else {
      LOGGER.error("Incorrect id value in jwt");
      promise.fail("Incorrect id value in jwt");
    }

    return promise.future();
  }

  private Future<Boolean> isResourceExist(String id, String groupACL) {
    LOGGER.info("isResourceExist() started");
    Promise<Boolean> promise = Promise.promise();
    CacheType cacheType = CacheType.RESOURCE_ID;
    JsonObject requestJson =
        new JsonObject().put("type", cacheType).put("key", id).put("groupACL", groupACL);

    cache.get(
        requestJson,
        getACLHandler -> {
          if (getACLHandler.succeeded()) {
            JsonObject responseJson = getACLHandler.result();
            LOGGER.debug("responseJson : " + responseJson);
            String resourceACL = responseJson.getString("value");
            if (resourceACL != null) {
              LOGGER.info("Info : cache Hit");
              promise.complete(true);
            }
          } else if (getACLHandler.failed()){
            cache.refresh(requestJson,updateCacheHandler->{
              if(updateCacheHandler.succeeded()){
                promise.complete(true);
              }else promise.fail("false");
            });
          }
        });
    return promise.future();
  }

  private Future<String> getGroupAccessPolicy(String groupId) {
    LOGGER.info("getGroupAccessPolicy() started "+ groupId);
    Promise<String> promise = Promise.promise();
    CacheType cacheType = CacheType.RESOURCE_GROUP;
    JsonObject requestJson = new JsonObject().put("type", cacheType).put("key", groupId);

    cache.get(
        requestJson,
        getACLHandler -> {
          if (getACLHandler.succeeded()) {
            JsonObject responseJson = getACLHandler.result();
            LOGGER.debug("responseJson : " + responseJson);
            String resourceACL = responseJson.getString("value");
            promise.complete(resourceACL);
          } else if (getACLHandler.failed()) {
            promise.fail("Resource not found");
          }
        });

    return promise.future();
  }

  // class to contain intermediate data for token introspection
  final class ResultContainer {
    JwtData jwtData;
    boolean isResourceExist;
    boolean isOpen;
  }
}
