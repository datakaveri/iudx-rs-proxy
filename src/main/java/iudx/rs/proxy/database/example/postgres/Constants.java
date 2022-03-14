package iudx.rs.proxy.database.example.postgres;

public class Constants {
  // configs
  public static final String DATABASE_IP = "databaseIp";
  public static final String DATABASE_PORT = "databasePort";
  public static final String DATABASE_NAME = "databaseName";
  public static final String DATABASE_USERNAME = "databaseUserName";
  public static final String DATABASE_PASSWORD = "databasePassword";
  public static final String POOL_SIZE = "poolSize";

  // json
  public static final String ID = "id";
  public static final String SEARCH_TYPE = "searchType";
  public static final String TIME_REL = "timerel";
  public static final String TIME = "time";
  public static final String END_TIME = "endTime";
  public static final String ATTRS = "attrs";
  public static final String BEFORE = "before";
  public static final String AFTER = "after";
  public static final String ATTR_QUERY = "attr_query";
  public static final String ATTRIBUTE = "attribute";
  public static final String OPERATOR = "operator";
  public static final String VALUE = "value";

  //regex
  public static final String GEOSEARCH_REGEX = "(.*)geoSearch(.*)";
  public static final String ATTRIBUTE_SEARCH_REGEX = "(.*)attributeSearch(.*)";
  public static final String TEMPORAL_SEARCH_REGEX = "(.*)temporalSearch(.*)";

  // SQL
  public static String PSQL_TABLE_EXISTS_QUERY =
      "SELECT EXISTS ( SELECT FROM pg_tables WHERE schemaname='public' AND tablename='$1');";
  public static String PSQL_SELECT_QUERY = "SELECT $1 FROM $$";
  public static String PSQL_TEMPORAL_CONDITION = " WHERE time BETWEEN '$2' and '$3'";
  public static String PSQL_ATTR_CONDITION = "$4 $op $5";
}
