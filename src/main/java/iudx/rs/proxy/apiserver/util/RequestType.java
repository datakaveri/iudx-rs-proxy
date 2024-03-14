package iudx.rs.proxy.apiserver.util;


public enum RequestType {
  ENTITY("entity"),
  TEMPORAL("temporal"),
  POST_TEMPORAL("post_temporal_schema.json"),
  POST_ENTITIES("post_entities_schema.json"),
  ASYNC_SEARCH("async_search"),
  ASYNC_STATUS("async_status"),;

  private String filename;

  public String getFilename() {
    return this.filename;
  }


  RequestType(String fileName) {
    this.filename = fileName;
  }
}
