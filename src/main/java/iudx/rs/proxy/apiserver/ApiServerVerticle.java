package iudx.rs.proxy.apiserver;

import static iudx.rs.proxy.apiserver.util.ApiServerConstants.*;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;



public class ApiServerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(ApiServerVerticle.class);

  private int port;
  private HttpServer server;
  private Router router;
  private String keystore, keystorePassword;
  private boolean isSSL, isProduction;

  @Override
  public void start() throws Exception {

    router = Router.router(vertx);
    router
        .route()
        .handler(
            CorsHandler
                .create("*")
                .allowedHeaders(ALLOWED_HEADERS)
                .allowedMethods(ALLOWED_METHODS))
        .handler(responseHeaderHandler -> {
          responseHeaderHandler
              .response()
              .putHeader("Cache-Control", "no-cache, no-store,  must-revalidate,max-age=0")
              .putHeader("Pragma", "no-cache")
              .putHeader("Expires", "0")
              .putHeader("X-Content-Type-Options", "nosniff");
          responseHeaderHandler.next();
        });

    isSSL = config().getBoolean("ssl");
    isProduction = config().getBoolean("production");
    port = config().getInteger("port");

    HttpServerOptions serverOptions = new HttpServerOptions();

    if (isSSL) {
      LOGGER.info("Info: Starting HTTPs server");

      keystore = config().getString("keystore");
      keystorePassword = config().getString("keystorePassword");

      serverOptions
          .setSsl(true)
          .setKeyStoreOptions(new JksOptions()
              .setPath(keystore)
              .setPassword(keystorePassword));

    } else {
      LOGGER.info("Info: Starting HTTP server");

      serverOptions.setSsl(false);
      if (isProduction) {
        port = 80;
      } else {
        port = 8080;
      }
    }

    serverOptions.setCompressionSupported(true).setCompressionLevel(5);
    server = vertx.createHttpServer(serverOptions);
    server.requestHandler(router).listen(port);


    router.get("/health").handler(this::health);
  }

  public void health(RoutingContext context) {
    context
        .response()
        .putHeader("content-type", "application/json")
        .setStatusCode(200)
        .end(new JsonObject().put("status", "running").toString());
  }
}
