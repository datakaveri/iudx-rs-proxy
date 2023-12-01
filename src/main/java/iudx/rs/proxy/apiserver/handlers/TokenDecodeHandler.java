package iudx.rs.proxy.apiserver.handlers;

import static iudx.rs.proxy.apiserver.util.ApiServerConstants.APPLICATION_JSON;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.CONTENT_TYPE;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.HEADER_TOKEN;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.JSON_DETAIL;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.JSON_TITLE;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.JSON_TYPE;
import static iudx.rs.proxy.common.Constants.AUTH_SERVICE_ADDRESS;
import static iudx.rs.proxy.common.ResponseUrn.INVALID_TOKEN_URN;
import static iudx.rs.proxy.common.ResponseUrn.RESOURCE_NOT_FOUND_URN;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import iudx.rs.proxy.authenticator.AuthenticationService;
import iudx.rs.proxy.authenticator.model.JwtData;
import iudx.rs.proxy.common.HttpStatusCode;
import iudx.rs.proxy.common.ResponseUrn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TokenDecodeHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(TokenDecodeHandler.class);

  static AuthenticationService decode;
  private final String AUTH_INFO = "authInfo";
  private HttpServerRequest request;

  public static TokenDecodeHandler create(Vertx vertx) {
    decode = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    return new TokenDecodeHandler();
  }
  @Override
  public void handle(RoutingContext context) {
    LOGGER.debug("tokenDecoderHandler started");
    request = context.request();

    RequestBody requestBody = context.body();
    JsonObject requestJson = null;
    if (request != null) {
      if (requestBody.asJsonObject() != null) {
        requestJson = requestBody.asJsonObject().copy();
      }
    }
    if (requestJson == null) {
      requestJson = new JsonObject();
    }
    String token = request.headers().get(HEADER_TOKEN);


    decode.decodeJwt(token,decodeHandler ->{

      if(decodeHandler.succeeded()){
        JwtData jwtData = decodeHandler.result();
        context.data().put("jwtdata", jwtData);

      } else{
        processAuthFailure(context, decodeHandler.cause().getMessage());
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
}
