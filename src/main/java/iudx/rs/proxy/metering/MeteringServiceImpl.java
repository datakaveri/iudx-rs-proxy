package iudx.rs.proxy.metering;

import static iudx.rs.proxy.metering.util.Constants.API;
import static iudx.rs.proxy.metering.util.Constants.API_COLUMN;
import static iudx.rs.proxy.metering.util.Constants.BETWEEN;
import static iudx.rs.proxy.metering.util.Constants.CONSUMER;
import static iudx.rs.proxy.metering.util.Constants.COUNT;
import static iudx.rs.proxy.metering.util.Constants.COUNT_COLUMN;
import static iudx.rs.proxy.metering.util.Constants.DATABASE_IP;
import static iudx.rs.proxy.metering.util.Constants.DATABASE_NAME;
import static iudx.rs.proxy.metering.util.Constants.DATABASE_PASSWORD;
import static iudx.rs.proxy.metering.util.Constants.DATABASE_PORT;
import static iudx.rs.proxy.metering.util.Constants.DATABASE_TABLE_NAME;
import static iudx.rs.proxy.metering.util.Constants.DATABASE_USERNAME;
import static iudx.rs.proxy.metering.util.Constants.DURING;
import static iudx.rs.proxy.metering.util.Constants.ENDPOINT;
import static iudx.rs.proxy.metering.util.Constants.END_TIME;
import static iudx.rs.proxy.metering.util.Constants.ERROR;
import static iudx.rs.proxy.metering.util.Constants.HEADER_OPTIONS;
import static iudx.rs.proxy.metering.util.Constants.ID;
import static iudx.rs.proxy.metering.util.Constants.ID_COLUMN;
import static iudx.rs.proxy.metering.util.Constants.INVALID_PROVIDER_REQUIRED;
import static iudx.rs.proxy.metering.util.Constants.IUDX_PROVIDER_AUDIT_URL;
import static iudx.rs.proxy.metering.util.Constants.LAST_ID;
import static iudx.rs.proxy.metering.util.Constants.LATEST_ID;
import static iudx.rs.proxy.metering.util.Constants.POOL_SIZE;
import static iudx.rs.proxy.metering.util.Constants.PROVIDER_ID;
import static iudx.rs.proxy.metering.util.Constants.QUERY_KEY;
import static iudx.rs.proxy.metering.util.Constants.RESOURCE_ID_COLUMN;
import static iudx.rs.proxy.metering.util.Constants.RESPONSE_ARRAY;
import static iudx.rs.proxy.metering.util.Constants.RESPONSE_LIMIT_EXCEED;
import static iudx.rs.proxy.metering.util.Constants.RESPONSE_SIZE_COLUMN;
import static iudx.rs.proxy.metering.util.Constants.RESULTS;
import static iudx.rs.proxy.metering.util.Constants.START_TIME;
import static iudx.rs.proxy.metering.util.Constants.TIME;
import static iudx.rs.proxy.metering.util.Constants.TIME_COLUMN;
import static iudx.rs.proxy.metering.util.Constants.TIME_NOT_FOUND;
import static iudx.rs.proxy.metering.util.Constants.TIME_RELATION;
import static iudx.rs.proxy.metering.util.Constants.TIME_RELATION_NOT_FOUND;
import static iudx.rs.proxy.metering.util.Constants.TITLE;
import static iudx.rs.proxy.metering.util.Constants.TOTAL_HITS;
import static iudx.rs.proxy.metering.util.Constants.TYPE_KEY;
import static iudx.rs.proxy.metering.util.Constants.USERID_COLUMN;
import static iudx.rs.proxy.metering.util.Constants.USERID_NOT_FOUND;
import static iudx.rs.proxy.metering.util.Constants.USER_ID;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import iudx.rs.proxy.common.Response;
import iudx.rs.proxy.common.ResponseUrn;
import iudx.rs.proxy.metering.util.QueryBuilder;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MeteringServiceImpl implements MeteringService {

  private static final Logger LOGGER = LogManager.getLogger(MeteringServiceImpl.class);
  public final String _COUNT_COLUMN;
  public final String _RESOURCE_ID_COLUMN;
  public final String _API_COLUMN;
  public final String _USERID_COLUMN;
  public final String _TIME_COLUMN;
  public final String _RESPONSE_SIZE_COLUMN;
  public final String _ID_COLUMN;
  private final Vertx vertx;
  private final QueryBuilder queryBuilder = new QueryBuilder();
  PgConnectOptions connectOptions;
  PoolOptions poolOptions;
  PgPool pool;
  private JsonObject query = new JsonObject();
  private String databaseIP;
  private int databasePort;
  private String databaseName;
  private String databaseUserName;
  private String databasePassword;
  private int databasePoolSize;
  private String databaseTableName;

  public MeteringServiceImpl(JsonObject propObj, Vertx vertxInstance) {

    if (propObj != null && !propObj.isEmpty()) {
      databaseIP = propObj.getString(DATABASE_IP);
      databasePort = propObj.getInteger(DATABASE_PORT);
      databaseName = propObj.getString(DATABASE_NAME);
      databaseUserName = propObj.getString(DATABASE_USERNAME);
      databasePassword = propObj.getString(DATABASE_PASSWORD);
      databaseTableName = propObj.getString(DATABASE_TABLE_NAME);
      databasePoolSize = propObj.getInteger(POOL_SIZE);
    }

    this.connectOptions =
        new PgConnectOptions().setPort(databasePort).setHost(databaseIP).setDatabase(databaseName)
            .setUser(databaseUserName).setPassword(databasePassword).setReconnectAttempts(2)
            .setReconnectInterval(1000);

    this.poolOptions = new PoolOptions().setMaxSize(databasePoolSize);
    this.pool = PgPool.pool(vertxInstance, connectOptions, poolOptions);
    this.vertx = vertxInstance;

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
        RESPONSE_SIZE_COLUMN.insert(0, "(" + databaseName + "." + databaseTableName + ".")
            .toString();
    _ID_COLUMN = ID_COLUMN.insert(0, "(" + databaseName + "." + databaseTableName + ".").toString();
  }

  @Override
  public MeteringService executeReadQuery(JsonObject request,
                                          Handler<AsyncResult<JsonObject>> handler) {

    LOGGER.trace("Info: Read Query" + request.toString());

    if (request.getString(ENDPOINT).equals(IUDX_PROVIDER_AUDIT_URL) &&
        request.getString(PROVIDER_ID) == null) {
      Response response =
          new Response.Builder().withUrn(ResponseUrn.INVALID_PROVIDER_ID_VALUE_URN.getUrn())
              .withStatus(HttpStatus.SC_BAD_REQUEST).withDetail(INVALID_PROVIDER_REQUIRED).build();
      handler.handle(Future.failedFuture(response.toString()));
      return this;
    }

    if (request.getString(TIME_RELATION) == null ||
        !(request.getString(TIME_RELATION).equals(DURING) ||
            request.getString(TIME_RELATION).equals(BETWEEN))) {
      LOGGER.debug("Info: " + TIME_RELATION_NOT_FOUND);
      Response response =
          new Response.Builder().withUrn(ResponseUrn.INVALID_TEMPORAL_REL_URN.getUrn())
              .withStatus(HttpStatus.SC_BAD_REQUEST).withDetail(TIME_RELATION_NOT_FOUND).build();
      handler.handle(Future.failedFuture(response.toString()));
      return this;
    }

    if (request.getString(START_TIME) == null || request.getString(END_TIME) == null) {
      LOGGER.debug("Info: " + TIME_NOT_FOUND);
      Response response =
          new Response.Builder().withUrn(ResponseUrn.INVALID_TEMPORAL_PARAM_URN.getUrn())
              .withStatus(HttpStatus.SC_BAD_REQUEST).withDetail(TIME_NOT_FOUND).build();
      handler.handle(Future.failedFuture(response.toString()));
      return this;
    }

    if (request.getString(USER_ID) == null || request.getString(USER_ID).isEmpty()) {
      LOGGER.debug("Info: " + USERID_NOT_FOUND);
      Response response = new Response.Builder().withUrn(ResponseUrn.INVALID_ID_VALUE_URN.getUrn())
          .withStatus(HttpStatus.SC_BAD_REQUEST).withDetail(USERID_NOT_FOUND).build();
      handler.handle(Future.failedFuture(response.toString()));
      return this;
    }
    request.put(DATABASE_TABLE_NAME, databaseTableName);
    query = queryBuilder.buildCountQuery(request);

    LOGGER.trace(query);
    if (query.containsKey(ERROR)) {
      LOGGER.error("Fail: Query returned with an error: " + query.getString(ERROR));
      Response response =
          new Response.Builder().withUrn(ResponseUrn.INVALID_PARAM_VALUE_URN.getUrn())
              .withStatus(HttpStatus.SC_BAD_REQUEST).withDetail(query.getString(ERROR)).build();
      handler.handle(Future.failedFuture(response.toString()));
      return this;
    }
    LOGGER.debug("Info: Query constructed: " + query.getString(QUERY_KEY));

    Future<JsonObject> countResult = executeCountQuery(query);
    countResult.onComplete(countResultHandler -> {
      if (countResultHandler.succeeded()) {
        int totalCount = Integer.parseInt(
            countResultHandler.result().getJsonArray(RESULTS).getJsonObject(0)
                .getString(TOTAL_HITS));
        if (request.getString(HEADER_OPTIONS) != null) {
          handler.handle(Future.succeededFuture(countResultHandler.result()));
        } else {

          if (totalCount >= 10000 || totalCount == 0) {
            Response response =
                new Response.Builder().withUrn(ResponseUrn.INVALID_PARAM_VALUE_URN.getUrn())
                    .withStatus(HttpStatus.SC_BAD_REQUEST).withDetail(RESPONSE_LIMIT_EXCEED)
                    .build();
            handler.handle(Future.failedFuture(response.toString()));
          } else {
            query = queryBuilder.buildReadingQuery(request);
            query.put(COUNT, totalCount);
            Future<JsonObject> initialReadResult = executeReadQuery(query);

            initialReadResult.onComplete(initialReadHandler -> {
              if (initialReadHandler.succeeded()) {
                Future<JsonObject> remainingReadResult = executeRemainingReadQuery(query);
                remainingReadResult.onComplete(remainingReadHandler -> {
                  if (remainingReadHandler.succeeded()) {
                    JsonArray jsonArray =
                        remainingReadHandler.result().getJsonArray(RESPONSE_ARRAY);
                    JsonObject response =
                        new JsonObject().put(TYPE_KEY, ResponseUrn.SUCCESS_URN.getUrn())
                            .put(TITLE, ResponseUrn.SUCCESS_URN.getMessage())
                            .put(TOTAL_HITS, totalCount).put(RESULTS, jsonArray);
                    handler.handle(Future.succeededFuture(response));
                  } else {
                    LOGGER.info("FAILED " + remainingReadHandler.cause());
                  }
                });
              }
            });
          }
        }
      } else if (countResultHandler.failed()) {
        LOGGER.error("Read from DB failed:" + countResultHandler.cause());
        handler.handle(Future.failedFuture(countResultHandler.cause().getMessage()));
      }
    });
    return this;
  }

  /* First time read query */
  private Future<JsonObject> executeReadQuery(JsonObject query) {
    Promise<JsonObject> promise = Promise.promise();
    JsonArray jsonArray = new JsonArray();
    pool.withConnection(connection -> connection.query(query.getString(QUERY_KEY)).execute())
        .onSuccess(rows -> {
          String lastId = "";
          for (Row rs : rows) {
            JsonObject temp = new JsonObject();
            temp.put(ID, rs.getString(_RESOURCE_ID_COLUMN));
            temp.put(TIME, rs.getString(_TIME_COLUMN));
            temp.put(API, rs.getString(_API_COLUMN));
            temp.put(CONSUMER, rs.getString(_USERID_COLUMN));
            lastId = rs.getString(_ID_COLUMN);
            jsonArray.add(temp);
          }
          query.put(LAST_ID, lastId);
          query.put(LATEST_ID, lastId);
          query.put(RESPONSE_ARRAY, jsonArray);
          promise.complete(query);
        }).onFailure(event -> {
          promise.fail("Failed to get connection from the database");
        });
    return promise.future();
  }

  private Future<JsonObject> executeCountQuery(JsonObject query) {
    Promise<JsonObject> promise = Promise.promise();
    pool.withConnection(connection -> connection.query(query.getString(QUERY_KEY)).execute())
        .onSuccess(rows -> {
          int count = 0;
          for (Row rs : rows) {
            LOGGER.debug("COUNT: " + (rs.getInteger(_COUNT_COLUMN)));
            count = rs.getInteger(_COUNT_COLUMN);
          }
          JsonObject response = new JsonObject().put(TYPE_KEY, ResponseUrn.SUCCESS_URN.getUrn())
              .put(TITLE, ResponseUrn.SUCCESS_URN.getMessage())
              .put(RESULTS, new JsonArray().add(new JsonObject().put(TOTAL_HITS, count)));
          promise.complete(response);
        }).onFailure(event -> {
          promise.fail("Failed to get connection from the database");
        });
    return promise.future();
  }

  private Future<JsonObject> executeRemainingReadQuery(JsonObject query) {
    final Promise<JsonObject> promise = Promise.promise();
    executeRemainingReadQuery(promise, query);
    return promise.future();
  }

  private void executeRemainingReadQuery(final Promise<JsonObject> promise,
                                         final JsonObject query) {
    String latestId = query.getString(LATEST_ID);
    JsonArray jsonArray = query.getJsonArray(RESPONSE_ARRAY);
    if (latestId.isEmpty()) {
      promise.complete(query);
    } else {
      String tempQuery = queryBuilder.buildTempReadQuery(query);
      query.put(QUERY_KEY, tempQuery);
      query.put(LAST_ID, query.getValue(LATEST_ID));
      pool.withConnection(connection -> connection.query(tempQuery).execute()).onComplete(rows -> {
        String currId = "";
        RowSet<Row> result = rows.result();
        for (Row rs : result) {
          JsonObject temp = new JsonObject();
          temp.put(ID, rs.getString(_RESOURCE_ID_COLUMN));
          temp.put(TIME, rs.getString(_TIME_COLUMN));
          temp.put(API, rs.getString(_API_COLUMN));
          temp.put(CONSUMER, rs.getString(_USERID_COLUMN));
          currId = rs.getString(_ID_COLUMN);
          jsonArray.add(temp);
        }
        query.put(LATEST_ID, currId);
        executeRemainingReadQuery(promise, query);
      });
    }
  }

  @Override
  public MeteringService executeWriteQuery(JsonObject request,
                                           Handler<AsyncResult<JsonObject>> handler) {
    request.put(DATABASE_TABLE_NAME, databaseTableName);
    query = queryBuilder.buildWritingQuery(request);
    Future<JsonObject> result = writeInDatabase(query);
    result.onComplete(resultHandler -> {
      if (resultHandler.succeeded()) {
        handler.handle(Future.succeededFuture(resultHandler.result()));
      } else if (resultHandler.failed()) {
        LOGGER.error("failed ::" + resultHandler.cause());
        handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
      }
    });
    return this;
  }

  private Future<JsonObject> writeInDatabase(JsonObject query) {
    Promise<JsonObject> promise = Promise.promise();
    pool.withConnection(connection -> connection.query(query.getString(QUERY_KEY)).execute())
        .onComplete(rows -> {
          if (rows.succeeded()) {
            JsonObject response = new JsonObject().put(TYPE_KEY, ResponseUrn.SUCCESS_URN.getUrn())
                .put(TITLE, ResponseUrn.SUCCESS_URN.getMessage());
            promise.complete(response);
          }
          if (rows.failed()) {
            LOGGER.error("Info: failed :" + rows.cause());
            Response response = new Response.Builder().withUrn(ResponseUrn.BAD_REQUEST_URN.getUrn())
                .withStatus(HttpStatus.SC_BAD_REQUEST).withDetail(rows.cause().getMessage())
                .build();
            LOGGER.info("Info: " + response);
            promise.fail(response.toString());
          }
        });
    return promise.future();
  }
}