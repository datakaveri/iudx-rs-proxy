package iudx.rs.proxy.apiserver.validation;

import static iudx.rs.proxy.apiserver.util.ApiServerConstants.IUDXQUERY_OPTIONS;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.NGSILDQUERY_ATTRIBUTE;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.NGSILDQUERY_COORDINATES;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.NGSILDQUERY_ENDTIME;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.NGSILDQUERY_FROM;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.NGSILDQUERY_GEOMETRY;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.NGSILDQUERY_GEOPROPERTY;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.NGSILDQUERY_GEOREL;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.NGSILDQUERY_ID;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.NGSILDQUERY_MAXDISTANCE;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.NGSILDQUERY_Q;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.NGSILDQUERY_SIZE;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.NGSILDQUERY_TIME;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.NGSILDQUERY_TIMEREL;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.rs.proxy.apiserver.util.RequestType;
import iudx.rs.proxy.apiserver.validation.types.AttrsTypeValidator;
import iudx.rs.proxy.apiserver.validation.types.CoordinatesTypeValidator;
import iudx.rs.proxy.apiserver.validation.types.DateTypeValidator;
import iudx.rs.proxy.apiserver.validation.types.DistanceTypeValidator;
import iudx.rs.proxy.apiserver.validation.types.GeoPropertyTypeValidator;
import iudx.rs.proxy.apiserver.validation.types.GeoRelTypeValidator;
import iudx.rs.proxy.apiserver.validation.types.GeometryTypeValidator;
import iudx.rs.proxy.apiserver.validation.types.IDTypeValidator;
import iudx.rs.proxy.apiserver.validation.types.OptionsTypeValidator;
import iudx.rs.proxy.apiserver.validation.types.PaginationLimitTypeValidator;
import iudx.rs.proxy.apiserver.validation.types.PaginationOffsetTypeValidator;
import iudx.rs.proxy.apiserver.validation.types.QTypeValidator;
import iudx.rs.proxy.apiserver.validation.types.TimeRelTypeValidator;
import iudx.rs.proxy.apiserver.validation.types.Validator;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ValidatorsHandlersFactory {

  private static final Logger LOGGER = LogManager.getLogger(ValidatorsHandlersFactory.class);

  public List<Validator> build(
      final RequestType requestType,
      final MultiMap parameters,
      final MultiMap headers) {
    LOGGER.debug("getValidation4Context() started for :" + requestType);
    LOGGER.debug("type :" + requestType);
    List<Validator> validator = null;

    switch (requestType) {
      case TEMPORAL:
        validator = getTemporalRequestValidations(parameters, headers);
        break;
      default:
        break;
    }

    return validator;
  }

  private List<Validator> getTemporalRequestValidations(
      final MultiMap parameters, final MultiMap headers) {

    List<Validator> validators = new ArrayList<>();

    validators.add(new IDTypeValidator(parameters.get(NGSILDQUERY_ID), true));
    validators.add(new AttrsTypeValidator(parameters.get(NGSILDQUERY_ATTRIBUTE), false));
    validators.add(new GeoRelTypeValidator(parameters.get(NGSILDQUERY_GEOREL), false));
    validators.add(new GeometryTypeValidator(parameters.get(NGSILDQUERY_GEOMETRY), false));
    validators.add(new GeoPropertyTypeValidator(parameters.get(NGSILDQUERY_GEOPROPERTY), false));
    validators.add(new QTypeValidator(parameters.get(NGSILDQUERY_Q), false));
    validators.add(new DistanceTypeValidator(parameters.get(NGSILDQUERY_MAXDISTANCE), false));
    validators.add(new DistanceTypeValidator(parameters.get("maxDistance"), false));
    validators.add(new OptionsTypeValidator(parameters.get(IUDXQUERY_OPTIONS), false));
    validators.add(new CoordinatesTypeValidator(parameters.get(NGSILDQUERY_COORDINATES), false));
    validators.add(new TimeRelTypeValidator(parameters.get(NGSILDQUERY_TIMEREL), true));
    validators.add(new DateTypeValidator(parameters.get(NGSILDQUERY_TIME), true));
    validators.add(new DateTypeValidator(parameters.get(NGSILDQUERY_ENDTIME), false));

    // pagination optional fields
    validators.add(new PaginationLimitTypeValidator(parameters.get(NGSILDQUERY_SIZE), false));
    validators.add(new PaginationOffsetTypeValidator(parameters.get(NGSILDQUERY_FROM), false));

    return validators;
  }
}
