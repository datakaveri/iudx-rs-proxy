package iudx.rs.proxy.cache.cacheImpl;

public enum CacheType {
  REVOKED_CLIENT("revoked_client"),
  CATALOGUE_CACHE("catalogue_cache");

  String cacheName;

  CacheType(String name) {
    this.cacheName = name;
  }


}
