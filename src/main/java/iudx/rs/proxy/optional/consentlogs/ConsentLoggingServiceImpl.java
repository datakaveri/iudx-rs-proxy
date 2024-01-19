package iudx.rs.proxy.optional.consentlogs;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import iudx.rs.proxy.apiserver.handlers.ConsentLogRequestHandler;
import iudx.rs.proxy.authenticator.model.JwtData;
import iudx.rs.proxy.common.ConsentLogType;
import iudx.rs.proxy.metering.MeteringService;
import iudx.rs.proxy.optional.consentlogs.dss.PayloadSigningManager;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static iudx.rs.proxy.apiserver.util.ApiServerConstants.ITEM_TYPES;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.VALIDATION_ID_PATTERN;
import static iudx.rs.proxy.authenticator.Constants.CAT_SEARCH_PATH;
import static iudx.rs.proxy.metering.util.Constants.ORIGIN;

public class ConsentLoggingServiceImpl implements ConsentLoggingService {

    private static final Logger LOGGER = LogManager.getLogger(ConsentLogRequestHandler.class);
    static WebClient catWebClient;
    final String host;
    final int port;
    final String path;
    final String catBasePath;
    private final PayloadSigningManager payloadSigningManager;
    private final MeteringService meteringService;
    Vertx vertx = Vertx.vertx();
    Supplier<String> isoTimeSupplier =
            () -> ZonedDateTime.now(ZoneId.of(ZoneId.SHORT_IDS.get("IST"))).toString();
    Supplier<String> primaryKeySuppler = () -> UUID.randomUUID().toString();


    public ConsentLoggingServiceImpl(PayloadSigningManager signingManager, MeteringService meteringService, JsonObject config) {
        this.payloadSigningManager = signingManager;
        this.meteringService = meteringService;
        this.host = config.getString("catServerHost");
        this.port = config.getInteger("catServerPort");
        this.catBasePath = config.getString("dxCatalogueBasePath");
        this.path = catBasePath + CAT_SEARCH_PATH;
        WebClientOptions options = new WebClientOptions();
        options.setTrustAll(true).setVerifyHost(false).setSsl(true);
        catWebClient = WebClient.create(vertx, options);
    }

    @Override
    public Future<JsonObject> log(JsonObject request, JwtData jwtData) {
        LOGGER.trace("log started");
        Promise<JsonObject> promise = Promise.promise();
        String LogType = request.getString("logType");
        ConsentLogType consentLogType;
        try {
            consentLogType = getLogType(LogType);
        } catch (IllegalArgumentException e) {
            return Future.failedFuture("No Consent defined for given type");
        }

        if (!isConsentRequired(jwtData)) {
            promise.fail("token doesn't contains PII data.");
            return Future.failedFuture("token doesn't contains PII data.");
        }
        if (consentLogType != null) {
            getCatItem(jwtData)
                    .onSuccess(catItems -> {
                        jwtData.setProvider(catItems.getString("provider"));
                        jwtData.setType(catItems.getString("type"));
                        JsonObject consentAuditLog = generateConsentAuditLog(consentLogType.toString(), jwtData);
                        Future<Void> consentAuditFuture = auditingConsentLog(consentAuditLog);
                        consentAuditFuture.onComplete(auditHandler -> {
                            if (auditHandler.succeeded()) {
                                promise.complete(consentAuditLog);
                            } else {
                                promise.fail(auditHandler.cause().getMessage());
                            }
                        });
                    })
                    .onFailure(err -> {
                        LOGGER.error("cat failure : " + err.getMessage());
                        promise.fail(err.getMessage());
                    });

        } else {
            LOGGER.debug("null value passed as ConsentLogType");
            promise.fail("null value passed as ConsentLogType");
        }


        return promise.future();
    }


    private ConsentLogType getLogType(String Type) {
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
        if (!type.equalsIgnoreCase("RESOURCE")) {
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

    private Boolean isConsentRequired(JwtData jwtData) {
        String resourceId = jwtData.getIid().split(":")[1];
        Boolean isRequired = false;
        JsonObject cons = jwtData.getCons();
        if (VALIDATION_ID_PATTERN.matcher(resourceId).matches() &&
                cons.containsKey("artifact") && cons.containsKey("ppbNumber")) {
            isRequired = true;

        }
        return isRequired;
    }

    Future<JsonObject> getCatItem(JwtData jwtData) {
        String resourceId = jwtData.getIid().split(":")[1];
        LOGGER.trace("resourceid :{} ", resourceId);
        Promise promise = Promise.promise();

        catWebClient
                .get(port, host, path)
                .addQueryParam("property", "[id]")
                .addQueryParam("value", "[[" + resourceId + "]]")
                .addQueryParam("filter", "[id,provider,resourceGroup,type,accessPolicy]")
                .expect(ResponsePredicate.JSON)
                .send(httpResponseAsyncResult -> {
                    if (httpResponseAsyncResult.failed()) {
                        LOGGER.error(httpResponseAsyncResult.cause());
                        promise.fail("Resource not found");
                        return;
                    }
                    HttpResponse<Buffer> response = httpResponseAsyncResult.result();
                    if (response.statusCode() != HttpStatus.SC_OK) {
                        promise.fail("Resource not found");
                        return;
                    }
                    JsonObject responseBody = response.bodyAsJsonObject();
                    LOGGER.debug("responseBody:: " + responseBody);
                    if (!responseBody.getString("type").equals("urn:dx:cat:Success")) {
                        promise.fail("Resource not found");
                        return;
                    }
                    if (responseBody.getInteger("totalHits") <= 0) {
                        promise.fail("Empty response/Resource not found");
                        return;
                    }
                    try {
                        JsonArray results = responseBody.getJsonArray("results");
                        results.forEach(
                                json -> {
                                    JsonObject CatResult = (JsonObject) json;
                                    Set<String> type = new HashSet<String>(CatResult.getJsonArray("type").getList());
                                    Set<String> itemTypeSet =
                                            type.stream().map(e -> e.split(":")[1]).collect(Collectors.toSet());
                                    itemTypeSet.retainAll(ITEM_TYPES);
                                    CatResult.put("type", itemTypeSet.iterator().next());
                                    promise.complete(CatResult);
                                });
                    } catch (Exception ignored) {
                        LOGGER.error("Info: ID invalid : Empty response in results from Catalogue",
                                ignored);
                        promise.fail("Resource not found");
                    }
                });
        return promise.future();
    }

}
