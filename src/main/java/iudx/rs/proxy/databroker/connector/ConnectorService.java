package iudx.rs.proxy.databroker.connector;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface ConnectorService {

  /**
   * create a subscription.
   *
   * @param request subscription json.
   * @return Future object
   */
  Future<JsonObject> registerConnector(JsonObject request, String vHost);

  /**
   * delete a subscription request.
   *
   * @param json json containing id for sub to delete
   * @return Future object
   */
  Future<JsonObject> deleteConnectors(JsonObject json, String vhost);
}
