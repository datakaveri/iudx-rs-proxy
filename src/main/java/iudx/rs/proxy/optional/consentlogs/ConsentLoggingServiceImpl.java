package iudx.rs.proxy.optional.consentlogs;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.rs.proxy.apiserver.handlers.ConsentLogRequestHandler;
import iudx.rs.proxy.authenticator.model.JwtData;
import iudx.rs.proxy.common.ConsentLogType;
import iudx.rs.proxy.metering.MeteringService;
import iudx.rs.proxy.optional.consentlogs.dss.PayloadSigningManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

import static iudx.rs.proxy.metering.util.Constants.ORIGIN;

public class ConsentLoggingServiceImpl implements ConsentLoggingService {

    private static final Logger LOGGER = LogManager.getLogger(ConsentLogRequestHandler.class);
    private final PayloadSigningManager payloadSigningManager;
    private final MeteringService meteringService;
    Supplier<String> isoTimeSupplier =
            () -> ZonedDateTime.now(ZoneId.of(ZoneId.SHORT_IDS.get("IST"))).toString();
    Supplier<String> primaryKeySuppler = () -> UUID.randomUUID().toString();


    public ConsentLoggingServiceImpl(PayloadSigningManager signingManager, MeteringService meteringService) {
        this.payloadSigningManager = signingManager;
        this.meteringService = meteringService;
    }

    @Override
    public Future<JsonObject> log(JsonObject request, JwtData jwtData) {
        LOGGER.trace("log started");
        Promise<JsonObject> promise = Promise.promise();
        String LogType = request.getString("logType");
        ConsentLogType consentLogType;
        try {
            consentLogType = getLogTyp(LogType);
        } catch (IllegalArgumentException e) {
            return Future.failedFuture("No Consent defined for given type");
        }
        if (consentLogType != null) {
            JsonObject consentAuditLog = generateConsentAuditLog(consentLogType.toString(), jwtData);
            Future<Void> consentAuditFuture = auditingConsentLog(consentAuditLog);
            consentAuditFuture.onComplete(auditHandler -> {
                if (auditHandler.succeeded()) {
                    promise.complete(consentAuditLog);
                } else {
                    promise.fail(auditHandler.cause().getMessage());
                }
            });
        } else {
            LOGGER.error("null value passed as ConsentLogType");
            promise.fail("null value passed as ConsentLogType.");
        }
        return promise.future();
    }


    private ConsentLogType getLogTyp(String Type) {
        ConsentLogType logType = null;
        try {
            logType = ConsentLogType.valueOf(Type);

        } catch (IllegalArgumentException ex) {
            LOGGER.error("No consent type defined for given argument.");
        }
        return logType;
    }

    private JsonObject generateConsentAuditLog(String consentLogType, JwtData jwtData) {
        LOGGER.trace("generateAuditLog started");
        JsonObject cons = jwtData.getCons();
        String item_id = jwtData.getIid().split(":")[1];
        String type = jwtData.getType().toUpperCase();
        if(!type.equalsIgnoreCase("RESOURCE")){
            type = "RESOURCE_GROUP";
        }
        SignLogBuider signLog = new SignLogBuider.Builder()
                .withPrimaryKey(primaryKeySuppler.get())
                .forAiu_id(jwtData.getSub())
                .forEvent(consentLogType)
                .forItemType(type)
                .forItem_id(item_id)
                .witAipId(jwtData.getProvider())
                .withDpId(cons.getString("ppbNumber"))
                .withArtifactId(cons.getString("artifact"))
                .atIsoTime(isoTimeSupplier.get())
                .build();
LOGGER.debug(signLog.toJson());
        String signedLog = payloadSigningManager.signDocWithPKCS12(signLog.toJson());
        JsonObject consentAuditLog = signLog.toJson();
        consentAuditLog.put("log", signedLog);
        consentAuditLog.put(ORIGIN, "consent_log");
        return consentAuditLog;
    }

    private Future<Void> auditingConsentLog(JsonObject consentAuditLog) {
        LOGGER.trace("auditingConsentLog started");
        Promise<Void> promise = Promise.promise();
        meteringService.insertMeteringValuesInRMQ(
                consentAuditLog,
                handler -> {
                    if (handler.succeeded()) {
                        LOGGER.info("Log published into RMQ.");
                        promise.complete();
                    } else {
                        LOGGER.error("failed to publish log into RMQ.");
                        promise.fail("failed to publish log into RMQ.");
                    }
                });
        return promise.future();
    }
}
