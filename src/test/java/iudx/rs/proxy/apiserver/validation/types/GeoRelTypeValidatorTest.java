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
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class GeoRelTypeValidatorTest {

private GeoRelTypeValidator  geoRelTypeValidator;

@BeforeEach
    public void setup(Vertx vertx, VertxTestContext testContext){
    testContext.completeNow();
}
static Stream<Arguments>allowedValues(){
    return Stream.of(
            Arguments.of("within",true),
            Arguments.of("intersects",true),
            Arguments.of("near",true),
            Arguments.of(null,false)
    );
}

    @ParameterizedTest
    @MethodSource("allowedValues")
    @Description("georel parameter allowed values.")
    public void testValidGeoRelValue(String value, boolean required, Vertx vertx,
                                     VertxTestContext testContext) {
        geoRelTypeValidator = new GeoRelTypeValidator(value, required);
        assertTrue(geoRelTypeValidator.isValid());
        testContext.completeNow();
    }


    static Stream<Arguments> invalidValues() {
        // Add any invalid value for which validation must fail.
        String random600Id = RandomStringUtils.random(600);
        return Stream.of(
                Arguments.of("", true),
                Arguments.of("  ", true),
                Arguments.of("around", true),
                Arguments.of("bypass", true),
                Arguments.of("1=1", true),
                Arguments.of("AND XYZ=XYZ", true),
                Arguments.of(random600Id, true),
                Arguments.of("%2cX%2c", true),
                Arguments.of("",false)
        );
    }

    @ParameterizedTest
    @MethodSource("invalidValues")
    @Description("geo parameter invalid values.")
    public void testInvalidGeoRelValue(String value, boolean required, Vertx vertx,
                                       VertxTestContext testContext) {
        geoRelTypeValidator = new GeoRelTypeValidator(value, required);
        assertThrows(DxRuntimeException.class, () -> geoRelTypeValidator.isValid());
        testContext.completeNow();
    }


}