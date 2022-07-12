package iudx.rs.proxy.apiserver.validation.types;

import io.vertx.core.Vertx;
import io.vertx.core.cli.annotations.Description;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.rs.proxy.apiserver.exceptions.DxRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(VertxExtension.class)
class StringTypeValidatorTest {


    @BeforeEach
    public  void setup(Vertx vertx, VertxTestContext testContext){
        testContext.completeNow();
    }
    static Stream<Arguments> invalidValues() {
        return Stream.of(
                Arguments.of("dummy", true,Pattern.compile("a-zA-Z")),
                Arguments.of("  ", false,Pattern.compile("a-zA-Z")),
                Arguments.of(null,true,Pattern.compile("a-zA-Z")));
    }
    @ParameterizedTest
    @MethodSource("invalidValues")
    @DisplayName("String type parameter invalid values")
    public  void testValidStringTypeValue(String value, boolean required,Pattern regexPattern,VertxTestContext testContext){

       StringTypeValidator stringTypeValidator = new StringTypeValidator(value,required,regexPattern);
        assertThrows(DxRuntimeException.class,()->stringTypeValidator.isValid());
       testContext.completeNow();

    }

    static Stream<Arguments> allowedValues() {
        // Add any valid value for which validation will pass successfully.
        return Stream.of(
                Arguments.of("within", true,"within"),
                Arguments.of("intersects", true,"intersects"),
                Arguments.of("near", true,"near"),
                Arguments.of(null, false,null));
    }


    @ParameterizedTest
    @MethodSource("allowedValues")
    @Description("String type parameter valid values.")
    public void testValidGeoRelValue(String value, boolean required, Pattern regexPattern, Vertx vertx,
                                     VertxTestContext testContext) {
        StringTypeValidator stringTypeValidator = new StringTypeValidator(value, required,regexPattern);
        assertTrue(stringTypeValidator.isValid());
        testContext.completeNow();
    }

}