package iudx.rs.proxy.authenticator.authorization;

import java.util.Objects;

public final class AuthorizationRequest {
  private final Method method;
  private final String api;

  public AuthorizationRequest(final Method method, final String api) {
    this.method = method;
    this.api = api;
  }

  public Method getMethod() {
    return method;
  }

  public String getApi() {
    return api;
  }

  @Override
  public String toString() {
    return "AuthorizationRequest [method=" + method + ", api=" + api + "]";
  }

  @Override
  public int hashCode() {
    return Objects.hash(api, method);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    AuthorizationRequest other = (AuthorizationRequest) obj;
    return Objects.equals(api, other.api) && method == other.method;
  }
}
