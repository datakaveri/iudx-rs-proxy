package iudx.rs.proxy.common;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.rs.proxy.authenticator.authorization.Api;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class HttpStatusCodeTest {


    @ParameterizedTest
    @EnumSource
    public void test(HttpStatusCode httpStatusCode, VertxTestContext vertxTestContext)
    {
        assertNotNull(httpStatusCode);
        vertxTestContext.completeNow();
    }

}