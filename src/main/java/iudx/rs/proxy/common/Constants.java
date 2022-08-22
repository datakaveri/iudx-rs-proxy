package iudx.rs.proxy.common;

public class Constants {
  
  public final static String AUTH_SERVICE_ADDRESS="iudx.rs.proxy.auth.service";
  public final static String DB_SERVICE_ADDRESS="iudx.rs.proxy.db.service";
  public static final String CACHE_SERVICE_ADDRESS = "iudx.rs.proxy.cache.service";
  public static final String DATABASE_SERVICE_ADDRESS ="iudx.rs.proxy.database.service";
  public static final String METERING_SERVICE_ADDRESS = "iudx.rs.proxy.metering.service";
  public static final String DATABROKER_SERVICE_ADDRESS = "iudx.rs.proxy.broker.service";


  // postgres queries
  public static String SELECT_REVOKE_TOKEN_SQL = "SELECT * FROM revoked_tokens";
  public static String SELECT_UNIQUE_ATTRIBUTE = "SELECT * from unique_attributes";

}
