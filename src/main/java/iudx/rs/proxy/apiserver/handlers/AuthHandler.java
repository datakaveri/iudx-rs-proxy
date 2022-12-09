package iudx.rs.proxy.apiserver.handlers;

import static iudx.rs.proxy.apiserver.util.ApiServerConstants.API_ENDPOINT;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.API_METHOD;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.APPLICATION_JSON;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.CONTENT_TYPE;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.ENTITIES_URL_REGEX;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.HEADER_TOKEN;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.ID;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.IDS;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.IID;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.IUDX_CONSUMER_AUDIT_URL;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.IUDX_PROVIDER_AUDIT_URL;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.JSON_DETAIL;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.JSON_TITLE;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.JSON_TYPE;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.NGSILD_ENTITIES_URL;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.NGSILD_TEMPORAL_URL;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.TEMPORAL_URL_REGEX;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.USER_ID;
import static iudx.rs.proxy.common.Constants.AUTH_SERVICE_ADDRESS;
import static iudx.rs.proxy.common.ResponseUrn.INVALID_TOKEN_URN;
import static iudx.rs.proxy.common.ResponseUrn.RESOURCE_NOT_FOUND_URN;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.rs.proxy.authenticator.AuthenticationService;
import iudx.rs.proxy.common.HttpStatusCode;
import iudx.rs.proxy.common.ResponseUrn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** IUDX Authentication handler to authenticate token passed in HEADER */
public class AuthHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(AuthHandler.class);

  static AuthenticationService authenticator;
  private final String AUTH_INFO = "authInfo";
  private HttpServerRequest request;
  private static String basePath;
  public static AuthHandler create(Vertx vertx, JsonObject config) {
    authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    basePath = config.getString("basePath");
    return new AuthHandler();
  }

  @Override
  public void handle(RoutingContext context) {
    request = context.request();
    JsonObject requestJson = context.getBodyAsJson();

    if (requestJson == null) {
      requestJson = new JsonObject();
    }

    LOGGER.debug("Info : path " + request.path());

    String token = request.headers().get(HEADER_TOKEN);
    final String path = getNormalizedPath(request.path());
    final String method = context.request().method().toString();


    if (token == null) token = "public";

    JsonObject authInfo =
        new JsonObject().put(API_ENDPOINT, path).put(HEADER_TOKEN, token).put(API_METHOD, method);

    LOGGER.debug("Info :" + context.request().path());
    LOGGER.debug("Info :" + context.request().path().split("/").length);

    String id = getId(path);
    authInfo.put(ID, id);

    JsonArray ids = new JsonArray();
    String[] idArray = (id == null ? new String[0] : id.split(","));
    for (String i : idArray) {
      ids.add(i);
    }

    requestJson.put(IDS, ids);

    authenticator.tokenIntrospect(
        requestJson,
        authInfo,
        authHandler -> {
          if (authHandler.succeeded()) {
            authInfo.put(IID, authHandler.result().getValue(IID));
            authInfo.put(USER_ID, authHandler.result().getValue(USER_ID));
            context.data().put(AUTH_INFO, authInfo);
          } else {
            processAuthFailure(context, authHandler.cause().getMessage());
            return;
          }
          context.next();
        });
  }

  private void processAuthFailure(RoutingContext ctx, String result) {
    if (result.contains("Not Found")) {
      LOGGER.error("Error : Item Not Found");
      HttpStatusCode statusCode = HttpStatusCode.getByValue(404);
      ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(statusCode.getValue())
          .end(generateResponse(RESOURCE_NOT_FOUND_URN, statusCode).toString());
    } else {
      LOGGER.error("Error : Authentication Failure");
      HttpStatusCode statusCode = HttpStatusCode.getByValue(401);
      ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(statusCode.getValue())
          .end(generateResponse(INVALID_TOKEN_URN, statusCode).toString());
    }
  }

  private JsonObject generateResponse(ResponseUrn urn, HttpStatusCode statusCode) {
    return new JsonObject()
        .put(JSON_TYPE, urn.getUrn())
        .put(JSON_TITLE, statusCode.getDescription())
        .put(JSON_DETAIL, statusCode.getDescription());
  }

  /**
   * extract id from request (path/query or body )
   *
   * @param ctx     current routing context
   * @param forPath endpoint called for
   * @return id extraced fro path if present
   */
  private String getId(String path) {

    String pathId = getId4rmRequest();
    String id = "";
    if (pathId != null && !pathId.isBlank()) {
      id = pathId;
    }
    return id;
  }

  private String getId4rmRequest() {
    LOGGER.info("from request " + request.getParam(ID));
    return request.getParam(ID);
  }

  /**
   * get normalized path without id as path param.
   *
   * @param url complete path from request
   * @return path without id.
   */
  private String getNormalizedPath(String url) {
    LOGGER.debug("URL : " + url);
    String path = null;
    if (url.matches(basePath + TEMPORAL_URL_REGEX)) {
      path = basePath + NGSILD_TEMPORAL_URL;
    } else if (url.matches(basePath + ENTITIES_URL_REGEX)) {
      path = basePath + NGSILD_ENTITIES_URL;
    } else if (url.matches(basePath + IUDX_CONSUMER_AUDIT_URL)) {
      path = basePath + IUDX_CONSUMER_AUDIT_URL;
    } else if (url.matches(basePath + IUDX_PROVIDER_AUDIT_URL)) {
      path = basePath + IUDX_PROVIDER_AUDIT_URL;
    }
    return path;
  }
}
