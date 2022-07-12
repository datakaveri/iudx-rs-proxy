package iudx.rs.proxy.apiserver.exceptions;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.rs.proxy.common.ResponseUrn;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class DxRuntimeExceptionTest {


    @Test
    @DisplayName("Test constructor ")
    public void testConstructor(VertxTestContext vertxTestContext)
    {
        int statusCode = 400;
        ResponseUrn responseUrn = ResponseUrn.INVALID_GEO_PARAM_URN;
        DxRuntimeException obj = new DxRuntimeException(statusCode,responseUrn);
        assertEquals(400,obj.getStatusCode());
        assertEquals(ResponseUrn.INVALID_GEO_PARAM_URN, obj.getUrn());
        assertEquals(ResponseUrn.INVALID_GEO_PARAM_URN.getMessage(), obj.getMessage());
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test constructor for Throwable RuntimeException")
    public void testConstructorForRuntimeException(VertxTestContext vertxTestContext)
    {
        int statusCode = 400;
        ResponseUrn responseUrn = ResponseUrn.INVALID_GEO_PARAM_URN;
        RuntimeException runtimeException = new RuntimeException("failed");
        DxRuntimeException obj = new DxRuntimeException(statusCode, responseUrn, runtimeException);
        assertEquals(400, obj.getStatusCode());
        assertEquals(ResponseUrn.INVALID_GEO_PARAM_URN,obj.getUrn());
        assertEquals(ResponseUrn.INVALID_GEO_PARAM_URN.getMessage(),obj.getMessage());
        vertxTestContext.completeNow();
    }

}