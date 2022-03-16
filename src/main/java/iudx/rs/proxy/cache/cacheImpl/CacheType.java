package iudx.rs.proxy.cache.cacheImpl;

public enum CacheType {
  RESOURCE_GROUP("resource_group"),
  RESOURCE_ID("resource_Id");

  String cacheName;

  CacheType(String name) {
    this.cacheName = name;
  }


}
