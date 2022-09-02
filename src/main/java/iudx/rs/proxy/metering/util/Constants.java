package iudx.rs.proxy.metering.util;

public class Constants {

  public static final String ID = "id";
  /* Temporal */
  public static final String START_TIME = "startTime";
  public static final String TIME = "time";
  public static final String END_TIME = "endTime";
  public static final String BETWEEN = "between";
  public static final String TIME_RELATION = "timeRelation";
  public static final String DURING = "during";
  public static final String HEADER_OPTIONS = "options";
  public static final String IUDX_ADAPTOR_URL = "/ngsi-ld/v1";
  public static final String IUDX_PROVIDER_AUDIT_URL = IUDX_ADAPTOR_URL + "/provider/audit";

  /* configs */
  public static final String DATABASE_IP = "meteringDatabaseIP";
  public static final String DATABASE_PORT = "meteringDatabasePort";
  public static final String DATABASE_NAME = "meteringDatabaseName";
  public static final String DATABASE_USERNAME = "meteringDatabaseUserName";
  public static final String DATABASE_PASSWORD = "meteringDatabasePassword";
  public static final String DATABASE_TABLE_NAME = "meteringDatabaseTableName";
  public static final String POOL_SIZE = "meteringPoolSize";


  /* Errors */
  public static final String SUCCESS = "successful operations";
  public static final String DETAIL = "detail";
  public static final String TITLE = "title";
  public static final String RESULTS = "results";
  /* Database */
  public static final String ERROR = "Error";
  public static final String QUERY_KEY = "query";
  public static final String TYPE_KEY = "type";
  public static final String PROVIDER_ID = "providerID";
  public static final String CONSUMER_ID = "consumerID";
  public static final String ENDPOINT = "endPoint";
  public static final String IID = "iid";
  public static final String RESOURCE_ID = "resourceId";
  public static final String RESPONSE_SIZE = "response_size";
  public static final String LATEST_ID = "latestId";
  public static final String LAST_ID = "lastId";
  public static final String WHERE = "where";

  /* Metering Service Constants*/
  public static final String TIME_RELATION_NOT_FOUND = "Time relation not found.";
  public static final String TIME_NOT_FOUND = "Time interval not found.";
  public static final String USERID_NOT_FOUND = "User Id not found.";
  public static final String INVALID_DATE_TIME = "invalid date-time";
  public static final String INVALID_PROVIDER_ID = "invalid provider id.";
  public static final String INVALID_PROVIDER_REQUIRED = "provider id required.";
  public static final String INVALID_DATE_DIFFERENCE =
      "Difference between dates cannot be greater than 14 days or less than zero day.";
  public static final String RESOURCE_QUERY = " and resourceId='$4'";

  public static final String CONSUMER_ID_TIME_INTERVAL_COUNT_QUERY =
      "SELECT count(*) FROM $0 where epochtime>=$1 and epochtime<=$2 and userid='$3'";

  public static final String PROVIDER_ID_TIME_INTERVAL_COUNT_QUERY =
      "SELECT count(*) FROM $0 where epochtime>=$1 and epochtime<=$2 and providerid='$3'";

  public static final String CONSUMER_ID_TIME_INTERVAL_READ_QUERY =
      "SELECT * FROM $0 where epochtime>=$1 and epochtime<=$2 and userid='$3'";

  public static final String PROVIDER_ID_TIME_INTERVAL_READ_QUERY =
      "SELECT * FROM $0 where epochtime>=$1 and epochtime<=$2 and providerid='$3'";
  public static final String ORDER_BY_AND_LIMIT = " ORDER BY ID LIMIT 999;";
  public static final String API_QUERY = " and api='$5'";
  public static final String USER_ID_QUERY = " and userid='$6'";
  public static final String ID_QUERY = " id>'$7' and";

  public static final String API = "api";
  public static final String USER_ID = "userid";
  public static final String CONSUMER = "consumer";
  public static final String COUNT = "count";
  public static final String RESPONSE_ARRAY = "responseArray";
  public static final String RESPONSE_LIMIT_EXCEED = "Requested time range exceeds response limit";
  public static final String TOTAL_HITS = "totalHits";
  public static final String WRITE_QUERY =
      "INSERT INTO $0 (id,api,userid,epochtime,resourceid,isotime,providerid,size) VALUES ('$1','$2','$3',$4,'$5','$6','$7',$8)";
  public static final StringBuilder COUNT_COLUMN = new StringBuilder("col0)");
  public static final StringBuilder RESOURCE_ID_COLUMN = new StringBuilder("resourceid)");
  public static final StringBuilder API_COLUMN = new StringBuilder("api)");
  public static final StringBuilder USERID_COLUMN = new StringBuilder("userid)");
  public static final StringBuilder TIME_COLUMN = new StringBuilder("isotime)");
  public static final StringBuilder RESPONSE_SIZE_COLUMN = new StringBuilder("size)");
  public static final StringBuilder ID_COLUMN = new StringBuilder("id)");
}
