package iudx.rs.proxy.apiserver;

import static iudx.rs.proxy.apiserver.response.ResponseUtil.generateResponse;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.ALLOWED_HEADERS;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.ALLOWED_METHODS;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.API;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.API_ENDPOINT;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.APPLICATION_JSON;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.CONTENT_TYPE;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.HEADER_HOST;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.ID;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.IUDXQUERY_OPTIONS;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.JSON_COUNT;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.JSON_INSTANCEID;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.JSON_TITLE;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.JSON_TYPE;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.NGSILD_ENTITIES_URL;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.NGSILD_TEMPORAL_URL;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.USER_ID;
import static iudx.rs.proxy.common.Constants.DB_SERVICE_ADDRESS;
import static iudx.rs.proxy.common.Constants.METERING_SERVICE_ADDRESS;
import static iudx.rs.proxy.common.HttpStatusCode.BAD_REQUEST;
import static iudx.rs.proxy.common.ResponseUrn.BACKING_SERVICE_FORMAT_URN;
import static iudx.rs.proxy.common.ResponseUrn.INVALID_PARAM_URN;
import static iudx.rs.proxy.common.ResponseUrn.INVALID_TEMPORAL_PARAM_URN;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.serviceproxy.ServiceException;
import iudx.rs.proxy.apiserver.exceptions.DxRuntimeException;
import iudx.rs.proxy.apiserver.handlers.AuthHandler;
import iudx.rs.proxy.apiserver.handlers.FailureHandler;
import iudx.rs.proxy.apiserver.handlers.ValidationHandler;
import iudx.rs.proxy.apiserver.query.NGSILDQueryParams;
import iudx.rs.proxy.apiserver.query.QueryMapper;
import iudx.rs.proxy.apiserver.response.ResponseType;
import iudx.rs.proxy.apiserver.service.CatalogueService;
import iudx.rs.proxy.apiserver.util.RequestType;
import iudx.rs.proxy.common.HttpStatusCode;
import iudx.rs.proxy.common.ResponseUrn;
import iudx.rs.proxy.database.DatabaseService;
import iudx.rs.proxy.metering.MeteringService;

public class ApiServerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(ApiServerVerticle.class);

  private int port;
  private HttpServer server;
  private Router router;
  private String keystore, keystorePassword;
  private boolean isSSL, isProduction;
  private ParamsValidator validator;
  private CatalogueService catalogueService;
  private DatabaseService databaseService;
  private MeteringService meteringService;

  @Override
  public void start() throws Exception {
    catalogueService = new CatalogueService(vertx, config());
    databaseService = DatabaseService.createProxy(vertx, DB_SERVICE_ADDRESS);
    meteringService = MeteringService.createProxy(vertx, METERING_SERVICE_ADDRESS);
    validator = new ParamsValidator(catalogueService);
    router = Router.router(vertx);
    router
        .route()
        .handler(
            CorsHandler.create("*").allowedHeaders(ALLOWED_HEADERS).allowedMethods(ALLOWED_METHODS))
        .handler(
            responseHeaderHandler -> {
              responseHeaderHandler
                  .response()
                  .putHeader("Cache-Control", "no-cache, no-store,  must-revalidate,max-age=0")
                  .putHeader("Pragma", "no-cache")
                  .putHeader("Expires", "0")
                  .putHeader("X-Content-Type-Options", "nosniff");
              responseHeaderHandler.next();
            });

    router.route().handler(BodyHandler.create());

    isSSL = config().getBoolean("ssl");
    isProduction = config().getBoolean("production");
    port = config().getInteger("port");

    HttpServerOptions serverOptions = new HttpServerOptions();

    if (isSSL) {
      LOGGER.info("Info: Starting HTTPs server");

      keystore = config().getString("keystore");
      keystorePassword = config().getString("keystorePassword");

      serverOptions
          .setSsl(true)
          .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword));

    } else {
      LOGGER.info("Info: Starting HTTP server");

      serverOptions.setSsl(false);
      if (isProduction) {
        port = 80;
      } else {
        port = 8080;
      }
    }

    serverOptions.setCompressionSupported(true).setCompressionLevel(5);
    server = vertx.createHttpServer(serverOptions);
    server.requestHandler(router).listen(port);

    FailureHandler validationsFailureHandler = new FailureHandler();

    ValidationHandler entityValidationHandler = new ValidationHandler(vertx, RequestType.ENTITY);
    router
        .get(NGSILD_ENTITIES_URL)
        .handler(entityValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::handleEntitiesQuery)
        .failureHandler(validationsFailureHandler);

    ValidationHandler temporalValidationHandler =
        new ValidationHandler(vertx, RequestType.TEMPORAL);

    router
        .get(NGSILD_TEMPORAL_URL)
        .handler(temporalValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::handleTemporalQuery)
        .failureHandler(validationsFailureHandler);
  }

  private void handleEntitiesQuery(RoutingContext routingContext) {
    /* Handles HTTP request from client */
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    HttpServerRequest request = routingContext.request();
    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();
    // get query parameters
    MultiMap params = getQueryParams(routingContext, response).get();
    MultiMap headerParams = request.headers();
    // validate request parameters
    Future<Boolean> validationResult = validator.validate(params);
    validationResult.onComplete(
        validationHandler -> {
          if (validationHandler.succeeded()) {
            // parse query params
            NGSILDQueryParams ngsildQuery = new NGSILDQueryParams(params);
            if (isTemporalParamsPresent(ngsildQuery)) {
              DxRuntimeException ex =
                  new DxRuntimeException(
                      BAD_REQUEST.getValue(),
                      INVALID_TEMPORAL_PARAM_URN,
                      "Temporal parameters are not allowed in entities query.");
              routingContext.fail(ex);
            }
            // create json
            QueryMapper queryMapper = new QueryMapper();
            JsonObject json = queryMapper.toJson(ngsildQuery, false);
            Future<List<String>> filtersFuture =
                catalogueService.getApplicableFilters(json.getJsonArray("id").getString(0));
            /* HTTP request instance/host details */
            String instanceID = request.getHeader(HEADER_HOST);
            json.put(JSON_INSTANCEID, instanceID);
            /* HTTP request body as Json */
            JsonObject requestBody = new JsonObject();
            requestBody.put("ids", json.getJsonArray("id"));
            filtersFuture.onComplete(
                filtersHandler -> {
                  if (filtersHandler.succeeded()) {
                    json.put("applicableFilters", filtersHandler.result());
                    if (json.containsKey(IUDXQUERY_OPTIONS)
                        && JSON_COUNT.equalsIgnoreCase(json.getString(IUDXQUERY_OPTIONS))) {
                      executeCountQuery(routingContext, json, response);
                    } else {
                      executeSearchQuery(routingContext, json, response);
                    }
                  } else if (validationHandler.failed()) {
                    LOGGER.error("Fail: Validation failed");
                    handleResponse(
                        response,
                        BAD_REQUEST,
                        INVALID_PARAM_URN,
                        validationHandler.cause().getMessage());
                  }
                });
          } else if (validationHandler.failed()) {
            LOGGER.error("Fail: Validation failed");
            handleResponse(
                response, BAD_REQUEST, INVALID_PARAM_URN, validationHandler.cause().getMessage());
          }
        });
  }

  private boolean isTemporalParamsPresent(NGSILDQueryParams ngsildQueryParams) {

    return ngsildQueryParams.getTemporalRelation().getTimeRel() != null
        || ngsildQueryParams.getTemporalRelation().getTime() != null
        || ngsildQueryParams.getTemporalRelation().getEndTime() != null;
  }

  public void handleTemporalQuery(RoutingContext routingContext) {
    LOGGER.trace("Info: handleTemporalQuery method started.");
    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);
    // get query parameters
    MultiMap params = getQueryParams(routingContext, response).get();

    // validate request params
    Future<Boolean> validationResult = validator.validate(params);

    validationResult.onComplete(
        validationHandler -> {
          if (validationHandler.succeeded()) {
            // parse query params
            NGSILDQueryParams ngsildquery = new NGSILDQueryParams(params);
            // create json
            QueryMapper queryMapper = new QueryMapper();
            JsonObject json = queryMapper.toJson(ngsildquery, true);
            Future<List<String>> filtersFuture =
                catalogueService.getApplicableFilters(json.getJsonArray("id").getString(0));
            json.put(JSON_INSTANCEID, instanceID);
            LOGGER.debug("Info: IUDX temporal json query;" + json);
            /* HTTP request body as Json */
            JsonObject requestBody = new JsonObject();
            requestBody.put("ids", json.getJsonArray("id"));
            filtersFuture.onComplete(
                filtersHandler -> {
                  if (filtersHandler.succeeded()) {
                    json.put("applicableFilters", filtersHandler.result());
                    if (json.containsKey(IUDXQUERY_OPTIONS)
                        && JSON_COUNT.equalsIgnoreCase(json.getString(IUDXQUERY_OPTIONS))) {
                      executeCountQuery(routingContext, json, response);
                    } else {
                      executeSearchQuery(routingContext, json, response);
                    }
                  } else {
                    LOGGER.error("catalogue item/group doesn't have filters.");
                  }
                });
          } else if (validationHandler.failed()) {
            LOGGER.error("Fail: Bad request;");
            handleResponse(
                response, BAD_REQUEST, INVALID_PARAM_URN, validationHandler.cause().getMessage());
          }
        });
  }

  private void executeSearchQuery(
      RoutingContext context, JsonObject json, HttpServerResponse response) {
    databaseService.searchQuery(
        json,
        handler -> {
          if (handler.succeeded()) {
            LOGGER.info("Success: Search Success");
            Future.future(fu -> updateAuditTable(context));
            if (handler.result().getLong("totalHits") == 0) {
              handleSuccessResponse(
                  response, ResponseType.NoContent.getCode(), handler.result().toString());
            } else {
              handleSuccessResponse(
                  response, ResponseType.Ok.getCode(), handler.result().toString());
            }
          } else if (handler.failed()) {
            LOGGER.error("Fail: Search Fail");
            LOGGER.debug(handler instanceof ServiceException);
            processBackendResponse(response, handler.cause().getMessage());
          }
        });
  }

  private void executeCountQuery(
      RoutingContext context, JsonObject json, HttpServerResponse response) {
    databaseService.countQuery(
        json,
        handler -> {
          if (handler.succeeded()) {
            LOGGER.info("Success: Count Success");
            Future.future(fu -> updateAuditTable(context));
            if (handler.result().getJsonArray("results").getJsonObject(0).getLong("totalHits") == 0) {
              handleSuccessResponse(
                  response, ResponseType.NoContent.getCode(), handler.result().toString());
            } else {
              handleSuccessResponse(
                  response, ResponseType.Ok.getCode(), handler.result().toString());
            }
          } else if (handler.failed()) {
            LOGGER.error("Fail: Count Fail");
            processBackendResponse(response, handler.cause().getMessage());
          }
        });
  }

  private Optional<MultiMap> getQueryParams(
      RoutingContext routingContext, HttpServerResponse response) {
    MultiMap queryParams = null;
    try {
      queryParams = MultiMap.caseInsensitiveMultiMap();
      // Internally + sign is dropped and treated as space, replacing + with %2B do the trick
      String uri = routingContext.request().uri().replaceAll("\\+", "%2B");
      Map<String, List<String>> decodedParams =
          new QueryStringDecoder(uri, HttpConstants.DEFAULT_CHARSET, true, 1024, true).parameters();
      for (Map.Entry<String, List<String>> entry : decodedParams.entrySet()) {
        LOGGER.debug("Info: param :" + entry.getKey() + " value : " + entry.getValue());
        queryParams.add(entry.getKey(), entry.getValue());
      }
    } catch (IllegalArgumentException ex) {
      response
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(HttpStatusCode.BAD_REQUEST.getValue())
          .end(generateResponse(HttpStatusCode.BAD_REQUEST, INVALID_PARAM_URN).toString());
    }
    return Optional.of(queryParams);
  }

  private void handleSuccessResponse(HttpServerResponse response, int statusCode, String result) {
    response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(statusCode).end(result);
  }

  private void handleResponse(
      HttpServerResponse response, HttpStatusCode statusCode, ResponseUrn urn, String message) {
    response
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(statusCode.getValue())
        .end(generateResponse(statusCode, urn, message).toString());
  }

  private void handleResponse(HttpServerResponse response, HttpStatusCode code, ResponseUrn urn) {
    handleResponse(response, code, urn, code.getDescription());
  }

  private void processBackendResponse(HttpServerResponse response, String failureMessage) {
    LOGGER.debug("Info : " + failureMessage);
    try {
      JsonObject json = new JsonObject(failureMessage);
      String type = json.getString(JSON_TYPE);
      int status=json.getInteger("status");
      HttpStatusCode httpStatus = HttpStatusCode.getByValue(status);
      String urnTitle = type;
      ResponseUrn urn;
      if (urnTitle != null) {
        urn = ResponseUrn.fromCode(urnTitle);
      } else {
        urn = ResponseUrn.fromCode(type + "");
      }
      // return urn in body
      response
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(status)
          .end(generateResponse(httpStatus, urn).toString());
    } catch (DecodeException ex) {
      LOGGER.error("ERROR : Expecting Json from backend service [ jsonFormattingException ]");
      handleResponse(response, HttpStatusCode.BAD_REQUEST, BACKING_SERVICE_FORMAT_URN);
    }
  }

  private void updateAuditTable(RoutingContext context) {
    Promise<Void> promise = Promise.promise();
    JsonObject authInfo = (JsonObject) context.data().get("authInfo");

    JsonObject request = new JsonObject();
    request.put(USER_ID, authInfo.getValue(USER_ID));
    request.put(ID, authInfo.getValue(ID));
    request.put(API, authInfo.getValue(API_ENDPOINT));
    meteringService.executeWriteQuery(
        request,
        handler -> {
          if (handler.succeeded()) {
            LOGGER.info("audit table updated");
            promise.complete();
          } else {
            LOGGER.error("failed to update audit table");
            promise.complete();
          }
        });

    promise.future();
  }
}
