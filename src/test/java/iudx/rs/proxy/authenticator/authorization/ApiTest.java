package iudx.rs.proxy.authenticator.authorization;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.rs.proxy.common.Api;

@ExtendWith(VertxExtension.class)
class ApiTest {

    @Test
    public void test(VertxTestContext vertxTestContext)
    {
      
        String basePath="/ngsi-ld/v1";
        Api api=Api.getInstance(basePath);
        assertEquals("/ngsi-ld/v1/entities",api.getEntitiesEndpoint());
        vertxTestContext.completeNow();
    }


}