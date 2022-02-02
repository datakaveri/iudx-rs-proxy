package iudx.rs.proxy.authenticator;

import static iudx.rs.proxy.common.Constants.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;

public class AuthenticationVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(AuthenticationVerticle.class);

  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;
  private AuthenticationService authenticationService;

  @Override
  public void start() {

    binder = new ServiceBinder(vertx);
    authenticationService = new JwtAuthenticationServiceImpl();
    consumer = binder.setAddress(AUTH_SERVICE_ADDRESS).register(AuthenticationService.class,
        authenticationService);
    LOGGER.info("auth service deployed");
  }
}
