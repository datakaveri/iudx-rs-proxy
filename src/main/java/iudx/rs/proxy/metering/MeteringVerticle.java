package iudx.rs.proxy.metering;

import static iudx.rs.proxy.common.Constants.METERING_SERVICE_ADDRESS;
import static iudx.rs.proxy.metering.util.Constants.DATABASE_IP;
import static iudx.rs.proxy.metering.util.Constants.DATABASE_NAME;
import static iudx.rs.proxy.metering.util.Constants.DATABASE_PASSWORD;
import static iudx.rs.proxy.metering.util.Constants.DATABASE_PORT;
import static iudx.rs.proxy.metering.util.Constants.DATABASE_TABLE_NAME;
import static iudx.rs.proxy.metering.util.Constants.DATABASE_USERNAME;
import static iudx.rs.proxy.metering.util.Constants.POOL_SIZE;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.rs.proxy.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MeteringVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(MeteringVerticle.class);
  private String databaseIP;
  private int databasePort;
  private String databaseName;
  private String databaseUserName;
  private String databasePassword;
  private String databaseTableName;
  private int poolSize;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;
  private MeteringService metering;
  private Api api;

  @Override
  public void start() throws Exception {

    databaseIP = config().getString(DATABASE_IP);
    databasePort = config().getInteger(DATABASE_PORT);
    databaseName = config().getString(DATABASE_NAME);
    databaseUserName = config().getString(DATABASE_USERNAME);
    databasePassword = config().getString(DATABASE_PASSWORD);
    databaseTableName = config().getString(DATABASE_TABLE_NAME);
    poolSize = config().getInteger(POOL_SIZE);
    JsonObject propObj = new JsonObject();
    propObj.put(DATABASE_TABLE_NAME, databaseTableName);
    propObj.put(DATABASE_IP, databaseIP);
    propObj.put(DATABASE_PORT, databasePort);
    propObj.put(DATABASE_NAME, databaseName);
    propObj.put(DATABASE_USERNAME, databaseUserName);
    propObj.put(DATABASE_PASSWORD, databasePassword);
    propObj.put(POOL_SIZE, poolSize);
    api = Api.getInstance(config().getString("dxApiBasePath"));

    binder = new ServiceBinder(vertx);
    metering = new MeteringServiceImpl(propObj, vertx, api);
    consumer =
        binder.setAddress(METERING_SERVICE_ADDRESS).register(MeteringService.class, metering);
    LOGGER.info("Metering Verticle Started");
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
