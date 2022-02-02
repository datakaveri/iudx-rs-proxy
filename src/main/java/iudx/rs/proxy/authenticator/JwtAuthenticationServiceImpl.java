package iudx.rs.proxy.authenticator;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public class JwtAuthenticationServiceImpl implements AuthenticationService {

  @Override
  public AuthenticationService tokenInterospect(JsonObject request,
      JsonObject authenticationInfo, Handler<AsyncResult<JsonObject>> handler) {
    return null;
  }

}
