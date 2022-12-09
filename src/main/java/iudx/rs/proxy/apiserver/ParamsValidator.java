package iudx.rs.proxy.apiserver;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import iudx.rs.proxy.apiserver.service.CatalogueService;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.rs.proxy.apiserver.util.ApiServerConstants.*;

/**
 * This class is used to validate NGSI-LD request and request parameters.
 */
public class ParamsValidator {
  private static final Logger LOGGER = LogManager.getLogger(ParamsValidator.class);
  private static final Set<String> validParams = new HashSet<String>();
  private static final Set<String> validHeaders = new HashSet<String>();

  static {
    validParams.add(NGSILDQUERY_TYPE);
    validParams.add(NGSILDQUERY_ID);
    validParams.add(NGSILDQUERY_IDPATTERN);
    validParams.add(NGSILDQUERY_ATTRIBUTE);
    validParams.add(NGSLILDQUERY_Q);
    validParams.add(NGSILDQUERY_GEOREL);
    validParams.add(NGSILDQUERY_GEOMETRY);
    validParams.add(NGSILDQUERY_COORDINATES);
    validParams.add(NGSILDQUERY_GEOPROPERTY);
    validParams.add(NGSILDQUERY_TIMEPROPERTY);
    validParams.add(NGSILDQUERY_TIMEREL);
    validParams.add(NGSILDQUERY_TIME);
    validParams.add(NGSILDQUERY_ENDTIME);
    validParams.add(NGSILDQUERY_ENTITIES);
    validParams.add(NGSILDQUERY_GEOQ);
    validParams.add(NGSILDQUERY_TEMPORALQ);

    // Need to check with the timeProperty in Post Query property for NGSI-LD release v1.3.1
    validParams.add(NGSILDQUERY_TIME_PROPERTY);
    validParams.add(NGSILDQUERY_FROM);
    validParams.add(NGSILDQUERY_SIZE);

    // for IUDX count query
    validParams.add(IUDXQUERY_OPTIONS);
    // IUDX search-type
    validParams.add(IUDX_SEARCH_TYPE);
  }

  static {
    validHeaders.add(HEADER_OPTIONS);
    validHeaders.add(HEADER_TOKEN);
    validHeaders.add("User-Agent");
    validHeaders.add("Content-Type");
    validHeaders.add(HEADER_PUBLIC_KEY);

  }

  private final CatalogueService catalogueService;

  public ParamsValidator(CatalogueService catalogueService) {
    this.catalogueService = catalogueService;
  }

  /**
   * Validate a http request.
   *
   * @param parameterMap parameters map of request query
   * @param response HttpServerResponse object
   */
  private boolean validateParams(MultiMap parameterMap) {

    final List<Entry<String, String>> entries = parameterMap.entries();
    for (final Entry<String, String> entry : entries) {
      if (!validParams.contains(entry.getKey())) {
        return false;
      }
    }
    return true;
  }

  /**
   * validate request parameters.
   *
   * @param paramsMap map of request parameters
   * @return Future future JsonObject
   */
  public Future<Boolean> validate(MultiMap paramsMap) {

    Promise<Boolean> promise = Promise.promise();
    if (validateParams(paramsMap)) {
      isValidQueryWithFilters(paramsMap).onComplete(handler -> {
        if (handler.succeeded()) {
          {
            promise.complete(true);
          }
        } else {
          promise.fail(handler.cause().getMessage());
        }
      });
    } else {
      promise.fail(MSG_BAD_QUERY);
    }
    return promise.future();
  }

  private Future<Boolean> isValidQueryWithFilters(MultiMap paramsMap) {
    Promise<Boolean> promise = Promise.promise();
    Future<List<String>> filtersFuture = catalogueService.getApplicableFilters(paramsMap.get("id"));
    filtersFuture.onComplete(handler -> {
      if (handler.succeeded()) {
        List<String> filters = filtersFuture.result();
        if (isTemporalQuery(paramsMap) && !filters.contains("TEMPORAL")) {
          promise.fail("Temporal parameters are not supported by RS group/Item.");
          return;
        }
        if (isAttributeQuery(paramsMap) && !filters.contains("ATTR")) {
          promise.fail("Attribute parameters are not supported by RS group/Item.");
          return;
        }
        promise.complete(true);
      } else {
        promise.fail("fail to get filters for validation");
      }
    });
    return promise.future();
  }

  private Boolean isTemporalQuery(MultiMap params) {
    return params.contains(NGSILDQUERY_TIMEREL)
        || params.contains(NGSILDQUERY_TIME)
        || params.contains(NGSILDQUERY_ENDTIME)
        || params.contains(NGSILDQUERY_TIME_PROPERTY);
  }

  private Boolean isAttributeQuery(MultiMap params) {
    return params.contains(NGSILDQUERY_ATTRIBUTE);
  }
}
