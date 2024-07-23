package iudx.rs.proxy.authenticator;

import static iudx.rs.proxy.apiserver.util.ApiServerConstants.PII;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.PPB_NUMBER;
import static iudx.rs.proxy.authenticator.Constants.*;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import iudx.rs.proxy.authenticator.authorization.*;
import iudx.rs.proxy.authenticator.model.JwtData;
import iudx.rs.proxy.cache.CacheService;
import iudx.rs.proxy.cache.cacheImpl.CacheType;
import iudx.rs.proxy.common.Api;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JwtAuthenticationServiceImpl implements AuthenticationService {

  private static final Logger LOGGER = LogManager.getLogger(JwtAuthenticationServiceImpl.class);
  final JWTAuth jwtAuth;
  final String audience;
  final CacheService cache;
  final Api apis;

  JwtAuthenticationServiceImpl(
      Vertx vertx,
      final JWTAuth jwtAuth,
      final JsonObject config,
      final CacheService cacheService,
      final Api apis) {
    this.jwtAuth = jwtAuth;
    this.audience = config.getString("audience");
    this.apis = apis;
    this.cache = cacheService;
  }

  @Override
  public AuthenticationService tokenIntrospect(
      JsonObject request,
      JsonObject authenticationInfo,
      JwtData jwtData,
      Handler<AsyncResult<JsonObject>> handler) {
    LOGGER.info("tokenIntrospect() started");
    String id = authenticationInfo.getString("id");
    String ppbNumber = authenticationInfo.getString(PPB_NUMBER, "");

    ResultContainer result = new ResultContainer();
    result.jwtData = jwtData;
    String endPoint = authenticationInfo.getString("apiEndpoint");
    boolean skipResourceIdCheck =
        endPoint.equalsIgnoreCase(apis.getAsyncStatusEndpoint())
            || endPoint.equalsIgnoreCase(apis.getConsumerAuditEndpoint())
            || endPoint.equalsIgnoreCase(apis.getProviderAuditEndpoint())
            || endPoint.equalsIgnoreCase(apis.getOverviewEndPoint())
            || endPoint.equalsIgnoreCase(apis.getSummaryEndPoint());
    ;

    Future<Boolean> audienceFuture = isValidAudienceValue(jwtData);
    audienceFuture
        .compose(
            audienceHandler -> {
              if (!result.jwtData.getIss().equals(result.jwtData.getSub())) {
                return isRevokedClientToken(result.jwtData);
              } else {
                return Future.succeededFuture(true);
              }
            })
        .compose(
            revokeTokenHandler -> {
              if (!skipResourceIdCheck
                  && !result.jwtData.getIss().equals(result.jwtData.getSub())) {
                return isOpenResource(id);
              } else {
                return Future.succeededFuture("OPEN");
              }
            })
        .compose(
            openResourceHandler -> {
              LOGGER.debug("isOpenResource messahe {}", openResourceHandler);
              result.isOpen = openResourceHandler.equalsIgnoreCase("OPEN");
              if (result.isOpen && checkOpenEndPoints(endPoint)) {
                JsonObject json = new JsonObject();
                json.put(JSON_USERID, result.jwtData.getSub());
                return Future.succeededFuture(true);
              } else if (!skipResourceIdCheck && !result.isOpen) {
                return isValidId(result.jwtData, id, ppbNumber, openResourceHandler);
              } else {
                return Future.succeededFuture(true);
              }
            })
        .compose(
            validIdHandler -> {
              if (result.jwtData.getIss().equals(result.jwtData.getSub())) {
                JsonObject jsonResponse = new JsonObject();
                jsonResponse.put(JSON_USERID, result.jwtData.getSub());
                jsonResponse.put(
                    JSON_EXPIRY,
                    LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(
                                Long.parseLong(result.jwtData.getExp().toString())),
                            ZoneId.systemDefault())
                        .toString());
                jsonResponse.put(ROLE, result.jwtData.getRole());
                jsonResponse.put(DRL, result.jwtData.getDrl());
                jsonResponse.put(DID, result.jwtData.getDid());
                jsonResponse.put(JSON_APD, result.jwtData.getApd());
                jsonResponse.put(JSON_CONS, result.jwtData.getCons());
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

  Future<Boolean> isRevokedClientToken(JwtData jwtData) {
    Promise<Boolean> promise = Promise.promise();
    CacheType cacheType = CacheType.REVOKED_CLIENT;
    String subId = jwtData.getSub();
    JsonObject requestJson = new JsonObject().put("type", cacheType).put("key", subId);
    Future<JsonObject> cacheCallFuture = cache.get(requestJson);
    cacheCallFuture
        .onSuccess(
            successHandler -> {
              JsonObject responseJson = successHandler;

              String timestamp = responseJson.getString("value");

              LocalDateTime revokedAt = LocalDateTime.parse(timestamp);
              LocalDateTime jwtIssuedAt =
                  LocalDateTime.ofInstant(
                      Instant.ofEpochSecond(jwtData.getIat()), ZoneId.systemDefault());

              if (jwtIssuedAt.isBefore(revokedAt)) {
                LOGGER.info("jwt issued at : " + jwtIssuedAt + " revokedAt : " + revokedAt);
                LOGGER.error("Privileges for client are revoked.");
                JsonObject result = new JsonObject().put("401", "revoked token passes");
                promise.fail(result.toString());
              } else {
                promise.complete(true);
              }
            })
        .onFailure(
            failureHandler -> {
              LOGGER.info("revoked_client cache call result : [fail] " + failureHandler);
              promise.complete(true);
            });

    return promise.future();
  }

  Future<String> isOpenResource(String id) {
    LOGGER.info("isOpenResource() started");
    Promise<String> promise = Promise.promise();
    CacheType cacheType = CacheType.CATALOGUE_CACHE;
    JsonObject requestJson = new JsonObject().put("type", cacheType).put("key", id);
    cache
        .get(requestJson)
        .onSuccess(
            cacheResult -> {
              if (cacheResult == null || !cacheResult.containsKey("accessPolicy")) {
                promise.fail("ACL not defined for resource item");
              } else {
                promise.complete(cacheResult.getString("accessPolicy"));
              }
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error("catalogue_cache call result : [fail] " + failureHandler);
              promise.fail("Not Found");
            });
    return promise.future();
  }

  Future<Boolean> isValidId(JwtData jwtData, String id, String ppbNumber, String acl) {
    Promise<Boolean> promise = Promise.promise();
    if (acl.equalsIgnoreCase(PII)) {
      boolean isValid = isValidPpbNo(jwtData, ppbNumber);
      if (!isValid) {
        return Future.failedFuture("Incorrect ppbNumber value in jwt");
      }
    }
    String jwtId = jwtData.getIid().split(":")[1];
    if (id.equalsIgnoreCase(jwtId)) {
      promise.complete(true);
    } else {
      promise.fail("Incorrect id value in jwt");
    }

    return promise.future();
  }

  private boolean isValidPpbNo(JwtData jwtData, String ppbNumber) {
    String ppbNoInToken = extractPPBNo(jwtData);
    return ppbNoInToken.equalsIgnoreCase(ppbNumber) ? true : false;
  }

  public String extractPPBNo(JwtData jwtData) {
    JsonObject cons = jwtData.getCons();
    if (cons == null) {
      return "";
    }
    String ppbno = cons.getString(PPB_NUMBER);
    if (ppbno == null) {
      return "";
    }
    return ppbno;
  }

  public Future<JsonObject> validateAccess(
      JwtData jwtData, boolean openResource, JsonObject authInfo) {
    LOGGER.trace("validateAccess() started");
    Promise<JsonObject> promise = Promise.promise();
    String jwtId = jwtData.getIid().split(":")[1];

    if (openResource && checkOpenEndPoints(authInfo.getString("apiEndpoint"))) {
      LOGGER.info("User access is allowed.");
      JsonObject jsonResponse = new JsonObject();
      jsonResponse.put(JSON_IID, jwtId);
      jsonResponse.put(JSON_USERID, jwtData.getSub());
      jsonResponse.put(JSON_APD, jwtData.getApd());
      jsonResponse.put(JSON_CONS, jwtData.getCons());
      jsonResponse.put(ROLE, jwtData.getRole());
      jsonResponse.put(DRL, jwtData.getDrl());
      jsonResponse.put(DID, jwtData.getDid());
      return Future.succeededFuture(jsonResponse);
    }

    Method method = Method.valueOf(authInfo.getString("method"));
    String api = authInfo.getString("apiEndpoint");
    AuthorizationRequest authRequest = new AuthorizationRequest(method, api);
    IudxRole role = IudxRole.fromRole(jwtData.getRole());
    AuthorizationStrategy authStrategy = AuthorizationContextFactory.create(role, apis);
    JwtAuthorization jwtAuthStrategy = new JwtAuthorization(authStrategy);
    LOGGER.info("auth strategy " + authStrategy.getClass().getSimpleName());
    LOGGER.info("endPoint : " + authInfo.getString("apiEndpoint"));
    if (jwtAuthStrategy.isAuthorized(authRequest, jwtData)) {
      LOGGER.info("User access is allowed.");
      JsonObject jsonResponse = new JsonObject();
      jsonResponse.put(JSON_USERID, jwtData.getSub());
      jsonResponse.put(JSON_IID, jwtId);
      jsonResponse.put(JSON_APD, jwtData.getApd());
      jsonResponse.put(JSON_CONS, jwtData.getCons());
      jsonResponse.put(
          JSON_EXPIRY,
          LocalDateTime.ofInstant(
                  Instant.ofEpochSecond(Long.parseLong(jwtData.getExp().toString())),
                  ZoneId.systemDefault())
              .toString());
      jsonResponse.put(ROLE, jwtData.getRole());
      jsonResponse.put(DRL, jwtData.getDrl());
      jsonResponse.put(DID, jwtData.getDid());
      promise.complete(jsonResponse);
    } else {
      LOGGER.error("failed - no access provided to endpoint");
      JsonObject result = new JsonObject().put("401", "no access provided to endpoint");
      promise.fail(result.toString());
    }
    return promise.future();
  }

  private boolean checkOpenEndPoints(String endPoint) {
    for (String item : OPEN_ENDPOINTS) {
      if (endPoint.contains(item)) {
        return true;
      }
    }
    return false;
  }

  Future<Boolean> isValidAudienceValue(JwtData jwtData) {
    Promise<Boolean> promise = Promise.promise();
    if (audience != null && audience.equalsIgnoreCase(jwtData.getAud())) {
      promise.complete(true);
    } else {
      promise.fail("Incorrect audience value in jwt");
    }
    return promise.future();
  }

  @Override
  public AuthenticationService decodeJwt(String jwtToken, Handler<AsyncResult<JwtData>> handler) {
    TokenCredentials creds = new TokenCredentials(jwtToken);
    jwtAuth
        .authenticate(creds)
        .onSuccess(
            user -> {
              JwtData jwtData = new JwtData(user.principal());
              jwtData.setExp(user.get("exp"));
              jwtData.setIat(user.get("iat"));
              handler.handle(Future.succeededFuture(jwtData));
            })
        .onFailure(
            err -> {
              LOGGER.debug("err.getMessage():: " + err);
              LOGGER.error("failed to decode/validate jwt token : " + err.getMessage());
              handler.handle(Future.failedFuture(err.getMessage()));
            });

    return this;
  }

  // class to contain intermediate data for token introspection
  final class ResultContainer {
    JwtData jwtData;
    boolean isOpen;
  }
}
