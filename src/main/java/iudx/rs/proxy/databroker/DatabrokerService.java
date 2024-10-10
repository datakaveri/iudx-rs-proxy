package iudx.rs.proxy.databroker;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@ProxyGen
@VertxGen
public interface DatabrokerService {

  @GenIgnore
  static DatabrokerService createProxy(Vertx vertx, String address) {
    return new DatabrokerServiceVertxEBProxy(vertx, address);
  }

  @Fluent
  DatabrokerService executeAdapterQuery(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabrokerService executeAdapterQueryRPC(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabrokerService publishMessage(
      JsonObject body,
      String toExchange,
      String routingKey,
      Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabrokerService createConnector(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabrokerService deleteConnector(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabrokerService resetPassword(String userid, Handler<AsyncResult<JsonObject>> handler);
}
