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

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(VertxExtension.class)
class OptionsTypeValidatorTest {


    private OptionsTypeValidator optionsValidator;

    static Stream<Arguments> values() {
        // Add any invalid value which will throw error.
        return Stream.of(
                Arguments.of("count1", true),
                Arguments.of("AND 1=1", true),
                Arguments.of("1==1", true));
    }

    @BeforeEach
    public void setup(Vertx vertx, VertxTestContext testContext) {
        testContext.completeNow();

    }

    @ParameterizedTest
    @MethodSource("values")
    @Description("options parameter type failure for different invalid values.")
    public void testInvalidOptionsValue(String value, boolean required, Vertx vertx,
                                        VertxTestContext testContext) {
        optionsValidator = new OptionsTypeValidator(value, required);
        assertThrows(DxRuntimeException.class, () -> optionsValidator.isValid());
        testContext.completeNow();
    }

    @Test
    @Description("success for valid options")
    public void testValidOptionsValue(Vertx vertx, VertxTestContext testContext) {
        optionsValidator = new OptionsTypeValidator("count", true);
        assertTrue(optionsValidator.isValid());
        testContext.completeNow();
    }

    @Test
    @Description("success for valid options")
    public void testValidNullOptionsValue(Vertx vertx, VertxTestContext testContext) {
        optionsValidator = new OptionsTypeValidator(null, false);
        assertTrue(optionsValidator.isValid());
        testContext.completeNow();
    }


}