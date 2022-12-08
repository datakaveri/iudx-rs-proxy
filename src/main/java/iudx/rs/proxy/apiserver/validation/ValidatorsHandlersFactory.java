package iudx.rs.proxy.apiserver.validation;

import io.vertx.core.MultiMap;
import iudx.rs.proxy.apiserver.util.RequestType;
import iudx.rs.proxy.apiserver.validation.types.*;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.rs.proxy.apiserver.util.ApiServerConstants.*;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.HEADER_PUBLIC_KEY;

public class ValidatorsHandlersFactory {

  private static final Logger LOGGER = LogManager.getLogger(ValidatorsHandlersFactory.class);

  public List<Validator> build(
      final RequestType requestType,
      final MultiMap parameters) {
    LOGGER.debug("getValidation4Context() started for :" + requestType);
    LOGGER.debug("type :" + requestType);
    List<Validator> validator = null;

    switch (requestType) {
      case ENTITY:
        validator = getEntityRequestValidations(parameters);
        break;
      case TEMPORAL:
        validator = getTemporalRequestValidations(parameters);
        break;
      default:
        break;
    }

    return validator;
  }


  private List<Validator> getEntityRequestValidations(final MultiMap parameters) {
    List<Validator> validators = new ArrayList<>();

    validators.add(new IDTypeValidator(parameters.get(NGSILDQUERY_ID), true));
    validators.add(new AttrsTypeValidator(parameters.get(NGSILDQUERY_ATTRIBUTE), false));
    validators.add(new GeoRelTypeValidator(parameters.get(NGSILDQUERY_GEOREL), false));
    validators.add(new GeometryTypeValidator(parameters.get(NGSILDQUERY_GEOMETRY), false));
    validators.add(new GeoPropertyTypeValidator(parameters.get(NGSILDQUERY_GEOPROPERTY), false));
    validators.add(new QTypeValidator(parameters.get(NGSLILDQUERY_Q), false));
    validators.add(new DistanceTypeValidator(parameters.get(NGSILDQUERY_MAXDISTANCE), false));
    validators.add(new DistanceTypeValidator(parameters.get("maxDistance"), false));
    validators.add(new OptionsTypeValidator(parameters.get(IUDXQUERY_OPTIONS), false));
    validators.add(new CoordinatesTypeValidator(parameters.get(NGSILDQUERY_COORDINATES), false));

    // pagination optional fields
    validators.add(new PaginationLimitTypeValidator(parameters.get(NGSILDQUERY_SIZE), false));
    validators.add(new PaginationOffsetTypeValidator(parameters.get(NGSILDQUERY_FROM), false));

    //optional header public key
    validators.add(new HeaderKeyTypeValidation(parameters.get(HEADER_PUBLIC_KEY),false));

    return validators;

  }

  private List<Validator> getTemporalRequestValidations(
      final MultiMap parameters) {

    List<Validator> validators = new ArrayList<>();

    validators.add(new IDTypeValidator(parameters.get(NGSILDQUERY_ID), true));
    validators.add(new AttrsTypeValidator(parameters.get(NGSILDQUERY_ATTRIBUTE), false));
    validators.add(new GeoRelTypeValidator(parameters.get(NGSILDQUERY_GEOREL), false));
    validators.add(new GeometryTypeValidator(parameters.get(NGSILDQUERY_GEOMETRY), false));
    validators.add(new GeoPropertyTypeValidator(parameters.get(NGSILDQUERY_GEOPROPERTY), false));
    validators.add(new QTypeValidator(parameters.get(NGSILDQUERY_OPERATOR), false));
    validators.add(new DistanceTypeValidator(parameters.get(NGSILDQUERY_MAXDISTANCE), false));
    validators.add(new DistanceTypeValidator(parameters.get("maxDistance"), false));
    validators.add(new OptionsTypeValidator(parameters.get(IUDXQUERY_OPTIONS), false));
    validators.add(new CoordinatesTypeValidator(parameters.get(NGSILDQUERY_COORDINATES), false));
    validators.add(new TimeRelTypeValidator(parameters.get(NGSILDQUERY_TIMEREL), true));
    validators.add(new DateTypeValidator(parameters.get(NGSILDQUERY_TIME), true));
    validators.add(new DateTypeValidator(parameters.get(NGSILDQUERY_ENDTIME), false));

    //optional header public key
    validators.add(new HeaderKeyTypeValidation(parameters.get(HEADER_PUBLIC_KEY),false));

    return validators;
  }
}
