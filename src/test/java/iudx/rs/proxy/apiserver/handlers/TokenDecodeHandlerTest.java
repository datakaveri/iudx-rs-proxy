package iudx.rs.proxy.apiserver.handlers;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.rs.proxy.authenticator.AuthenticationService;
import iudx.rs.proxy.authenticator.model.JwtData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static iudx.rs.proxy.apiserver.util.ApiServerConstants.HEADER_TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
class TokenDecodeHandlerTest {

    @Mock
    RoutingContext routingContext;
    @Mock
    HttpServerResponse httpServerResponse;
    @Mock
    HttpServerRequest httpServerRequest;
    @Mock
    HttpMethod httpMethod;
    @Mock
    AsyncResult<JwtData> asyncResult;
    @Mock
    MultiMap map;
    @Mock
    Throwable throwable;
    TokenDecodeHandler tokenDecodeHandler;

    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext) {
        tokenDecodeHandler = new TokenDecodeHandler();
        lenient().when(httpServerRequest.method()).thenReturn(httpMethod);
        lenient().when(httpMethod.toString()).thenReturn("GET");
        lenient().when(routingContext.request()).thenReturn(httpServerRequest);
        vertxTestContext.completeNow();
    }

    @DisplayName("Test create method")
    @Test
    public void testCreate(VertxTestContext vertxTestContext) {
        AuthHandler.authenticator = mock(AuthenticationService.class);
        assertNotNull(TokenDecodeHandler.create(Vertx.vertx()));
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test handle method for Authentication Failure")
    public void testCanHandleAuthenticationFailure(VertxTestContext vertxTestContext) {
        tokenDecodeHandler = new TokenDecodeHandler();

        when(routingContext.request()).thenReturn(httpServerRequest);
        when(httpServerRequest.headers()).thenReturn(map);
        when(map.get(anyString())).thenReturn("Dummy token");

        TokenDecodeHandler.authenticationServiceDecoder = mock(AuthenticationService.class);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn("Dummy throwable message: Authentication Failure");
        when(routingContext.response()).thenReturn(httpServerResponse);
        when(httpServerResponse.putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
        when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);
        when(asyncResult.succeeded()).thenReturn(false);

        doAnswer(new Answer<AsyncResult<JwtData>>() {
            @Override
            public AsyncResult<JwtData> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JwtData>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(TokenDecodeHandler.authenticationServiceDecoder).decodeJwt(any(), any());

        tokenDecodeHandler.handle(routingContext);

        assertEquals("Dummy token", routingContext.request().headers().get(HEADER_TOKEN));
        assertEquals("GET", routingContext.request().method().toString());
        verify(TokenDecodeHandler.authenticationServiceDecoder, times(1)).decodeJwt(any(), any());
        verify(httpServerResponse, times(1)).setStatusCode(anyInt());
        verify(httpServerResponse, times(1)).putHeader(anyString(), anyString());
        verify(httpServerResponse, times(1)).end(anyString());
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test handle method for Authentication Failure")
    public void testCanHandleAuthenticationFailure2(VertxTestContext vertxTestContext) {
        tokenDecodeHandler = new TokenDecodeHandler();

        when(routingContext.request()).thenReturn(httpServerRequest);
        when(httpServerRequest.headers()).thenReturn(map);
        when(map.get(anyString())).thenReturn("Dummy token");

        TokenDecodeHandler.authenticationServiceDecoder = mock(AuthenticationService.class);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn("Dummy throwable message: Not Found");
        when(routingContext.response()).thenReturn(httpServerResponse);
        when(httpServerResponse.putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
        when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);
        when(asyncResult.succeeded()).thenReturn(false);

        doAnswer(new Answer<AsyncResult<JwtData>>() {
            @Override
            public AsyncResult<JwtData> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JwtData>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(TokenDecodeHandler.authenticationServiceDecoder).decodeJwt(any(), any());

        tokenDecodeHandler.handle(routingContext);

        assertEquals("Dummy token", routingContext.request().headers().get(HEADER_TOKEN));
        assertEquals("GET", routingContext.request().method().toString());
        verify(TokenDecodeHandler.authenticationServiceDecoder, times(1)).decodeJwt(any(), any());
        verify(httpServerResponse, times(1)).setStatusCode(anyInt());
        verify(httpServerResponse, times(1)).putHeader(anyString(), anyString());
        verify(httpServerResponse, times(1)).end(anyString());

        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test handle method for Authentication Failure")
    public void testCanHandleAuthenticationFailure3(VertxTestContext vertxTestContext) {
        tokenDecodeHandler = new TokenDecodeHandler();

        when(routingContext.request()).thenReturn(httpServerRequest);
        when(httpServerRequest.headers()).thenReturn(map);
        when(map.get(anyString())).thenReturn("Dummy token");

        TokenDecodeHandler.authenticationServiceDecoder = mock(AuthenticationService.class);
        when(asyncResult.succeeded()).thenReturn(true);

        doAnswer(new Answer<AsyncResult<JwtData>>() {
            @Override
            public AsyncResult<JwtData> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JwtData>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(TokenDecodeHandler.authenticationServiceDecoder).decodeJwt(any(), any());

        tokenDecodeHandler.handle(routingContext);
        vertxTestContext.completeNow();
    }
}

