package iudx.rs.proxy.database;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceException;

@VertxGen
@ProxyGen
public interface DatabaseService {

  /**
   * The create implements the count operation with the database.
   *
   * @return DatabaseService object.
   */
  @GenIgnore
  static DatabaseService createProxy(Vertx vertx, String address) {
    return new DatabaseServiceVertxEBProxy(vertx, address);
  }

  @Fluent
  DatabaseService executeQuery(
      final JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler)
      throws ServiceException;
}
