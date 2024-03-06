package iudx.rs.proxy.metering.util;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

import static iudx.rs.proxy.apiserver.util.ApiServerConstants.LIMITPARAM;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.OFFSETPARAM;
import static iudx.rs.proxy.metering.util.Constants.*;

public class QueryBuilder {

  private static final Logger LOGGER = LogManager.getLogger(QueryBuilder.class);

  public JsonObject buildMessageForRMQ(JsonObject request) {
    if (!request.containsKey(ORIGIN)) {
      String primaryKey = UUID.randomUUID().toString().replace("-", "");
      String userId = request.getString(USER_ID);
      request.put(PRIMARY_KEY, primaryKey);
      request.put(USER_ID, userId);
      request.put(ORIGIN, ORIGIN_SERVER);
      LOGGER.trace("Info: Request " + request);
    }
    return request;
  }

  public JsonObject buildReadQueryFromPG(JsonObject request) {
    String startTime = request.getString(START_TIME);
    String endTime = request.getString(END_TIME);
    String userId = request.getString(USER_ID);
    String providerID = request.getString(PROVIDER_ID);
    String databaseTableName = request.getString(TABLE_NAME);
    StringBuilder query;
    String api = request.getString(API);
    String resourceId = request.getString(RESOURCE_ID);
    String consumerID = request.getString(CONSUMER_ID);
    String limit = request.getString(LIMITPARAM);;
    String offset = request.getString(OFFSETPARAM);;

    if (providerID != null) {
      query =
          new StringBuilder(
              PROVIDERID_TIME_INTERVAL_READ_QUERY
                  .replace("$0", databaseTableName)
                  .replace("$1", startTime)
                  .replace("$2", endTime)
                  .replace("$3", providerID));
      if (api != null) {
        query.append(" and api = '$5' ".replace("$5", api));
      }
      if (resourceId != null) {
        query.append(" and resourceid = '$6' ".replace("$6", resourceId));
      }
      if (consumerID != null) {
        query.append(" and userid='$6' ".replace("$6", userId));
      }
    } else {
      query =
          new StringBuilder(
              CONSUMERID_TIME_INTERVAL_READ_QUERY
                  .replace("$0", databaseTableName)
                  .replace("$1", startTime)
                  .replace("$2", endTime)
                  .replace("$3", userId));
      if (api != null) {
        query.append(" and api = '$5' ".replace("$5", api));
      }
      if (resourceId != null) {
        query.append(" and resourceid = '$6' ".replace("$6", resourceId));
      }
    }
    query.append(" limit $7"
            .replace("$7", limit));

    query.append(" offset $8"
            .replace("$8", offset));
    return new JsonObject().put(QUERY_KEY, query);
  }

  public JsonObject buildCountReadQueryFromPG(JsonObject request) {
    String startTime = request.getString(START_TIME);
    String endTime = request.getString(END_TIME);
    String userId = request.getString(USER_ID);
    String providerID = request.getString(PROVIDER_ID);
    String databaseTableName = request.getString(TABLE_NAME);
    StringBuilder query;
    String api = request.getString(API);
    String resourceId = request.getString(RESOURCE_ID);
    String consumerID = request.getString(CONSUMER_ID);

    if (providerID != null) {
      query =
          new StringBuilder(
              PROVIDERID_TIME_INTERVAL_COUNT_QUERY
                  .replace("$0", databaseTableName)
                  .replace("$1", startTime)
                  .replace("$2", endTime)
                  .replace("$3", providerID));
      if (api != null) {
        query.append(" and api = '$5' ".replace("$5", api));
      }
      if (resourceId != null) {
        query.append(" and resourceid = '$6' ".replace("$6", resourceId));
      }
      if (consumerID != null) {
        query.append(" and userid='$6' ".replace("$6", userId));
      }

    } else {
      query =
          new StringBuilder(
              CONSUMERID_TIME_INTERVAL_COUNT_QUERY
                  .replace("$0", databaseTableName)
                  .replace("$1", startTime)
                  .replace("$2", endTime)
                  .replace("$3", userId));
      if (api != null) {
        query.append(" and api = '$5' ".replace("$5", api));
      }
      if (resourceId != null) {
        query.append(" and resourceid = '$6' ".replace("$6", resourceId));
      }
    }
    return new JsonObject().put(QUERY_KEY, query);
  }
}
