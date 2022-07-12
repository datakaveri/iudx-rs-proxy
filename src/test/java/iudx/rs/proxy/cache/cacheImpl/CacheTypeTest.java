package iudx.rs.proxy.cache.cacheImpl;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.rs.proxy.common.HttpStatusCode;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class CacheTypeTest {

    @ParameterizedTest
    @EnumSource
    public void test(CacheType CacheType, VertxTestContext vertxTestContext)
    {
        assertNotNull(CacheType);
        vertxTestContext.completeNow();
    }



}