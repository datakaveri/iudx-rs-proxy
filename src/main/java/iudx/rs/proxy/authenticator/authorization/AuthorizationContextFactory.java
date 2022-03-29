package iudx.rs.proxy.authenticator.authorization;

public class AuthorizationContextFactory {


  private final static AuthorizationStrategy consumerAuth = new ConsumerAuthStrategy();

  public static AuthorizationStrategy create(IudxRole role) {
    switch (role) {
      case CONSUMER: {
        return consumerAuth;
      }
      default:
        throw new IllegalArgumentException(role + "role is not defined in IUDX");
    }
  }

}
