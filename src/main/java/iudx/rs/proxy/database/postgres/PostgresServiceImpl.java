package iudx.rs.proxy.database.postgres;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlResult;
import iudx.rs.proxy.common.Response;
import iudx.rs.proxy.common.ResponseUrn;
import iudx.rs.proxy.database.DatabaseService;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PostgresServiceImpl implements DatabaseService {
  private static final Logger LOGGER = LogManager.getLogger(PostgresServiceImpl.class);
  private final PgPool pgClient;

  public PostgresServiceImpl(PgPool pgClient) {
    this.pgClient = pgClient;
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
}
