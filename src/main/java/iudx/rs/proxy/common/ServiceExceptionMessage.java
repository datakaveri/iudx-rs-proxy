package iudx.rs.proxy.common;

import io.vertx.core.json.JsonObject;

/**
 * An Exception Message to be included in Service Exception. It will abstract the IUDX response
 * URN's and a detailed message for the exception thrown. URN will be used to create a valid API
 * response for user. Detailed message field can be used to either log the message to logging system
 * or to inform user about failure/exception cause.
 */
public class ServiceExceptionMessage {

  private final String urn;
  private JsonObject detail;

  private ServiceExceptionMessage(Builder builder) {
    this.urn = builder.urn;
    this.detail = builder.detail;
  }

  public String getUrn() {
    return urn;
  }

  public JsonObject toJson() {

    JsonObject json = new JsonObject();

    json.put("urn", this.urn);
    json.put("detail", this.detail);

    return json;
  }

  public JsonObject getDetails() {
    return detail;
  }

  public static class Builder {
    private final String urn;
    private JsonObject detail;

    public Builder(String urn) {
      this.urn = urn;
    }

    public Builder withDetails(JsonObject detail) {
      this.detail = detail;
      return this;
    }

    public ServiceExceptionMessage build() {
      ServiceExceptionMessage message = new ServiceExceptionMessage(this);
      return message;
    }
  }
}
