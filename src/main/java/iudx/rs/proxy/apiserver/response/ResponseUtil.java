package iudx.rs.proxy.apiserver.response;

import io.vertx.core.json.JsonObject;
import iudx.rs.proxy.common.HttpStatusCode;
import iudx.rs.proxy.common.ResponseUrn;

public class ResponseUtil {
 

  public static JsonObject generateResponse(HttpStatusCode statusCode, ResponseUrn urn) {
    return generateResponse(statusCode, urn, statusCode.getDescription());
  }

  public static JsonObject generateResponse(HttpStatusCode statusCode, ResponseUrn urn, String message) {
    String urnType = urn.getUrn();
    return new RestResponse.Builder()
        .withType(urnType)
        .withTitle(statusCode.getDescription())
        .withMessage(message)
        .build().toJson();

  }
  
  public static JsonObject generateResponse(HttpStatusCode statusCode, String message) {
    String urn = statusCode.getUrn();
    message=(message==null || message.isBlank())?statusCode.getDescription():message;
    return new RestResponse.Builder()
        .withType(urn)
        .withTitle(statusCode.getDescription())
        .withMessage(message)
        .build().toJson();

  }
  
  public static JsonObject generateResponse(HttpStatusCode statusCode) {
    String urn = statusCode.getUrn();
    return generateResponse(statusCode, statusCode.getDescription());
  }


}
