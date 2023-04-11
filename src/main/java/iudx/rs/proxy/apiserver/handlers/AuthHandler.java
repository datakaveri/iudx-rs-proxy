package iudx.rs.proxy.apiserver.handlers;

import static iudx.rs.proxy.apiserver.util.ApiServerConstants.*;
import static iudx.rs.proxy.common.Constants.AUTH_SERVICE_ADDRESS;
import static iudx.rs.proxy.common.ResponseUrn.INVALID_TOKEN_URN;
import static iudx.rs.proxy.common.ResponseUrn.RESOURCE_NOT_FOUND_URN;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import iudx.rs.proxy.authenticator.AuthenticationService;
import iudx.rs.proxy.common.Api;
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
  static Api api;

  public static AuthHandler create(Vertx vertx,Api apiEndpoints) {
    authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    api=apiEndpoints;
    return new AuthHandler();
  }

  @Override
  public void handle(RoutingContext context) {
    request = context.request();

    RequestBody requestBody = context.body();
    JsonObject requestJson=null;
    if(request!=null) {
      if(requestBody.asJsonObject()!=null) {
        requestJson=requestBody.asJsonObject().copy();
      }
    }
    if(requestJson==null) {
      requestJson=new JsonObject();
    }

    String token = request.headers().get(HEADER_TOKEN);
    final String path = getNormalizedPath(request.path());
    final String method = context.request().method().toString();


    if (token == null) token = "public";

    JsonObject authInfo =
        new JsonObject().put(API_ENDPOINT, path).put(HEADER_TOKEN, token).put(API_METHOD, method);

    LOGGER.debug("Info :" + context.request().path());
    LOGGER.debug("Info :" + context.request().path().split("/").length);

    String id = getId(context);
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
            authInfo.put("apd", authHandler.result().getValue("apd"));
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
  private String getId(RoutingContext context) {
    String paramId = getId4rmRequest();
    String bodyId = getId4rmBody(context);
    String id;
      if (paramId != null && !paramId.isBlank()) {
        id = paramId;
      } else {
        id = bodyId;
      }
    return id;
  }

  private String getId4rmRequest() {
    LOGGER.info("from request " + request.getParam(ID));
    return request.getParam(ID);
  }

  private String getId4rmBody(RoutingContext context) {
    JsonObject body = context.body().asJsonObject();
    String id = null;
    if (body != null) {
      JsonArray array = body.getJsonArray(JSON_ENTITIES);
      if (array != null) {
        JsonObject json = array.getJsonObject(0);
        if (json != null) {
          id = json.getString(ID);
        }
      }
    }
    return id;
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
    if (url.matches(getpathRegex(api.getTemporalEndpoint()))) {
      path = api.getTemporalEndpoint();
    } else if (url.matches(getpathRegex(api.getEntitiesEndpoint()))) {
      path = api.getEntitiesEndpoint();
    } else if (url.matches(api.getConsumerAuditEndpoint())) {
      path = api.getConsumerAuditEndpoint();
    } else if (url.matches(api.getProviderAuditEndpoint())) {
      path = api.getProviderAuditEndpoint();
    } else if (url.matches(api.getPostEntitiesEndpoint())) {
      path = api.getPostEntitiesEndpoint();
    }else if(url.matches(api.getPostTemporalEndpoint())){
      path = api.getPostTemporalEndpoint();
    }
    return path;
  }
  
  private String getpathRegex(String path) {
    return path+"(.*)";
  }
}
