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
import java.util.UUID;
import java.util.function.Supplier;

import static iudx.rs.proxy.metering.util.Constants.CONSENT_LOG;

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
        String api = request.getString("api");
        ConsentLogType consentLogType;
        try {
            consentLogType = getLogTyp(LogType);
        } catch (IllegalArgumentException e) {
            return Future.failedFuture("No Consent defined for given type");
        }
        if (consentLogType != null) {
            Future<JsonObject> signedLogFuture = generateConsentAuditLog(consentLogType.toString(), jwtData);
            signedLogFuture.onSuccess(logHandler -> {
                        promise.complete(logHandler);
                    })
                    .onFailure(failureHandler -> {
                        promise.fail("failed to generate signedLog");
                    });
        } else {
            LOGGER.error("null value passed as ConsentLogType");
            promise.fail("null value passed as ConsentLogType .");
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

    Future<JsonObject> generateConsentAuditLog(String consentLogType, JwtData jwtData) {
        LOGGER.trace("generateAuditLog started");
        Promise promise = Promise.promise();
        String signedLog = payloadSigningManager.signDocWithPKCS12(jwtData.toJson());
        String item_id = jwtData.getIid().split(":")[1];
        ConsentAuditMessage auditMessage = new ConsentAuditMessage.Builder()
                .withPrimaryKey(primaryKeySuppler.get())
                .forAiu_id(jwtData.getSub())
                .forEvent(consentLogType)
                .forItemType(jwtData.getType())
                .forItem_id(item_id)
                .witAipId(jwtData.getProvider())
                .withDpId(jwtData.getPpbNumber())
                .withArtifactId(jwtData.getArtifact())
                .withLog(signedLog)
                .atIsoTime(isoTimeSupplier.get())
                .forOrigin(CONSENT_LOG)
                .build();

        promise.complete(auditMessage.toJson());

        return promise.future();
    }
}
