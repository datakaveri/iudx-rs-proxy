package iudx.rs.proxy.apiserver.response;

import io.vertx.core.json.JsonObject;
import iudx.rs.proxy.common.HttpStatusCode;
import iudx.rs.proxy.common.ResponseUrn;

public class ResponseUtil {
 

  public static JsonObject generateResponse(HttpStatusCode statusCode, ResponseUrn urn) {
    return generateResponse(statusCode, urn, statusCode.getDescription());
  }

  public static JsonObject generateResponse(HttpStatusCode statusCode, ResponseUrn urn, String message) {
    String type = urn.getUrn();
    return new RestResponse.Builder()
        .withType(type)
        .withTitle(statusCode.getDescription())
        .withMessage(message)
        .build().toJson();

  }


}
