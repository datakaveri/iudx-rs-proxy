package iudx.rs.proxy.optional.consentlogs;

import io.vertx.core.json.JsonObject;

public interface ConsentLog {
  public void log();
  public JsonObject toJsonObject();

}
