package iudx.rs.proxy.authenticator.authorization;

import iudx.rs.proxy.authenticator.model.JwtData;
import iudx.rs.proxy.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DelegateAuthStrategy implements AuthorizationStrategy {

  private static final Logger LOGGER = LogManager.getLogger(DelegateAuthStrategy.class);

  static Map<String, List<AuthorizationRequest>> providerAuthorizationRules = new HashMap<>();
  private final Api apis;
  private static volatile DelegateAuthStrategy instance;

  private DelegateAuthStrategy(Api apis) {
    this.apis=apis;
    buildPermissions(apis);
  }

  public static DelegateAuthStrategy getInstance(Api api)
  {
    if (instance == null)
    {
      synchronized (DelegateAuthStrategy.class)
      {
        if (instance == null)
        {
          instance = new DelegateAuthStrategy(api);
        }
      }
    }
    return instance;
  }
  private void buildPermissions(Api api) {
    // delegate allowed to access all endpoints

  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return true;
  }
}
