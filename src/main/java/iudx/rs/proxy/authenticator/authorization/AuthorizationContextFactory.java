package iudx.rs.proxy.authenticator.authorization;

public class AuthorizationContextFactory {


  private final static AuthorizationStrategy consumerAuth = new ConsumerAuthStrategy();
  private final static AuthorizationStrategy providerAuth = new ProviderAuthStrategy();

  public static AuthorizationStrategy create(IudxRole role) {
    switch (role) {
      case CONSUMER: {
        return consumerAuth;
      }
      case PROVIDER: {
        return providerAuth;
      }
      default:
        throw new IllegalArgumentException(role + "role is not defined in IUDX");
    }
  }

}
