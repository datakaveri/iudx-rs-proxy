package iudx.rs.proxy.database.example.postgres;

public class Constants {
  // configs
  public static final String DATABASE_IP = "databaseIp";
  public static final String DATABASE_PORT = "databasePort";
  public static final String DATABASE_NAME = "databaseName";
  public static final String DATABASE_USERNAME = "databaseUserName";
  public static final String DATABASE_PASSWORD = "databasePassword";
  public static final String POOL_SIZE = "poolSize";

  //json
  public static final String ID = "id";
  public static final String TIME_REL = "timerel";
  public static final String TIME = "time";
  public static final String END_TIME= "endTime";
  public static final String ATTRS = "attrs";
  public static final String BEFORE = "before";
  public static final String AFTER = "after";


  //SQL
  public static String PSQL_TABLE_EXISTS_QUERY =
      "SELECT EXISTS ( SELECT FROM pg_tables WHERE schemaname='public' AND tablename='$1');";
  public static String PSQL_SELECT_QUERY = "SELECT $1 FROM $$ WHERE time BETWEEN '$2' and '$3'";
}
