package iudx.rs.proxy.authenticator.authorization;

import java.util.stream.Stream;

public enum IudxAccess {

  API("api"),
  ASYNC("async"),
  MANAGEMENT("management");

  private final String access;

  IudxAccess(String access) {
    this.access = access;
  }

  public static IudxAccess fromAccess(final String access) {
    return Stream.of(values())
        .filter(v -> v.access.equalsIgnoreCase(access))
        .findAny()
        .orElse(null);
  }

  public String getAccess() {
    return this.access;
  }

}
