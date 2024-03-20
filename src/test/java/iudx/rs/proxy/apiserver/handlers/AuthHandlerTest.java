package iudx.rs.proxy.apiserver.handlers;

import static iudx.rs.proxy.apiserver.util.ApiServerConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.vertx.core.*;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.rs.proxy.authenticator.AuthenticationService;
import iudx.rs.proxy.common.Api;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
class AuthHandlerTest {

  private static String dxApiBasePath;
  private static Api apis;
  @Mock RoutingContext routingContext;
  @Mock HttpServerResponse httpServerResponse;
  @Mock HttpServerRequest httpServerRequest;
  @Mock HttpMethod httpMethod;
  @Mock AsyncResult<JsonObject> asyncResult;
  @Mock MultiMap map;
  @Mock Throwable throwable;
  @Mock Future<Void> voidFuture;
  AuthHandler authHandler;
  JsonObject jsonObject;

  public static Stream<Arguments> urls() {
    dxApiBasePath = "/ngsi-ld/v1";
    apis = Api.getInstance(dxApiBasePath);
    return Stream.of(
        Arguments.of(apis.getPostTemporalEndpoint(), apis.getPostTemporalEndpoint()),
        Arguments.of(apis.getConsumerAuditEndpoint(), apis.getConsumerAuditEndpoint()),
        Arguments.of(apis.getProviderAuditEndpoint(), apis.getProviderAuditEndpoint()),
        Arguments.of(apis.getProviderAuditEndpoint(), apis.getProviderAuditEndpoint()),
        Arguments.of(apis.getAsyncSearchEndPoint(), apis.getAsyncSearchEndPoint()),
        Arguments.of(apis.getAsyncStatusEndpoint(), apis.getAsyncStatusEndpoint()),
        Arguments.of(apis.getEntitiesEndpoint(), apis.getEntitiesEndpoint()),
        Arguments.of(apis.getTemporalEndpoint(), apis.getTemporalEndpoint()),
        Arguments.of(apis.getPostEntitiesEndpoint(), apis.getPostEntitiesEndpoint()));
  }

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) {
    jsonObject = new JsonObject();
    jsonObject.put("Dummy Key", "Dummy Value");
    jsonObject.put("IID", "Dummy IID value");
    jsonObject.put("USER_ID", "Dummy USER_ID");
    jsonObject.put("EXPIRY", "Dummy EXPIRY");
    jsonObject.put("dxApiBasePath", "/ngsi-ld/v1");
    lenient().when(httpServerRequest.method()).thenReturn(httpMethod);
    lenient().when(httpMethod.toString()).thenReturn("GET");
    lenient().when(routingContext.request()).thenReturn(httpServerRequest);
    dxApiBasePath = jsonObject.getString("dxApiBasePath");
    dxApiBasePath = "/ngsi-ld/v1";
    apis = Api.getInstance(dxApiBasePath);

    authHandler = AuthHandler.create(Vertx.vertx(), apis, true);
    vertxTestContext.completeNow();
  }

  @ParameterizedTest(name = "{index}) url = {0}, path = {1}")
  @MethodSource("urls")
  @DisplayName("Test handler for succeeded authHandler")
  public void testCanHandleSuccess(String url, String path, VertxTestContext vertxTestContext) {

    JsonObject jsonObject = mock(JsonObject.class);
    RequestBody requestBody = mock(RequestBody.class);

    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(jsonObject);
    when(jsonObject.copy()).thenReturn(jsonObject);
    when(routingContext.body().asJsonObject()).thenReturn(jsonObject);
    when(routingContext.request()).thenReturn(httpServerRequest);
    when(httpServerRequest.uri())
        .thenReturn(
            "/ngsi-ld/v1/temporal/entities?timerel=during&id=b58da193-23d9-43eb-b98a-a103d4b6103c&q=ppbNumber=T13010001107&time=2020-11-11T18:30:00Z&endtime=2020-11-13T17:09:24Z' \\\n"
                + "--header 'token:' ");
    when(httpServerRequest.path()).thenReturn(url);
    AuthHandler.authenticator = mock(AuthenticationService.class);
    when(httpServerRequest.headers()).thenReturn(map);
    when(map.get(anyString())).thenReturn("Dummy Token");
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(jsonObject);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(3)).handle(asyncResult);
                return null;
              }
            })
        .when(AuthHandler.authenticator)
        .tokenIntrospect(any(), any(), any(), any());

    authHandler.handle(routingContext);

    assertEquals(path, routingContext.request().path());
    assertEquals("Dummy Token", routingContext.request().headers().get(HEADER_TOKEN));
    assertEquals("GET", routingContext.request().method().toString());
    verify(AuthHandler.authenticator, times(1)).tokenIntrospect(any(), any(), any(), any());
    verify(routingContext, times(4)).body();

    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test handle method for Item not found")
  public void testCanHandleFailure(VertxTestContext vertxTestContext) {
    authHandler = new AuthHandler();
    String str = ASYNC + STATUS;
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("Dummy Key", "Dummy Value");

    RequestBody requestBody = mock(RequestBody.class);

    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(jsonObject);
    // when(jsonObject.copy()).thenReturn(jsonObject);

    // when(routingContext.body().asJsonObject()).thenReturn(jsonObject);
    when(httpServerRequest.path()).thenReturn(str);
    AuthHandler.authenticator = mock(AuthenticationService.class);
    when(httpServerRequest.headers()).thenReturn(map);
    when(httpServerRequest.uri())
        .thenReturn(
            "/ngsi-ld/v1/temporal/entities?timerel=during&id=b58da193-23d9-43eb-b98a-a103d4b6103c&q=ppbNumber=T13010001107&time=2020-11-11T18:30:00Z&endtime=2020-11-13T17:09:24Z' \\\n"
                + "--header 'token:' ");

    when(map.get(anyString())).thenReturn("Dummy token");
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Dummy throwable message: Not Found");
    when(routingContext.response()).thenReturn(httpServerResponse);
    when(httpServerResponse.putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);
    when(httpServerResponse.end(anyString())).thenReturn(voidFuture);
    when(asyncResult.succeeded()).thenReturn(false);
    doAnswer(
            (Answer<AsyncResult<JsonObject>>)
                arg0 -> {
                  ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(3)).handle(asyncResult);
                  return null;
                })
        .when(AuthHandler.authenticator)
        .tokenIntrospect(any(), any(), any(), any());

    authHandler.handle(routingContext);

    assertEquals(ASYNC + STATUS, routingContext.request().path());
    assertEquals("Dummy token", routingContext.request().headers().get(HEADER_TOKEN));
    assertEquals("GET", routingContext.request().method().toString());
    verify(AuthHandler.authenticator, times(1)).tokenIntrospect(any(), any(), any(), any());
    verify(httpServerResponse, times(1)).setStatusCode(anyInt());
    verify(httpServerResponse, times(1)).putHeader(anyString(), anyString());
    verify(httpServerResponse, times(1)).end(anyString());
    verify(routingContext, times(3)).body();

    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test handle method for Authentication Failure")
  public void testCanHandleAuthenticationFailure(VertxTestContext vertxTestContext) {
    authHandler = new AuthHandler();
    String str = ASYNC + STATUS;
    JsonObject jsonObject = mock(JsonObject.class);
    Map<String, String> stringMap = mock(Map.class);

    RequestBody requestBody = mock(RequestBody.class);

    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(jsonObject);
    when(jsonObject.copy()).thenReturn(jsonObject);
    when(httpServerRequest.path()).thenReturn(str);

    AuthHandler.authenticator = mock(AuthenticationService.class);

    when(httpServerRequest.uri())
        .thenReturn(
            "/ngsi-ld/v1/temporal/entities?timerel=during&id=b58da193-23d9-43eb-b98a-a103d4b6103c&q=ppbNumber=T13010001107&time=2020-11-11T18:30:00Z&endtime=2020-11-13T17:09:24Z' \\\n"
                + "--header 'token:' ");
    when(httpServerRequest.headers()).thenReturn(map);
    when(map.get(anyString())).thenReturn("Dummy token");
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Dummy throwable message: Authentication Failure");
    when(routingContext.response()).thenReturn(httpServerResponse);
    when(httpServerResponse.putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);
    when(httpServerResponse.end(anyString())).thenReturn(voidFuture);
    when(asyncResult.succeeded()).thenReturn(false);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(3)).handle(asyncResult);
                return null;
              }
            })
        .when(AuthHandler.authenticator)
        .tokenIntrospect(any(), any(), any(), any());

    authHandler.handle(routingContext);

    assertEquals(ASYNC + STATUS, routingContext.request().path());
    assertEquals("Dummy token", routingContext.request().headers().get(HEADER_TOKEN));
    assertEquals("GET", routingContext.request().method().toString());
    verify(AuthHandler.authenticator, times(1)).tokenIntrospect(any(), any(), any(), any());
    verify(httpServerResponse, times(1)).setStatusCode(anyInt());
    verify(httpServerResponse, times(1)).putHeader(anyString(), anyString());
    verify(httpServerResponse, times(1)).end(anyString());
    verify(routingContext, times(3)).body();

    vertxTestContext.completeNow();
  }

  @DisplayName("Test create method")
  @Test
  public void testCanCreate(VertxTestContext vertxTestContext) {
    AuthHandler.authenticator = mock(AuthenticationService.class);
    assertNotNull(AuthHandler.create(Vertx.vertx(), apis, false));
    vertxTestContext.completeNow();
  }
}
