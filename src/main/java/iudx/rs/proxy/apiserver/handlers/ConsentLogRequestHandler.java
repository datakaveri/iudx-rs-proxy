package iudx.rs.proxy.apiserver.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.ext.web.RoutingContext;

public class ConsentLogRequestHandler implements Handler<RoutingContext>{
  
  private static final Logger LOGGER = LogManager.getLogger(ConsentLogRequestHandler.class);
  
  private boolean isAdexDeployment;
  public ConsentLogRequestHandler(boolean isAdexDeployment){
    this.isAdexDeployment=isAdexDeployment;
  }

  @Override
  public void handle(RoutingContext context) {
    
    if(isAdexDeployment) {
      Future.future(f->logRequestReceived(context));
      LOGGER.info("consent log : {}","DATA_REQUESTED");
      context.next();
    }
    LOGGER.info("return");
    context.next();
  }
  
  private Future<Void> logRequestReceived(RoutingContext context){
//    LOGGER.info("Request received body :{}",context.request().body())
    Promise<Void> promise=Promise.promise();
    LOGGER.info("Request received body :{}",context.body().asJsonObject());
    LOGGER.info("HEADERS : {}",context.request().headers());
    promise.complete();
    return promise.future();
  }

}
