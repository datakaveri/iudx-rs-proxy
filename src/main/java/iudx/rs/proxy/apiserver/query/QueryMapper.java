package iudx.rs.proxy.apiserver.query;

import static iudx.rs.proxy.common.HttpStatusCode.BAD_REQUEST;
import static iudx.rs.proxy.common.ResponseUrn.INVALID_ATTR_PARAM_URN;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.rs.proxy.apiserver.exceptions.DxRuntimeException;
import iudx.rs.proxy.apiserver.util.ApiServerConstants;
import iudx.rs.proxy.common.ResponseUrn;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * QueryMapper class to convert NGSILD query into json object for the purpose of information
 * exchange among different verticals. TODO Need to add documentation.
 */
public class QueryMapper {

  private static final Logger LOGGER = LogManager.getLogger(QueryMapper.class);
  private boolean isTemporal = false;
  private boolean isAttributeSearch = false;


  /**
   * This method is used to create a json object from NGSILDQueryParams.
   *
   * @param params A map of query parameters passed.
   * @param isTemporal flag indicating whether temporal or not.
   * @return JsonObject result.
   */
  public JsonObject toJson(NGSILDQueryParams params, boolean isTemporal) {
    LOGGER.trace("Info QueryMapper#toJson() started");
    LOGGER.debug("Info : params" + params);
    this.isTemporal = isTemporal;
    JsonObject json = new JsonObject();

    if (params.getId() != null) {
      JsonArray jsonArray = new JsonArray();
      params.getId().forEach(s -> jsonArray.add(s.toString()));
      json.put(ApiServerConstants.JSON_ID, jsonArray);
      LOGGER.debug("Info : json " + json);
    }
    if (params.getAttrs() != null) {
      JsonArray jsonArray = new JsonArray();
      params.getAttrs().forEach(attribute -> jsonArray.add(attribute));
      json.put(ApiServerConstants.JSON_ATTRIBUTE_FILTER, jsonArray);
      LOGGER.debug("Info : json " + json);
    }
    if (isTemporal
        && params.getTemporalRelation().getTimeRel() != null
        && params.getTemporalRelation().getTime() != null) {
      isTemporal = true;
      if (params
          .getTemporalRelation()
          .getTimeRel()
          .equalsIgnoreCase(ApiServerConstants.JSON_DURING)) {
        LOGGER.debug("Info : inside during ");

        json.put(ApiServerConstants.JSON_TIME, params.getTemporalRelation().getTime());
        json.put(ApiServerConstants.JSON_ENDTIME, params.getTemporalRelation().getEndTime());
        json.put(ApiServerConstants.JSON_TIMEREL, params.getTemporalRelation().getTimeRel());

        isValidTimeInterval(
            ApiServerConstants.JSON_DURING,
            json.getString(ApiServerConstants.JSON_TIME),
            json.getString(ApiServerConstants.JSON_ENDTIME));
      } else {
        json.put(ApiServerConstants.JSON_TIME, params.getTemporalRelation().getTime());
        json.put(ApiServerConstants.JSON_TIMEREL, params.getTemporalRelation().getTimeRel());
      }
      LOGGER.debug("Info : json " + json);
    }
    if (params.getQ() != null) {
      isAttributeSearch = true;
      JsonArray query = new JsonArray();
      String[] qterms = params.getQ().split(";");
      for (String term : qterms) {
        query.add(getQueryTerms(term));
      }
      json.put(ApiServerConstants.JSON_ATTR_QUERY, query);
      LOGGER.debug("Info : json " + json);
    }
    if (params.getOptions() != null) {
      json.put(ApiServerConstants.IUDXQUERY_OPTIONS, params.getOptions());
      LOGGER.debug("Info : json " + json);
    }

    json.put(ApiServerConstants.JSON_SEARCH_TYPE, getSearchType());
    LOGGER.debug("Info : json " + json);
    return json;
  }

  /*
   * check for a valid days interval for temporal queries
   */
  // TODO : decide how to enforce for before and after queries.
  private void isValidTimeInterval(
      String timeRel, String time, String endTime) {
    long totalDaysAllowed = 0;
    if (timeRel.equalsIgnoreCase(ApiServerConstants.JSON_DURING)) {
      if (isNullOrEmpty(time) || isNullOrEmpty(endTime)) {
        throw new DxRuntimeException(
            BAD_REQUEST.getValue(),
            ResponseUrn.INVALID_TEMPORAL_PARAM_URN,
            "time and endTime both are mandatory for during Query.");
      }

      try {
        ZonedDateTime start = ZonedDateTime.parse(time);
        ZonedDateTime end = ZonedDateTime.parse(endTime);
        Duration duration = Duration.between(start, end);
        totalDaysAllowed = duration.toDays();
      } catch (Exception ex) {
        throw new DxRuntimeException(
            BAD_REQUEST.getValue(),
            ResponseUrn.INVALID_TEMPORAL_PARAM_URN,
            "time and endTime both are mandatory for during Query.");
      }
    } else if (timeRel.equalsIgnoreCase("after")) {
      // how to enforce days duration for after and before,i.e here or DB
    } else if (timeRel.equalsIgnoreCase("before")) {

    }
    if (totalDaysAllowed > ApiServerConstants.VALIDATION_MAX_DAYS_INTERVAL_ALLOWED) {
      throw new DxRuntimeException(BAD_REQUEST.getValue(), ResponseUrn.INVALID_TEMPORAL_PARAM_URN,
          "time interval greater than 10 days is not allowed");
    }
  }

  public boolean isNullOrEmpty(String value) {
    return value == null || value.isEmpty();
  }

  private String getSearchType() {
    StringBuilder searchType = new StringBuilder();
    if (isTemporal) {
      searchType.append(ApiServerConstants.JSON_TEMPORAL_SEARCH);
    }
    if (isAttributeSearch) {
      searchType.append(ApiServerConstants.JSON_ATTRIBUTE_SEARCH);
    }
    return searchType.substring(0, searchType.length() - 1);
  }

  JsonObject getQueryTerms(String queryTerms) {
    JsonObject json = new JsonObject();
    int length = queryTerms.length();
    List<Character> allowedSpecialCharacter = Arrays.asList('>', '=', '<', '!');
    List<String> allowedOperators = Arrays.asList(">", "=", "<", ">=", "<=", "==", "!=");
    int startIndex = 0;
    boolean specialCharFound = false;
    for (int i = 0; i < length; i++) {
      Character c = queryTerms.charAt(i);
      if (!(Character.isLetter(c) || Character.isDigit(c)) && !specialCharFound) {
        if (allowedSpecialCharacter.contains(c)) {
          json.put(ApiServerConstants.JSON_ATTRIBUTE, queryTerms.substring(startIndex, i));
          startIndex = i;
          specialCharFound = true;
        } else {
          LOGGER.debug("Ignore " + c);
          throw new DxRuntimeException(
              BAD_REQUEST.getValue(), INVALID_ATTR_PARAM_URN, "Operator not allowed.");
        }
      } else {
        if (specialCharFound && (Character.isLetter(c) || Character.isDigit(c))) {
          json.put(ApiServerConstants.JSON_OPERATOR, queryTerms.substring(startIndex, i));
          json.put(ApiServerConstants.JSON_VALUE, queryTerms.substring(i));
          break;
        }
      }
    }
    if (!allowedOperators.contains(json.getString(ApiServerConstants.JSON_OPERATOR))) {
      throw new DxRuntimeException(
          BAD_REQUEST.getValue(), INVALID_ATTR_PARAM_URN, "Operator not allowed.");
    }
    return json;
  }
}
