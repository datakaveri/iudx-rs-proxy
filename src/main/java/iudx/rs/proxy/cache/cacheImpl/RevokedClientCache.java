package iudx.rs.proxy.cache.cacheImpl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.rs.proxy.common.Constants;
import iudx.rs.proxy.database.DatabaseService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RevokedClientCache implements IudxCache {

  private static final Logger LOGGER = LogManager.getLogger(RevokedClientCache.class);
  private final static CacheType cacheType = CacheType.REVOKED_CLIENT;

  private final Cache<String, String> cache =
      CacheBuilder.newBuilder()
          .maximumSize(5000)
          .expireAfterWrite(1L, TimeUnit.DAYS)
          .build();

  private DatabaseService pgService;

  public RevokedClientCache(Vertx vertx, DatabaseService postgresService) {
    this.pgService = postgresService;
    refreshCache();

    vertx.setPeriodic(TimeUnit.HOURS.toMillis(1), handler -> {
      refreshCache();
    });
  }

  @Override
  public void put(String key, String value) {
    cache.put(key, value);
  }

  @Override
  public String get(String key) {
    return cache.getIfPresent(key);
  }

  @Override
  public void refreshCache() {
    LOGGER.trace(cacheType + " refreshCache() called");
    String query = Constants.SELECT_REVOKE_TOKEN_SQL;
    JsonObject jsonObject=new JsonObject().put("query",query);
    pgService.executeQuery(
        jsonObject,
        handler -> {
          if (handler.succeeded()) {
            JsonArray clientIdArray = handler.result().getJsonArray("result");
            cache.invalidateAll();
            clientIdArray.forEach(
                e -> {
                  JsonObject clientInfo = (JsonObject) e;
                  String key = clientInfo.getString("_id");
                  String value = clientInfo.getString("expiry");
                  this.cache.put(key, value);
                });
          }
        });
  }



}
