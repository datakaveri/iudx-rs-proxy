package iudx.rs.proxy.apiserver.handlers;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.rs.proxy.authenticator.model.JwtData;
import iudx.rs.proxy.metering.MeteringService;
import iudx.rs.proxy.optional.consentlogs.ConsentLoggingService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.rs.proxy.common.Constants.CONSEENTLOG_SERVICE_ADDRESS;
import static iudx.rs.proxy.common.Constants.METERING_SERVICE_ADDRESS;

public class ConsentLogRequestHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LogManager.getLogger(ConsentLogRequestHandler.class);

    ConsentLoggingService consentLoggingService;
    private boolean isAdexDeployment;
    private MeteringService meteringService;

    public ConsentLogRequestHandler(Vertx vertx, boolean isAdexDeployment) {
        this.isAdexDeployment = isAdexDeployment;
        consentLoggingService = ConsentLoggingService.createProxy(vertx, CONSEENTLOG_SERVICE_ADDRESS);
        meteringService = MeteringService.createProxy(vertx, METERING_SERVICE_ADDRESS);
    }

    @Override
    public void handle(RoutingContext context) {
        LOGGER.trace("ConsentLogRequestHandler started");

        if (isAdexDeployment) {
            Future.future(f -> logRequestReceived(context));
            LOGGER.info("consent log : {}", "DATA_REQUESTED");
            context.next();
        }
        context.next();

    }

    private Future<Void> logRequestReceived(RoutingContext context) {
        Promise<Void> promise = Promise.promise();
        JwtData jwtData = (JwtData) context.data().get("jwtData");
        LOGGER.info("HEADERS : {}", context.request().headers());
        JsonObject jsonObject = new JsonObject().put("logType", "DATA_REQUESTED");
        consentLoggingService.log(jsonObject, jwtData)
                .onSuccess(logHandler -> {
                    auditingConsentLog(logHandler);
                    promise.complete();
                })
                .onFailure(failure -> {
                    LOGGER.error("Failed to log :{}", failure.getMessage());
                    promise.fail("Failed to log :{}" + failure.getMessage());
                });
        return promise.future();
    }

    private void auditingConsentLog(JsonObject consentAuditLog) {
        Promise<Void> promise = Promise.promise();
        meteringService.insertMeteringValuesInRMQ(
                consentAuditLog,
                handler -> {
                    if (handler.succeeded()) {
                        LOGGER.info("Log published into RMQ.");
                        promise.complete();
                    } else {
                        LOGGER.error("failed to publish log into RMQ.");
                        promise.complete();
                    }
                });
        promise.future();
    }
}
