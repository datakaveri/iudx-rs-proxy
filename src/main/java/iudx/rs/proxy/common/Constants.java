package iudx.rs.proxy.common;

public class Constants {

  public static final String AUTH_SERVICE_ADDRESS = "iudx.rs.proxy.auth.service";
  public static final String DB_SERVICE_ADDRESS = "iudx.rs.proxy.db.service";
  public static final String CACHE_SERVICE_ADDRESS = "iudx.rs.proxy.cache.service";
  public static final String DATABASE_SERVICE_ADDRESS = "iudx.rs.proxy.database.service";
  public static final String METERING_SERVICE_ADDRESS = "iudx.rs.proxy.metering.service";
  public static final String DATABROKER_SERVICE_ADDRESS = "iudx.rs.proxy.broker.service";
  public static final String CONSEENTLOG_SERVICE_ADDRESS = "iudx-rs-proxy-optional-consentlogs";

  // postgres queries
  public static String SELECT_REVOKE_TOKEN_SQL = "SELECT * FROM revoked_tokens";
  public static String SELECT_UNIQUE_ATTRIBUTE = "SELECT * from unique_attributes";
  public static String ISERT_ASYNC_REQUEST_DETAIL_SQL =
      "INSERT INTO async_request_detail(search_id, consumer_id, resource_id, request_query) "
          + "values('$0','$1','$2','$3'::JSON)";
  public static String SELECT_ASYNC_DETAILS =
      "SELECT consumer_id,search_id,resource_id FROM async_request_detail WHERE search_id ='$0'";
}
