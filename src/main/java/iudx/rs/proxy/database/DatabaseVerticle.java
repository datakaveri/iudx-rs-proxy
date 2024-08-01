package iudx.rs.proxy.database;

import static iudx.rs.proxy.common.Constants.DB_SERVICE_ADDRESS;
import static iudx.rs.proxy.database.postgres.Constants.*;
import static iudx.rs.proxy.database.postgres.Constants.POOL_SIZE;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;
import iudx.rs.proxy.database.postgres.PostgresServiceImpl;

public class DatabaseVerticle extends AbstractVerticle {
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;
  private DatabaseService dbServiceImpl;
  private PgPool pgClient;

  @Override
  public void start() throws Exception {
    String databaseIp = config().getString(DATABASE_IP);
    int databasePort = config().getInteger(DATABASE_PORT);
    String databaseName = config().getString(DATABASE_NAME);
    String databaseUserName = config().getString(DATABASE_USERNAME);
    String databasePassword = config().getString(DATABASE_PASSWORD);
    int poolSize = config().getInteger(POOL_SIZE);

    PgConnectOptions connectOptions =
        new PgConnectOptions()
            .setPort(databasePort)
            .setHost(databaseIp)
            .setDatabase(databaseName)
            .setUser(databaseUserName)
            .setPassword(databasePassword);

    PoolOptions poolOptions = new PoolOptions().setMaxSize(poolSize);
    pgClient = PgPool.pool(vertx, connectOptions, poolOptions);

    binder = new ServiceBinder(vertx);
    dbServiceImpl = new PostgresServiceImpl(pgClient);
    consumer = binder.setAddress(DB_SERVICE_ADDRESS).register(DatabaseService.class, dbServiceImpl);
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
