package iudx.rs.proxy.authenticator.authorization;

import static iudx.rs.proxy.authenticator.authorization.Api.ENTITIES;
import static iudx.rs.proxy.authenticator.authorization.Api.TEMPORAL;
import static iudx.rs.proxy.authenticator.authorization.Api.CONSUMER_AUDIT;
import static iudx.rs.proxy.authenticator.authorization.Method.GET;

import io.vertx.core.json.JsonArray;
import iudx.rs.proxy.authenticator.model.JwtData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConsumerAuthStrategy implements AuthorizationStrategy {

  private static final Logger LOGGER = LogManager.getLogger(ConsumerAuthStrategy.class);

  static Map<String, List<AuthorizationRequest>> consumerAuthorizationRules = new HashMap<>();
  static {
    // api access list/rules
    List<AuthorizationRequest> apiAccessList = new ArrayList<>();
    apiAccessList.add(new AuthorizationRequest(GET, TEMPORAL));
    apiAccessList.add(new AuthorizationRequest(GET, CONSUMER_AUDIT));
    apiAccessList.add(new AuthorizationRequest(GET, ENTITIES));

    consumerAuthorizationRules.put(IudxAccess.API.getAccess(), apiAccessList);
  }


  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    JsonArray access = jwtData.getCons() != null ? jwtData.getCons().getJsonArray("access") : null;
    boolean result = false;
    if (access == null) {
      return result;
    }
    String endpoint = authRequest.getApi().getApiEndpoint();
    Method method = authRequest.getMethod();
    LOGGER.debug("authorization request for : " + endpoint + " with method : " + method.name());
    LOGGER.debug("allowed access : " + access);

    if (!result && access.contains(IudxAccess.API.getAccess())) {
      result = consumerAuthorizationRules.get(IudxAccess.API.getAccess()).contains(authRequest);
    }

    return result;
  }

}
