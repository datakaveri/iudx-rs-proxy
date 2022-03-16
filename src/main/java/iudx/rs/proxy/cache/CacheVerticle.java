package iudx.rs.proxy.cache;

import static iudx.rs.proxy.common.Constants.CACHE_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.serviceproxy.ServiceBinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CacheVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(CacheVerticle.class);

  private static final String SERVICE_ADDRESS = CACHE_SERVICE_ADDRESS;

  private MessageConsumer<JsonObject> consumer;
  private ServiceBinder binder;
  private WebClient webClient;

  private CacheService cacheService;

  static WebClient createWebClient(Vertx vertx) {
    return createWebClient(vertx, false);
  }

  static WebClient createWebClient(Vertx vertxObj, boolean testing) {
    WebClientOptions webClientOptions = new WebClientOptions();
    if (testing) {
      webClientOptions.setTrustAll(true).setVerifyHost(false);
    }
    webClientOptions.setSsl(true);
    return WebClient.create(vertxObj, webClientOptions);
  }

  @Override
  public void start() throws Exception {

    cacheService = new CacheServiceImpl(vertx,createWebClient(vertx),config());
    binder = new ServiceBinder(vertx);
    consumer = binder.setAddress(SERVICE_ADDRESS).register(CacheService.class, cacheService);

    LOGGER.info("Cache Verticle deployed.");
  }
}
