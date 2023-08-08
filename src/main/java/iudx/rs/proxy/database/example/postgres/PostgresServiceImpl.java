package iudx.rs.proxy.database.example.postgres;

import static iudx.rs.proxy.database.example.postgres.Constants.*;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.serviceproxy.ServiceException;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlResult;
import iudx.rs.proxy.common.Response;
import iudx.rs.proxy.common.ResponseUrn;
import iudx.rs.proxy.database.DatabaseService;

public class PostgresServiceImpl implements DatabaseService {

  private final PgPool pgClient;
  private boolean exists;
  private Map<String, String> resourceGroup2TableMapping;
  private Map<String, String> iudxQueryOperator2PgMapping;

  private static final Logger LOGGER = LogManager.getLogger(PostgresServiceImpl.class);

  public PostgresServiceImpl(Vertx vertx, JsonObject config) {

    String databaseIP = config.getString(DATABASE_IP);
    int databasePort = config.getInteger(DATABASE_PORT);
    String databaseName = config.getString(DATABASE_NAME);
    String databaseUserName = config.getString(DATABASE_USERNAME);
    String databasePassword = config.getString(DATABASE_PASSWORD);
    int poolSize = config.getInteger(POOL_SIZE);

    PgConnectOptions connectOptions =
        new PgConnectOptions()
            .setPort(databasePort)
            .setHost(databaseIP)
            .setDatabase(databaseName)
            .setUser(databaseUserName)
            .setPassword(databasePassword);

    PoolOptions poolOptions = new PoolOptions().setMaxSize(poolSize);
    this.pgClient = PgPool.pool(vertx, connectOptions, poolOptions);

    resourceGroup2TableMapping = new HashMap<>();
    resourceGroup2TableMapping.put(
        "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood",
        "pune_flood");

    iudxQueryOperator2PgMapping = new HashMap<>();
    iudxQueryOperator2PgMapping.put("==", "=");
    iudxQueryOperator2PgMapping.put(">=", ">=");
    iudxQueryOperator2PgMapping.put("<=", "<=");
    iudxQueryOperator2PgMapping.put("<", "<");
    iudxQueryOperator2PgMapping.put(">", ">");
    iudxQueryOperator2PgMapping.put("!=", "!=");
  }

  @Override
  public DatabaseService searchQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler)
      throws ServiceException {

    if (!request.containsKey(ID)
        || request.getJsonArray(ID).isEmpty()
        || !request.containsKey(SEARCH_TYPE)) {
      throw new ServiceException(HttpStatus.SC_BAD_REQUEST, "message for failure");
    }

    String query = queryBuilder(request, false);

    Collector<Row, ?, List<JsonObject>> rowCollector =
        Collectors.mapping(Row::toJson, Collectors.toList());
    LOGGER.debug("query : " + query);
    pgClient
        .withConnection(
            connection ->
                connection.query(query).collecting(rowCollector).execute().map(SqlResult::value))
        .onSuccess(
            successHandler -> {
              long totalHits = successHandler.size();
              JsonArray response = new JsonArray(successHandler);
              handler.handle(
                  Future.succeededFuture(
                      new JsonObject()
                          .put("type", ResponseUrn.SUCCESS_URN.getUrn())
                          .put("title", "Success")
                          .put("totalHits", totalHits)
                          .put("results", response)));
            })
        .onFailure(
            failureHandler -> {
              LOGGER.debug(failureHandler);
              Response response =
                  new Response.Builder()
                      .withUrn(ResponseUrn.DB_ERROR_URN.getUrn())
                      .withStatus(HttpStatus.SC_BAD_REQUEST)
                      .withDetail(failureHandler.getLocalizedMessage())
                      .build();
              handler.handle(Future.failedFuture(response.toString()));
            });
    return this;
  }

  @Override
  public DatabaseService countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler)
      throws ServiceException {

    if (!request.containsKey(ID)
        || request.getJsonArray(ID).isEmpty()
        || !request.containsKey(SEARCH_TYPE)) {
      throw new ServiceException(HttpStatus.SC_BAD_REQUEST, "message for failure");
    }

    String query = queryBuilder(request, true);
    LOGGER.debug("query : " + query);
    pgClient
        .withConnection(
            sqlConnection ->
                sqlConnection
                    .query(query)
                    .execute()
                    .map(rows -> rows.iterator().next().getInteger(0)))
        .onSuccess(
            count -> {
              handler.handle(
                  Future.succeededFuture(
                      new JsonObject()
                          .put("type", ResponseUrn.SUCCESS_URN.getUrn())
                          .put("title", "Success")
                          .put(
                              "results",
                              new JsonArray().add(new JsonObject().put("totalHits", count)))));
            })
        .onFailure(
            failureHandler -> {
              LOGGER.debug(failureHandler);
              Response response =
                  new Response.Builder()
                      .withUrn(ResponseUrn.DB_ERROR_URN.getUrn())
                      .withStatus(HttpStatus.SC_BAD_REQUEST)
                      .withDetail(failureHandler.getLocalizedMessage())
                      .build();
              handler.handle(Future.failedFuture(response.toString()));
            });
    return this;
  }

  private String queryBuilder(JsonObject request, boolean isCount) {
    StringBuilder query;
    String searchType = request.getString(SEARCH_TYPE);
    String id = request.getJsonArray(ID).getString(0);
    String resourceGroup = getResourceGroup(id);
    String tableID = resourceGroup2TableMapping.get(resourceGroup);

    String[] attrs = null;

    if (request.containsKey(ATTRS)) {
      attrs = request.getJsonArray(ATTRS).stream().toArray(String[]::new);
    }

    String selection = PSQL_SELECT_QUERY.replace("$$", tableID).replace("$2", id);

    if (attrs == null || attrs.length == 0) {
      if (isCount) {
        query = new StringBuilder(selection.replace("$1", "count(*)"));
      } else {
        query = new StringBuilder(selection.replace("$1", "*"));
      }
    } else {

      query = new StringBuilder(selection.replace("$1", String.join(",", attrs)));
    }

    if (searchType.matches(TEMPORAL_SEARCH_REGEX)) {
      query = temporalQueryBuilder(request, query);
    }
    if (searchType.matches(GEOSEARCH_REGEX)) {
      query = new StringBuilder(geoQueryBuilder(request, query));
    }
    if (searchType.matches(ATTRIBUTE_SEARCH_REGEX)) {
      query = attrsQueryBuilder(request, query);
    }

    return query.toString();
  }

  private StringBuilder temporalQueryBuilder(JsonObject request, StringBuilder query) {
    String timerel = request.getString(TIME_REL);
    ZonedDateTime time, endTime;
    time = ZonedDateTime.parse(request.getString(TIME));

    if (timerel.equalsIgnoreCase(BEFORE)) {
      endTime = time;
      time = time.minusDays(10);
    } else if (timerel.equalsIgnoreCase(AFTER)) {
      endTime = time.plusDays(10);
    } else {
      endTime = ZonedDateTime.parse(request.getString(END_TIME));
    }
    if (query.toString().contains("WHERE")) {
      query.append(" AND ");
    } else {
      query.append(" WHERE ");
    }

    query.append(
        PSQL_TEMPORAL_CONDITION.replace("$2", time.toString()).replace("$3", endTime.toString()));

    return query;
  }

  private StringBuilder geoQueryBuilder(JsonObject request, StringBuilder query) {
    // TODO: implementation
    return null;
  }

  private StringBuilder attrsQueryBuilder(JsonObject request, StringBuilder query) {
    JsonObject attr_query = request.getJsonArray(ATTR_QUERY).getJsonObject(0);
    String attribute = attr_query.getString(ATTRIBUTE);
    String operator = attr_query.getString(OPERATOR);
    String value = attr_query.getString(VALUE);

    if (query.toString().contains("WHERE")) {
      query.append(" AND ");
    } else {
      query.append(" WHERE ");
    }
    query.append(
        PSQL_ATTR_CONDITION
            .replace("$4", attribute)
            .replace("$op", iudxQueryOperator2PgMapping.get(operator))
            .replace("$5", value));
    return query;
  }

  @Override
  public DatabaseService executeQuery(
      final JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
    LOGGER.info("In execute query");
    Collector<Row, ?, List<JsonObject>> rowCollector =
        Collectors.mapping(Row::toJson, Collectors.toList());
    String query = jsonObject.getString("query");

    pgClient
        .withConnection(
            connection ->
                connection.query(query).collecting(rowCollector).execute().map(SqlResult::value))
        .onSuccess(
            successHandler -> {
              LOGGER.info("In postgres success");
              JsonArray result = new JsonArray(successHandler);
              JsonObject responseJson =
                  new JsonObject()
                      .put("type", ResponseUrn.SUCCESS_URN.getUrn())
                      .put("title", ResponseUrn.SUCCESS_URN.getMessage())
                      .put("result", result);
              handler.handle(Future.succeededFuture(responseJson));
            })
        .onFailure(
            failureHandler -> {
              LOGGER.info("In postgres failed");

              LOGGER.debug(failureHandler);
              Response response =
                  new Response.Builder()
                      .withUrn(ResponseUrn.DB_ERROR_URN.getUrn())
                      .withStatus(HttpStatus.SC_BAD_REQUEST)
                      .withDetail(failureHandler.getLocalizedMessage())
                      .build();
              handler.handle(Future.failedFuture(response.toString()));
            });
    return this;
  }

  private String getResourceGroup(String id) {
    /*return id.substring(0, id.lastIndexOf('/'));*/
    return  id;
  }
}
