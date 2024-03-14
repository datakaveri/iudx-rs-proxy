package iudx.rs.proxy.metering;

import static iudx.rs.proxy.apiserver.util.ApiServerConstants.*;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.TABLE_NAME;
import static iudx.rs.proxy.common.Constants.DATABROKER_SERVICE_ADDRESS;
import static iudx.rs.proxy.common.Constants.DB_SERVICE_ADDRESS;
import static iudx.rs.proxy.metering.util.Constants.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import iudx.rs.proxy.common.Api;
import iudx.rs.proxy.common.Response;
import iudx.rs.proxy.database.DatabaseService;
import iudx.rs.proxy.databroker.DatabrokerService;
import iudx.rs.proxy.metering.util.ParamsValidation;
import iudx.rs.proxy.metering.util.QueryBuilder;
import iudx.rs.proxy.metering.util.ResponseBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MeteringServiceImpl implements MeteringService {

  private static final Logger LOGGER = LogManager.getLogger(MeteringServiceImpl.class);
  public static DatabrokerService rmqService;
  public static DatabaseService postgresService;
  public final String _COUNT_COLUMN;
  public final String _RESOURCE_ID_COLUMN;
  public final String _API_COLUMN;
  public final String _USERID_COLUMN;
  public final String _TIME_COLUMN;
  public final String _RESPONSE_SIZE_COLUMN;
  public final String _ID_COLUMN;
  private final QueryBuilder queryBuilder = new QueryBuilder();
  private final ObjectMapper objectMapper = new ObjectMapper();
  PgConnectOptions connectOptions;
  PoolOptions poolOptions;
  PgPool pool;
  JsonObject validationCheck = new JsonObject();
  int total;
  private ParamsValidation validation;
  private JsonObject query = new JsonObject();
  private String databaseIp;
  private int databasePort;
  private String databaseName;
  private String databaseUserName;
  private String databasePassword;
  private int databasePoolSize;
  private String databaseTableName;
  private ResponseBuilder responseBuilder;

  public MeteringServiceImpl(JsonObject propObj, Vertx vertxInstance, Api api) {

    if (propObj != null && !propObj.isEmpty()) {
      databaseIp = propObj.getString(DATABASE_IP);
      databasePort = propObj.getInteger(DATABASE_PORT);
      databaseName = propObj.getString(DATABASE_NAME);
      databaseUserName = propObj.getString(DATABASE_USERNAME);
      databasePassword = propObj.getString(DATABASE_PASSWORD);
      databaseTableName = propObj.getString(DATABASE_TABLE_NAME);
      databasePoolSize = propObj.getInteger(POOL_SIZE);
      validation = new ParamsValidation(api);
    }

    this.connectOptions =
        new PgConnectOptions()
            .setPort(databasePort)
            .setHost(databaseIp)
            .setDatabase(databaseName)
            .setUser(databaseUserName)
            .setPassword(databasePassword)
            .setReconnectAttempts(2)
            .setReconnectInterval(1000);

    this.poolOptions = new PoolOptions().setMaxSize(databasePoolSize);
    this.pool = PgPool.pool(vertxInstance, connectOptions, poolOptions);
    this.rmqService = DatabrokerService.createProxy(vertxInstance, DATABROKER_SERVICE_ADDRESS);
    if (postgresService == null) {
      postgresService = DatabaseService.createProxy(vertxInstance, DB_SERVICE_ADDRESS);
    }
    _COUNT_COLUMN =
        COUNT_COLUMN.insert(0, "(" + databaseName + "." + databaseTableName + ".").toString();
    _RESOURCE_ID_COLUMN =
        RESOURCE_ID_COLUMN.insert(0, "(" + databaseName + "." + databaseTableName + ".").toString();
    _API_COLUMN =
        API_COLUMN.insert(0, "(" + databaseName + "." + databaseTableName + ".").toString();
    _USERID_COLUMN =
        USERID_COLUMN.insert(0, "(" + databaseName + "." + databaseTableName + ".").toString();
    _TIME_COLUMN =
        TIME_COLUMN.insert(0, "(" + databaseName + "." + databaseTableName + ".").toString();
    _RESPONSE_SIZE_COLUMN =
        RESPONSE_SIZE_COLUMN
            .insert(0, "(" + databaseName + "." + databaseTableName + ".")
            .toString();
    _ID_COLUMN = ID_COLUMN.insert(0, "(" + databaseName + "." + databaseTableName + ".").toString();
  }

  @Override
  public MeteringService executeReadQuery(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    LOGGER.trace("Info: Read Query" + request.toString());

    validationCheck = validation.paramsCheck(request);

    if (validationCheck != null && validationCheck.containsKey(ERROR)) {
      responseBuilder =
          new ResponseBuilder(FAILED)
              .setTypeAndTitle(400)
              .setMessage(validationCheck.getString(ERROR));
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return this;
    }
    request.put(TABLE_NAME, databaseTableName);

    String count = request.getString("options");
    if (count == null) {
      countQueryForRead(request, handler);
    } else {
      countQuery(request, handler);
    }

    return this;
  }

  private void countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    query = queryBuilder.buildCountReadQueryFromPg(request);
    Future<JsonObject> resultCountPg = executeQueryDatabaseOperation(query);
    resultCountPg.onComplete(
        countHandler -> {
          if (countHandler.succeeded()) {
            try {
              var countHandle = countHandler.result().getJsonArray("result");
              total = countHandle.getJsonObject(0).getInteger("count");
              if (total == 0) {
                responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(204).setCount(0);
                handler.handle(Future.succeededFuture(responseBuilder.getResponse()));

              } else {
                responseBuilder = new ResponseBuilder(SUCCESS).setTypeAndTitle(200).setCount(total);
                handler.handle(Future.succeededFuture(responseBuilder.getResponse()));
              }
            } catch (NullPointerException nullPointerException) {
              LOGGER.debug(nullPointerException.toString());
            }
          }
        });
  }

  private void countQueryForRead(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    query = queryBuilder.buildCountReadQueryFromPg(request);
    Future<JsonObject> resultCountPg = executeQueryDatabaseOperation(query);
    resultCountPg.onComplete(
        countHandler -> {
          if (countHandler.succeeded()) {
            try {
              var countHandle = countHandler.result().getJsonArray("result");
              total = countHandle.getJsonObject(0).getInteger("count");
              request.put("totalHits", total);
              if (total == 0) {
                responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(204).setCount(0);
                handler.handle(Future.succeededFuture(responseBuilder.getResponse()));
              } else {
                readMethod(request, handler);
              }
            } catch (NullPointerException nullPointerException) {
              LOGGER.debug(nullPointerException.toString());
            }
          }
        });
  }

  private void readMethod(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String limit;
    String offset;
    if (request.getString(LIMITPARAM) == null) {
      limit = "2000";
      request.put(LIMITPARAM, limit);
    } else {
      limit = request.getString(LIMITPARAM);
    }
    if (request.getString(OFFSETPARAM) == null) {
      offset = "0";
      request.put(OFFSETPARAM, offset);
    } else {
      offset = request.getString(OFFSETPARAM);
    }
    query = queryBuilder.buildReadQueryFromPg(request);
    LOGGER.debug("read query = " + query);
    Future<JsonObject> resultsPg = executeQueryDatabaseOperation(query);
    resultsPg.onComplete(
        readHandler -> {
          if (readHandler.succeeded()) {
            LOGGER.info("Read Completed successfully");
            JsonObject resultJsonObject = readHandler.result();
            resultJsonObject.put(LIMITPARAM, limit);
            resultJsonObject.put(OFFSETPARAM, offset);
            resultJsonObject.put("totalHits", request.getLong("totalHits"));
            handler.handle(Future.succeededFuture(resultJsonObject));
          } else {
            LOGGER.debug("Could not read from DB : " + readHandler.cause());
            handler.handle(Future.failedFuture(readHandler.cause().getMessage()));
          }
        });
  }

  @Override
  public MeteringService insertMeteringValuesInRMQ(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    JsonObject writeMessage = queryBuilder.buildMessageForRmq(request);

    rmqService.publishMessage(
        writeMessage,
        EXCHANGE_NAME,
        ROUTING_KEY,
        rmqHandler -> {
          if (rmqHandler.succeeded()) {
            handler.handle(Future.succeededFuture());
            LOGGER.info("inserted into rmq");
          } else {
            LOGGER.error(rmqHandler.cause());
            try {
              Response resp =
                  objectMapper.readValue(rmqHandler.cause().getMessage(), Response.class);
              LOGGER.debug("response from rmq " + resp);
              handler.handle(Future.failedFuture(resp.toString()));
            } catch (JsonProcessingException e) {
              LOGGER.error("Failure message not in format [type,title,detail]");
              handler.handle(Future.failedFuture(e.getMessage()));
            }
          }
        });
    return this;
  }

  private Future<JsonObject> executeQueryDatabaseOperation(JsonObject jsonObject) {
    Promise<JsonObject> promise = Promise.promise();
    postgresService.executeQuery(
        jsonObject,
        dbHandler -> {
          if (dbHandler.succeeded()) {
            promise.complete(dbHandler.result());
          } else {
            promise.fail(dbHandler.cause().getMessage());
          }
        });

    return promise.future();
  }
}
