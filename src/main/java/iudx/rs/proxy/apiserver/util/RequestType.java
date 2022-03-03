package iudx.rs.proxy.apiserver.util;


public enum RequestType {
  TEMPORAL("temporal");

  private String filename;

  private RequestType(String fileName) {
    this.filename = fileName;
  }
}
