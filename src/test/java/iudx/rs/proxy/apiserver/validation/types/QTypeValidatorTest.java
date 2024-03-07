package iudx.rs.proxy.apiserver.validation.types;

import io.vertx.core.Vertx;
import io.vertx.core.cli.annotations.Description;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.rs.proxy.apiserver.exceptions.DxRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class QTypeValidatorTest {



    private QTypeValidator qTypeValidator;

    static Stream<Arguments> invalidValues() {
        // Add any invalid value which will throw error.
        return Stream.of(
                Arguments.of("", true),
                Arguments.of("    ", true),
                Arguments.of(RandomStringUtils.random(600), true),
                Arguments.of("referenceLevel<>15.0", true),
                Arguments.of("referenceLevel>>15.0", true),
                Arguments.of("referenceLevel===15.0", true),
                Arguments.of("referenceLevel+15.0", true),
                Arguments.of("referenceLevel/15.0", true),
                Arguments.of("referenceLevel*15.0", true),
                Arguments.of("reference_Level$>15.0", true),
                Arguments.of("reference$Level>15.0", true),
                Arguments.of("referenceLevel!<15.0", true),
                Arguments.of("",false),
                Arguments.of(null,true));
    }


    @BeforeEach
    public void setup(Vertx vertx, VertxTestContext testContext) {
        testContext.completeNow();
    }

    @ParameterizedTest
    @MethodSource("invalidValues")
    @Description("q parameter type failure for different invalid values.")
    public void testInvalidQTypeValue(String value, boolean required, Vertx vertx,
                                      VertxTestContext testContext) {
        qTypeValidator = new QTypeValidator(value, required);
        assertThrows(DxRuntimeException.class, () -> qTypeValidator.isValid());
        testContext.completeNow();
    }

    static Stream<Arguments> validValues() {
        return Stream.of(
                Arguments.of("referenceLevel>15.0", true),
                Arguments.of("reference_Level>15.0", true),
                Arguments.of(
                        "id==iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR055",
                        true),
                Arguments.of(null, false));
    }

    @ParameterizedTest
    @MethodSource("validValues")
    @Description("success for valid q query")
    public void testValidQValue(String value, boolean required, Vertx vertx,
                                VertxTestContext testContext) {
        qTypeValidator = new QTypeValidator(value, required);
        assertTrue(qTypeValidator.isValid());
        testContext.completeNow();
    }
    @ParameterizedTest
    @MethodSource("validValues")
    public void TestIsvlidValue(String value, boolean required, Vertx vertx,
                                     VertxTestContext vertxTestContext){
    qTypeValidator = new QTypeValidator(value, required);
    assertTrue(qTypeValidator.isValidValue("1.2"));

        vertxTestContext.completeNow();
}

    @ParameterizedTest
    @MethodSource("validValues")
    public void TestIsvlidValuethrow(String value, boolean required, Vertx vertx,
                                     VertxTestContext vertxTestContext){
        qTypeValidator = new QTypeValidator(value, required);
        assertThrows(DxRuntimeException.class, () -> qTypeValidator.isValidValue("abc"));

        vertxTestContext.completeNow();
    }

}