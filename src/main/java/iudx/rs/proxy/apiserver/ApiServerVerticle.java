package iudx.rs.proxy.apiserver;

import static iudx.rs.proxy.apiserver.util.ApiServerConstants.ALLOWED_HEADERS;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.ALLOWED_METHODS;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.HEADER_HOST;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.NGSILD_TEMPORAL_URL;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;
import iudx.rs.proxy.apiserver.handlers.AuthHandler;
import iudx.rs.proxy.apiserver.handlers.FailureHandler;
import iudx.rs.proxy.apiserver.handlers.ValidationHandler;
import iudx.rs.proxy.apiserver.util.RequestType;
import iudx.rs.proxy.apiserver.validation.ValidatorsHandlersFactory;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
            CorsHandler.create("*").allowedHeaders(ALLOWED_HEADERS).allowedMethods(ALLOWED_METHODS))
        .handler(
            responseHeaderHandler -> {
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
          .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword));

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

    ValidatorsHandlersFactory validators = new ValidatorsHandlersFactory();
    FailureHandler validationsFailureHandler = new FailureHandler();

    ValidationHandler temporalValidationHandler =
        new ValidationHandler(vertx, RequestType.TEMPORAL);
    router
        .get(NGSILD_TEMPORAL_URL)
        .handler(temporalValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::handleTemporalQuery)
        .failureHandler(validationsFailureHandler);

  }

  public void handleTemporalQuery(RoutingContext routingContext) {

    LOGGER.trace("Info: handleTemporalQuery method started.");

  }
}
