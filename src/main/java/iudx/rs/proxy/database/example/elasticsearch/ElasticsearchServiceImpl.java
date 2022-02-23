package iudx.rs.proxy.database.example.elasticsearch;

import org.apache.http.HttpStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceException;
import iudx.rs.proxy.database.DatabaseService;

public class ElasticsearchServiceImpl implements DatabaseService{
  
  private final JsonObject config;
  
  public ElasticsearchServiceImpl(JsonObject config) {
    this.config=config;
  }

  @Override
  public DatabaseService searchQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler)
      throws ServiceException {
    boolean success=true;
    //example how to use service exception
    if(success) {
      handler.handle(Future.succeededFuture(new JsonObject()));
    }else {
      throw new ServiceException(HttpStatus.SC_BAD_REQUEST, "message for failures");
    }
    return this;
  }

  @Override
  public DatabaseService countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler)
      throws ServiceException {
    // TODO Auto-generated method stub
    return null;
  }

}
