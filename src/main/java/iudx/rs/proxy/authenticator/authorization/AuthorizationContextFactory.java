package iudx.rs.proxy.authenticator.authorization;

import iudx.rs.proxy.common.Api;

public class AuthorizationContextFactory {

  public static AuthorizationStrategy create(IudxRole role,Api apis) {
    switch (role) {
      case CONSUMER: {
        return new ConsumerAuthStrategy(apis);
      }
      case PROVIDER: {
        return new ProviderAuthStrategy(apis);
      }
      default:
        throw new IllegalArgumentException(role + "role is not defined in IUDX");
    }
  }

}
