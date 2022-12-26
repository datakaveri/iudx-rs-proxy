package iudx.rs.proxy.authenticator.authorization;

import iudx.rs.proxy.authenticator.model.JwtData;
import iudx.rs.proxy.common.Api;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProviderAuthStrategy implements AuthorizationStrategy {

  private static final Logger LOGGER = LogManager.getLogger(ProviderAuthStrategy.class);

  static Map<String, List<AuthorizationRequest>> providerAuthorizationRules = new HashMap<>();
  private final Api apis;
  public ProviderAuthStrategy(Api apis) {
    this.apis=apis;
    buildPermissions(apis);
  }
  
  private void buildPermissions(Api api) {
    
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return true;
  }
}
