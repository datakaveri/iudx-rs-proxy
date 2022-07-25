package iudx.rs.proxy.apiserver.validation.types;

import io.vertx.core.Vertx;
import io.vertx.core.cli.annotations.Description;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.rs.proxy.apiserver.exceptions.DxRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class GeoPropertyTypeValidatorTest {

    private GeoPropertyTypeValidator geoPropertyTypeValidator;

    @BeforeEach
    public void setup(Vertx vertx, VertxTestContext testContext) {
        testContext.completeNow();
    }

    static Stream<Arguments> allowedValues() {
        // Add any valid value which will pass successfully.
        return Stream.of(
                Arguments.of("location", true),
                Arguments.of("Location", true),
                Arguments.of(null, false));
    }

    @ParameterizedTest
    @MethodSource("allowedValues")
    @Description("GeoProperty parameter allowed values.")
    public void testValidGeoPropertyValue(String value, boolean required, Vertx vertx,
                                          VertxTestContext testContext) {
        geoPropertyTypeValidator = new GeoPropertyTypeValidator(value, required);
        assertTrue(geoPropertyTypeValidator.isValid());
        testContext.completeNow();
    }


    static Stream<Arguments> invalidValues() {
        // Add any valid value which will pass successfully.
        String random600Id = RandomStringUtils.random(600);
        return Stream.of(
                Arguments.of("", true),
                Arguments.of("  ", true),
                Arguments.of("around", true),
                Arguments.of("bypass", true),
                Arguments.of("1=1", true),
                Arguments.of("AND XYZ=XYZ", true),
                Arguments.of(random600Id, true),
                Arguments.of("", false)
        );
    }


    @ParameterizedTest
    @MethodSource("invalidValues")
    @Description("GeoProperty parameter invalid values.")
    public void testInvalidGeoPropertyValue(String value, boolean required,
                                            Vertx vertx,
                                            VertxTestContext testContext) {
        geoPropertyTypeValidator = new GeoPropertyTypeValidator(value, required);
        assertThrows(DxRuntimeException.class, () -> geoPropertyTypeValidator.isValid());
        testContext.completeNow();
    }

}