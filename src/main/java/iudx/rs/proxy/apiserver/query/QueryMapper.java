package iudx.rs.proxy.apiserver.query;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.rs.proxy.apiserver.exceptions.DxRuntimeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static iudx.rs.proxy.apiserver.util.ApiServerConstants.*;
import static iudx.rs.proxy.common.HttpStatusCode.BAD_REQUEST;
import static iudx.rs.proxy.common.ResponseUrn.*;

/**
 * QueryMapper class to convert NGSILD query into json object for the purpose of information
 * exchange among different verticals. TODO Need to add documentation.
 */
public class QueryMapper {

  private static final Logger LOGGER = LogManager.getLogger(QueryMapper.class);
  private boolean isTemporal = false;
  private boolean isAttributeSearch = false;
  private boolean isGeoSearch = false;


  /**
   * This method is used to create a json object from NGSILDQueryParams.
   *
   * @param params A map of query parameters passed.
   * @param isTemporal flag indicating whether temporal or not.
   * @return JsonObject result.
   */
  public JsonObject toJson(NGSILDQueryParams params, boolean isTemporal) {
    LOGGER.debug("Info : params" + params);
    this.isTemporal = isTemporal;
    JsonObject json = new JsonObject();
    JsonObject geoJson = new JsonObject();
    JsonObject temporal = new JsonObject();

    if (params.getId() != null) {
      JsonArray jsonArray = new JsonArray();
      params.getId().forEach(s -> jsonArray.add(s.toString()));
      json.put(JSON_ID, jsonArray);
      LOGGER.debug("Info : json " + json);
    }
    if (params.getAttrs() != null) {
      JsonArray jsonArray = new JsonArray();
      params.getAttrs().forEach(attribute -> jsonArray.add(attribute));
      json.put(JSON_ATTRIBUTE_FILTER, jsonArray);
      LOGGER.debug("Info : json " + json);
    }
    if (isGeoQuery(params)) {
      LOGGER.debug("getGeoRel:"+params.getGeoRel().getRelation());
      LOGGER.debug("getCoordinates:"+params.getCoordinates());
      LOGGER.debug("getGeometry:"+params.getGeometry());
      LOGGER.debug("getGeoProperty:"+params.getGeoProperty());

      if (params.getGeoRel().getRelation() != null && params.getCoordinates() != null
              && params.getGeometry() != null && params.getGeoProperty() != null) {
        isGeoSearch = true;
        if (params.getGeometry().equalsIgnoreCase(GEOM_POINT)
                && params.getGeoRel().getRelation().equals(JSON_NEAR)
                && params.getGeoRel().getMaxDistance() != null) {
          String[] coords = params.getCoordinates().replaceAll("\\[|\\]", "").split(",");
          geoJson.put(JSON_LAT, Double.parseDouble(coords[0]));
          geoJson.put(JSON_LON, Double.parseDouble(coords[1]));
          geoJson.put(JSON_RADIUS, params.getGeoRel().getMaxDistance());
        } else {
          geoJson.put(JSON_GEOMETRY, params.getGeometry());
          geoJson.put(JSON_COORDINATES, params.getCoordinates());
          geoJson.put(JSON_GEOREL,
                  getOrDefault(params.getGeoRel().getRelation(), JSON_WITHIN));
          if (params.getGeoRel().getMaxDistance() != null) {
            geoJson.put(JSON_MAXDISTANCE, params.getGeoRel().getMaxDistance());
          } else if (params.getGeoRel().getMinDistance() != null) {
            geoJson.put(JSON_MINDISTANCE, params.getGeoRel().getMinDistance());
          }
        }
        LOGGER.debug("Info : json " + geoJson);
      } else {
        throw new DxRuntimeException(BAD_REQUEST.getValue(), INVALID_GEO_PARAM_URN,
                "incomplete geo-query geoproperty, geometry, georel, coordinates all are mandatory.");
      }
      json.put(GEO_QUERY,geoJson);
    }
    if (isTemporal
        && params.getTemporalRelation().getTimeRel() != null
        && params.getTemporalRelation().getTime() != null) {
      isTemporal = true;
      if (params
              .getTemporalRelation()
              .getTimeRel()
              .equalsIgnoreCase(JSON_DURING)
              || params.getTemporalRelation().getTimeRel().equalsIgnoreCase(JSON_BETWEEN)) {
        LOGGER.debug("Info : inside during ");

        temporal.put(JSON_TIME, params.getTemporalRelation().getTime());
        temporal.put(JSON_ENDTIME, params.getTemporalRelation().getEndTime());
        temporal.put(JSON_TIMEREL, params.getTemporalRelation().getTimeRel());

        isValidTimeInterval(
            JSON_DURING,
                temporal.getString(JSON_TIME),
                temporal.getString(JSON_ENDTIME));
      } else {
        LOGGER.debug("Info : outside during ");
        temporal.put(JSON_TIME, params.getTemporalRelation().getTime());
        temporal.put(JSON_TIMEREL, params.getTemporalRelation().getTimeRel());
      }
      LOGGER.debug("Info : json " + temporal);
      json.put(TEMPORAL_QUERY,temporal);
    }

    if (params.getQ() != null) {
      isAttributeSearch = true;
      //TODO: disabled this check for ESDS-IPeG deployment
//      JsonArray query = new JsonArray();
//      String[] qterms = params.getQ().split(";");
//      for (String term : qterms) {
//        query.add(getQueryTerms(term));
//      }
      json.put(JSON_ATTR_QUERY, params.getQ());
    }
    if (params.getGeoProperty() != null) {
      geoJson.put(JSON_GEOPROPERTY, params.getGeoProperty());
      LOGGER.debug("Info : json " + json);
    }
    if (params.getOptions() != null) {
      json.put(IUDXQUERY_OPTIONS, params.getOptions());
      LOGGER.debug("Info : json " + json);
    }
    if (params.getPageFrom() != null) {
      json.put(NGSILDQUERY_FROM, params.getPageFrom());
    }
    if (params.getPageSize() != null) {
      json.put(NGSILDQUERY_SIZE, params.getPageSize());
    }
    json.put(JSON_SEARCH_TYPE, getSearchType());
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
    if (timeRel.equalsIgnoreCase(JSON_DURING)) {
      if (isNullOrEmpty(time) || isNullOrEmpty(endTime)) {
        throw new DxRuntimeException(
            BAD_REQUEST.getValue(),
            INVALID_TEMPORAL_PARAM_URN,
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
            INVALID_TEMPORAL_PARAM_URN,
            "time and endTime both are mandatory for during Query.");
      }
    } else if (timeRel.equalsIgnoreCase("after")) {
      // how to enforce days duration for after and before,i.e here or DB
    } else if (timeRel.equalsIgnoreCase("before")) {

    }
    /*if (totalDaysAllowed >VALIDATION_MAX_DAYS_INTERVAL_ALLOWED) {
      throw new DxRuntimeException(BAD_REQUEST.getValue(),INVALID_TEMPORAL_PARAM_URN,
          "time interval greater than 10 days is not allowed");
    }*/
  }

  public boolean isNullOrEmpty(String value) {
    return value == null || value.isEmpty();
  }

  private String getSearchType() {
    StringBuilder searchType = new StringBuilder();
    if (isTemporal) {
      searchType.append(JSON_TEMPORAL_SEARCH);
    }
    else if(!isTemporal){
      searchType.append(JSON_LATEST_SEARCH);
    }
    if (isAttributeSearch) {
      searchType.append(JSON_ATTRIBUTE_SEARCH);
    }
    if (isGeoSearch) {
      searchType.append(JSON_GEO_SEARCH);
    }
    return searchType.substring(0, searchType.length() - 1).toString();
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
          json.put(JSON_ATTRIBUTE, queryTerms.substring(startIndex, i));
          startIndex = i;
          specialCharFound = true;
        } else {
          LOGGER.debug("Ignore " + c);
          throw new DxRuntimeException(
              BAD_REQUEST.getValue(), INVALID_ATTR_PARAM_URN, "Operator not allowed.");
        }
      } else {
        if (specialCharFound && (Character.isLetter(c) || Character.isDigit(c))) {
          json.put(JSON_OPERATOR, queryTerms.substring(startIndex, i));
          json.put(JSON_VALUE, queryTerms.substring(i));
          break;
        }
      }
    }
    if (!allowedOperators.contains(json.getString(JSON_OPERATOR))) {
      throw new DxRuntimeException(
          BAD_REQUEST.getValue(), INVALID_ATTR_PARAM_URN, "Operator not allowed.");
    }
    return json;
  }
  private boolean isGeoQuery(NGSILDQueryParams params) {
    LOGGER
            .debug("georel " + params.getGeoRel() + " relation : " + params.getGeoRel().getRelation());
    return params.getGeoRel().getRelation() != null || params.getCoordinates() != null
            || params.getGeometry() != null || params.getGeoProperty() != null;
  }
  private <T> T getOrDefault(T value, T def) {
    return (value == null) ? def : value;
  }

}
