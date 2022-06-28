package iudx.rs.proxy.apiserver.handlers;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.rs.proxy.apiserver.exceptions.DxRuntimeException;
import iudx.rs.proxy.apiserver.response.ResponseType;
import iudx.rs.proxy.common.ResponseUrn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
class FailureHandlerTest {

    private static FailureHandler validationFailureHandler;
    @Mock
    RoutingContext event;
    @Mock
    HttpServerResponse httpServerResponseMock;
    @Mock
    DxRuntimeException throwableMock;
    @Mock
    RuntimeException runtimeExceptionMock;

    @Mock
    Future<Void> voidFutureMock;
    ResponseUrn responseUrn = ResponseUrn.BAD_REQUEST_URN;

    @BeforeEach
    public void setUp() {
        validationFailureHandler = new FailureHandler();
    }

   @Test
   @DisplayName("DxRuntime exception test case")
   public void dxruntimeExceptiontest(VertxTestContext vertxTestContext) {
      /* RoutingContext routingContextMock = mock(RoutingContext.class);
       HttpServerResponse httpServerResponseMock = mock(HttpServerResponse.class);
       Future<Void> voidFutureMock = mock(Future.class);
       DxRuntimeException dxRuntimeExceptionMock = mock(DxRuntimeException.class);
*/
       when(event.failure()).thenReturn(throwableMock);
       when(throwableMock.getUrn()).thenReturn(responseUrn);
       when(throwableMock.getStatusCode()).thenReturn(400);
       when(event.response()).thenReturn(httpServerResponseMock);

       when(httpServerResponseMock.putHeader(anyString(),anyString())).thenReturn(httpServerResponseMock);
       when(httpServerResponseMock.setStatusCode(anyInt())).thenReturn(httpServerResponseMock);
       when(httpServerResponseMock.end(anyString())).thenReturn(voidFutureMock);
       validationFailureHandler.handle(event);

       DxRuntimeException dxRuntimeException = (DxRuntimeException) event.failure();
       assertEquals(400, dxRuntimeException.getStatusCode());
       vertxTestContext.completeNow();
   }


    @DisplayName("Test handle method when failure is RuntimeException")
    @Test
    public void testHandleWhenRuntimeException(VertxTestContext vertxTestContext) {
        when(event.failure()).thenReturn(runtimeExceptionMock);
        when(event.response()).thenReturn(httpServerResponseMock);
        when(httpServerResponseMock.putHeader(anyString(), anyString())).thenReturn(httpServerResponseMock);
        when(httpServerResponseMock.setStatusCode(anyInt())).thenReturn(httpServerResponseMock);
        when(httpServerResponseMock.end(anyString())).thenReturn(voidFutureMock);
        validationFailureHandler.handle(event);

        verify(httpServerResponseMock).setStatusCode(400);
        vertxTestContext.completeNow();
    }


}