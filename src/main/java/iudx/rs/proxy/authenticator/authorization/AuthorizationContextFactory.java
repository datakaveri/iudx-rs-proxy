package iudx.rs.proxy.authenticator.authorization;

import iudx.rs.proxy.common.Api;

public class AuthorizationContextFactory {

  public static AuthorizationStrategy create(IudxRole role,Api apis) {
    switch (role) {
      case CONSUMER: {
        return ConsumerAuthStrategy.getInstance(apis);
      }
      case PROVIDER: {
        return ProviderAuthStrategy.getInstance(apis);
      }
      case DELEGATE: {
        return DelegateAuthStrategy.getInstance(apis);
      }
      default:
        throw new IllegalArgumentException(role + "role is not defined in IUDX");
    }
  }

}
