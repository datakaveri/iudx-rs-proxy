package iudx.rs.proxy.authenticator.authorization;

import java.util.stream.Stream;

public enum IudxRole {
  
  CONSUMER("consumer");

  private final String role;

  IudxRole(String role) {
    this.role = role;
  }

  public static IudxRole fromRole(final String role) {
    return Stream.of(values())
        .filter(v -> v.role.equalsIgnoreCase(role))
        .findAny()
        .orElse(null);
  }

}
