package iudx.rs.proxy.common;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class ResponseUrnTest {

    @ParameterizedTest
    @EnumSource
    public void test(ResponseUrn responseUrn, VertxTestContext testContext) {
        assertNotNull(responseUrn);
        testContext.completeNow();
    }

}