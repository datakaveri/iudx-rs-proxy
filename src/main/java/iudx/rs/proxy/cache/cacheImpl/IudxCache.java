package iudx.rs.proxy.cache.cacheImpl;

import io.vertx.core.json.JsonObject;

public interface IudxCache {

  void put(String key, String value);

  String get(JsonObject entries);

  JsonObject updateCache(JsonObject entries);

  void refreshCache();
}
