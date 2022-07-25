package iudx.rs.proxy.apiserver.util;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class RequestTypeTest {
    @ParameterizedTest
    @EnumSource
    public void test(RequestType requestType, VertxTestContext vertxTestContext)
    {
        assertNotNull(requestType);
        vertxTestContext.completeNow();
    }

}