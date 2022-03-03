package iudx.rs.proxy.apiserver.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import io.vertx.core.http.HttpMethod;
import java.util.regex.Pattern;

public class ApiServerConstants {

  // header
  public static final String HEADER_TOKEN = "token";
  public static final String HEADER_HOST = "Host";
  public static final String HEADER_ACCEPT = "Accept";
  public static final String HEADER_CONTENT_LENGTH = "Content-Length";
  public static final String HEADER_CONTENT_TYPE = "Content-Type";
  public static final String HEADER_ORIGIN = "Origin";
  public static final String HEADER_REFERER = "Referer";
  public static final String HEADER_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

  public static final Set<String> ALLOWED_HEADERS =
      new HashSet<>(Arrays.asList(HEADER_ACCEPT, HEADER_TOKEN, HEADER_CONTENT_LENGTH,
          HEADER_CONTENT_TYPE, HEADER_HOST, HEADER_ORIGIN, HEADER_REFERER, HEADER_ALLOW_ORIGIN));

  public static final Set<HttpMethod> ALLOWED_METHODS =
      new HashSet<>(Arrays.asList(HttpMethod.GET,
          HttpMethod.POST, HttpMethod.OPTIONS, HttpMethod.DELETE, HttpMethod.PATCH,
          HttpMethod.PUT));

  //path regex
  public static final String NGSILD_BASE_PATH = "/ngsi-ld/v1";
  public static final String NGSILD_TEMPORAL_URL = NGSILD_BASE_PATH + "/temporal/entities";

    // date-time format

    public static final String API_ENDPOINT = "apiEndpoint";
    public static final String API_METHOD = "method";
    public static final String ID = "id";
    public static final String IDS = "ids";

    // path regex
    public static final String TEMPORAL_URL_REGEX = NGSILD_TEMPORAL_URL + "(.*)";

    // ngsi-ld/IUDX query paramaters
    public static final String NGSILDQUERY_ID = "id";
    public static final String NGSILDQUERY_COORDINATES = "coordinates";
    public static final String NGSILDQUERY_GEOMETRY = "geometry";
    public static final String NGSILDQUERY_ATTRIBUTE = "attrs";
    public static final String NGSILDQUERY_GEOREL = "georel";
    public static final String NGSILDQUERY_TIMEREL = "timerel";
    public static final String NGSILDQUERY_TIME = "time";
    public static final String NGSILDQUERY_ENDTIME = "endtime";
    public static final String NGSILDQUERY_Q = "q";
    public static final String NGSILDQUERY_GEOPROPERTY = "geoproperty";
    public static final String NGSILDQUERY_MAXDISTANCE = "maxdistance";
    public static final String IUDXQUERY_OPTIONS = "options";
    public static final String NGSILDQUERY_FROM = "offset";
    public static final String NGSILDQUERY_SIZE = "limit";


    // request/response params
    public static final String CONTENT_TYPE = "content-type";
    public static final String APPLICATION_JSON = "application/json";

    public static final String JSON_ATTRIBUTE = "attribute";
    public static final String JSON_OPERATOR = "operator";
    public static final String JSON_VALUE = "value";
    public static final String JSON_TITLE = "title";
    public static final String JSON_DETAIL = "detail";
    public static final String USER_ID = "userid";
    public static final String EXPIRY = "expiry";
    public static final String IID = "iid";

    public static final String MSG_BAD_QUERY = "Bad query";


    // Validations
    public static final int VALIDATION_ID_MIN_LEN = 0;
    public static final int VALIDATION_ID_MAX_LEN = 512;
    public static final Pattern VALIDATION_ID_PATTERN =
        Pattern.compile(
            "^[a-zA-Z0-9.]{4,100}/{1}[a-zA-Z0-9.]{4,100}/{1}[a-zA-Z.]{4,100}/{1}[a-zA-Z-_.]{4,100}/{1}[a-zA-Z0-9-_.]{4,100}$");
    public static final int VALIDATION_MAX_ATTRS = 5;
    public static final int VALIDATION_COORDINATE_PRECISION_ALLOWED = 6;
    public static final int VALIDATIONS_MAX_ATTR_LENGTH = 100;
    public static final int VALIDATION_ALLOWED_COORDINATES = 10;

    public static final Pattern ID_REGEX =
        Pattern.compile(
            "^[a-zA-Z0-9.]{4,100}/{1}[a-zA-Z0-9.]{4,100}/{1}[a-zA-Z.]{4,100}/{1}[a-zA-Z-_.]{4,100}/{1}[a-zA-Z0-9-_.]{4,100}$");


    public static final double VALIDATION_ALLOWED_DIST = 1000.0;
    public static final int VALIDATION_PAGINATION_LIMIT_MAX = 5000;
    public static final int VALIDATION_PAGINATION_OFFSET_MAX = 49999;
    public static final List<Object> VALIDATION_ALLOWED_GEOM =
        List.of("Point", "point", "Polygon", "polygon", "LineString", "linestring", "bbox");
    public static final List<Object> VALIDATION_ALLOWED_GEOPROPERTY = List.of("location", "Location");
    public static final List<String> VALIDATION_ALLOWED_OPERATORS =
        List.of(">", "=", "<", ">=", "<=", "==", "!=");
    public static final List<String> VALIDATION_ALLOWED_TEMPORAL_REL =
        List.of("after", "before", "during", "between");

    public static final String VALIDATION_Q_ATTR_PATTERN = "^[a-zA-Z0-9_]{1,100}+$";

  }

