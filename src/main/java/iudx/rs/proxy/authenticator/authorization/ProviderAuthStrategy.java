package iudx.rs.proxy.authenticator.authorization;

import iudx.rs.proxy.authenticator.model.JwtData;
import iudx.rs.proxy.common.Api;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProviderAuthStrategy implements AuthorizationStrategy {

  static Map<String, List<AuthorizationRequest>> providerAuthorizationRules = new HashMap<>();
  private static volatile ProviderAuthStrategy instance;

  private ProviderAuthStrategy(Api apis) {
    buildPermissions(apis);
  }

  public static ProviderAuthStrategy getInstance(Api api) {
    if (instance == null) {
      synchronized (ProviderAuthStrategy.class) {
        if (instance == null) {
          instance = new ProviderAuthStrategy(api);
        }
      }
    }
    return instance;
  }

  private void buildPermissions(Api api) {}

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return true;
  }
}
