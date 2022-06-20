package iudx.rs.proxy.metering.util;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.rs.proxy.common.ResponseUrn;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static iudx.rs.proxy.common.ResponseUrn.SUCCESS_URN;
import static iudx.rs.proxy.metering.util.Constants.SUCCESS;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class ResponseBuilderTest {

    @Test
    @DisplayName("Set Type And Title Test")
    public void setTypeAndTitleTest(VertxTestContext vertxTestContext){
        ResponseBuilder responseBuilder= new ResponseBuilder("200");
        responseBuilder.setTypeAndTitle(200);
        assertEquals("successful operations", SUCCESS_URN.getMessage());
       /*responseBuilder.setTypeAndTitle(204);
        assertEquals("successful operations",SUCCESS_URN);*/
        responseBuilder.setTypeAndTitle(400);
        assertEquals("bad request parameter",ResponseUrn.BAD_REQUEST_URN.getMessage());
        vertxTestContext.completeNow();
    }

}