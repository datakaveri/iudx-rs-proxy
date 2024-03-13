package iudx.rs.proxy.optional.consentlogs;

import static iudx.rs.proxy.common.Constants.*;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.rs.proxy.cache.CacheService;
import iudx.rs.proxy.metering.MeteringService;
import iudx.rs.proxy.optional.consentlogs.dss.PayloadSigningManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConsentLoggingVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(ConsentLoggingVerticle.class);
  PayloadSigningManager payloadSigningManager;
  ConsentLoggingService consentLoggingService;
  MeteringService meteringService;
  CacheService cacheService;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;
  private String certFileName;
  private String password;

  @Override
  public void start() throws Exception {
    certFileName = config().getString("certFileName");
    password = config().getString("password");
    binder = new ServiceBinder(vertx);

    meteringService = MeteringService.createProxy(vertx, METERING_SERVICE_ADDRESS);
    cacheService = CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);
    payloadSigningManager = PayloadSigningManager.init(config());
    consentLoggingService =
        new ConsentLoggingServiceImpl(vertx, payloadSigningManager, meteringService, cacheService);
    consumer =
        binder
            .setAddress(CONSEENTLOG_SERVICE_ADDRESS)
            .register(ConsentLoggingService.class, consentLoggingService);

    LOGGER.info("ConsentLogging Vertical deployed.");
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
