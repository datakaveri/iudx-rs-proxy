package iudx.rs.proxy.cache;

import static iudx.rs.proxy.common.Constants.CACHE_SERVICE_ADDRESS;
import static iudx.rs.proxy.common.Constants.DATABASE_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.rs.proxy.database.DatabaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CacheVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(CacheVerticle.class);

  private static final String SERVICE_ADDRESS = CACHE_SERVICE_ADDRESS;

  private MessageConsumer<JsonObject> consumer;
  private ServiceBinder binder;

  private CacheService cacheService;
  private DatabaseService pgService;

  @Override
  public void start() throws Exception {

    pgService = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);

    cacheService = new CacheServiceImpl(vertx, pgService);

    binder = new ServiceBinder(vertx);
    consumer = binder.setAddress(SERVICE_ADDRESS).register(CacheService.class, cacheService);

    LOGGER.info("Cache Verticle deployed.");
  }

}
