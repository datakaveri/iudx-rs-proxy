package iudx.rs.proxy.authenticator.authorization;

import iudx.rs.proxy.authenticator.model.JwtData;

public interface AuthorizationStrategy {

  boolean isAuthorized(AuthorizationRequest authRequest,JwtData jwtData);

}
