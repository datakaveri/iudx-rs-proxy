package iudx.rs.proxy.authenticator;
import static iudx.rs.proxy.authenticator.authorization.Method.GET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import java.net.http.HttpResponse;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import io.micrometer.core.ipc.http.HttpSender;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.rs.proxy.authenticator.authorization.AuthorizationRequest;
import iudx.rs.proxy.authenticator.model.JwtData;
import iudx.rs.proxy.cache.CacheService;
import iudx.rs.proxy.common.Api;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationServiceImplTest {
    private static final Logger LOGGER = LogManager.getLogger(JwtAuthenticationServiceImplTest.class);
    private static JsonObject authConfig;
    private static JwtAuthenticationServiceImpl jwtAuthenticationService;

    private static JwtAuthenticationServiceImpl jwtAuthImplSpy;

    private static CacheService cacheServiceMock;
    @Mock
    HttpRequest<Buffer> httpRequestMock;
    @Mock
    HttpResponse<Buffer> httpResponseMock;
    @Mock
    HttpRequest<Buffer> httpRequest;
    @Mock
    io.vertx.ext.web.client.HttpResponse<Buffer> httpResponse;

    @Mock
    AsyncResult<io.vertx.ext.web.client.HttpResponse<Buffer>> asyncResult;
    static WebClient catWebClient;

    private static Api apis;

    private static String delegateJwt =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJhMTNlYjk1NS1jNjkxLTRmZDMtYjIwMC1mMThiYzc4ODEwYjUiLCJpc3MiOiJhdXRoLnRlc3QuY29tIiwiYXVkIjoiZm9vYmFyLml1ZHguaW8iLCJleHAiOjE2MjgxODIzMjcsImlhdCI6MTYyODEzOTEyNywiaWlkIjoicmk6ZXhhbXBsZS5jb20vNzllN2JmYTYyZmFkNmM3NjViYWM2OTE1NGMyZjI0Yzk0Yzk1MjIwYS9yZXNvdXJjZS1ncm91cC9yZXNvdXJjZSIsInJvbGUiOiJkZWxlZ2F0ZSIsImNvbnMiOnsiYWNjZXNzIjpbImFwaSIsInN1YnMiLCJpbmdlc3QiLCJmaWxlIl19fQ.tUoO1L-tXByxNtjY_iK41neeshCiYrNr505wWn1hC1ACwoeL9frebABeFiCqJQGrsBsGOZ1-OACZdHBNcetwyw";
    private static String consumerJwt =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiIzMmE0Yjk3OS00ZjRhLTRjNDQtYjBjMy0yZmUxMDk5NTJiNWYiLCJpc3MiOiJhdXRoLnRlc3QuY29tIiwiYXVkIjoiZm9vYmFyLml1ZHguaW8iLCJleHAiOjE2MjgxODUzNTksImlhdCI6MTYyODE0MjE1OSwiaWlkIjoicmc6ZXhhbXBsZS5jb20vNzllN2JmYTYyZmFkNmM3NjViYWM2OTE1NGMyZjI0Yzk0Yzk1MjIwYS9yZXNvdXJjZS1ncm91cCIsInJvbGUiOiJjb25zdW1lciIsImNvbnMiOnsiYWNjZXNzIjpbImFwaSIsInN1YnMiLCJpbmdlc3QiLCJmaWxlIl19fQ.NoEiJB_5zwTU-zKbFHTefMuqDJ7L6mA11mfckzA4IZOSrdweSmR6my0zGcf7hEVljX9OOFm4tToZQYfCtPg4Uw";
    private static String providerJwt =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJhMTNlYjk1NS1jNjkxLTRmZDMtYjIwMC1mMThiYzc4ODEwYjUiLCJpc3MiOiJhdXRoLnRlc3QuY29tIiwiYXVkIjoiZm9vYmFyLml1ZHguaW8iLCJleHAiOjE2MjgxODU4MjEsImlhdCI6MTYyODE0MjYyMSwiaWlkIjoicmc6ZXhhbXBsZS5jb20vNzllN2JmYTYyZmFkNmM3NjViYWM2OTE1NGMyZjI0Yzk0Yzk1MjIwYS9yZXNvdXJjZS1ncm91cCIsInJvbGUiOiJwcm92aWRlciIsImNvbnMiOnsiYWNjZXNzIjpbImFwaSIsInN1YnMiLCJpbmdlc3QiLCJmaWxlIl19fQ.BSoCQPUT8_YA-6p7-_OEUBOfbbvQZs8VKwDzdnubT3gutVueRe42a9d9mhszhijMQK7Qa0ww_rmAaPhA_2jP6w";

    private static String closedResourceToken =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJhMTNlYjk1NS1jNjkxLTRmZDMtYjIwMC1mMThiYzc4ODEwYjUiLCJpc3MiOiJhdXRoLnRlc3QuY29tIiwiYXVkIjoicnMuaXVkeC5pbyIsImV4cCI6MTYyODYxMjg5MCwiaWF0IjoxNjI4NTY5NjkwLCJpaWQiOiJyZzppaXNjLmFjLmluLzg5YTM2MjczZDc3ZGFjNGNmMzgxMTRmY2ExYmJlNjQzOTI1NDdmODYvcnMuaXVkeC5pby9zdXJhdC1pdG1zLXJlYWx0aW1lLWluZm9ybWF0aW9uL3N1cmF0LWl0bXMtbGl2ZS1ldGEiLCJyb2xlIjoiY29uc3VtZXIiLCJjb25zIjp7ImFjY2VzcyI6WyJhcGkiLCJzdWJzIiwiaW5nZXN0IiwiZmlsZSJdfX0.OBJZUc15s8gDA6PB5IK3KkUGmjvJQWr7RvByhMXmmrCULmPGgtesFmNDVG2gqD4WXZob5OsjxZ1vxRmgMBgLxw";

    private String id = "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053";

    static JwtData jwtData = new JwtData();;

    @BeforeAll
    @DisplayName("Initialize Vertx and deploy Auth Verticle")
    static void init(Vertx vertx, VertxTestContext testContext) {


        jwtData.setIss("auth.test.com");
        jwtData.setAud("rs.iudx.io");
        jwtData.setExp(1627408865);
        jwtData.setIat(1627408865);
        jwtData.setIid("ri:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
        jwtData.setRole("consumer");
        jwtData.setCons(null);


        authConfig = new JsonObject();
        authConfig.put("catServerHost", "rs.iudx.io");
        authConfig.put("host", "rs.iudx.io");
        authConfig.put("catServerPort", 8080);
        authConfig.put("dxApiBasePath","/ngsi-ld/v1");
        authConfig.put("dxCatalogueBasePath", "/iudx/cat/v1");
        authConfig.put("dxAuthBasePath", "/auth/v1");
        authConfig.put("audience", "rs.iudx.io");

        JWTAuthOptions jwtAuthOptions = new JWTAuthOptions();
        jwtAuthOptions.addPubSecKey(
                new PubSecKeyOptions()
                        .setAlgorithm("ES256")
                        .setBuffer("-----BEGIN PUBLIC KEY-----\n" +
                                "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE8BKf2HZ3wt6wNf30SIsbyjYPkkTS\n" +
                                "GGyyM2/MGF/zYTZV9Z28hHwvZgSfnbsrF36BBKnWszlOYW0AieyAUKaKdg==\n" +
                                "-----END PUBLIC KEY-----\n" +
                                ""));


        jwtAuthOptions.getJWTOptions().setIgnoreExpiration(true);// ignore token expiration only for test
        JWTAuth jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);

        String dxApiBasePath=authConfig.getString("dxApiBasePath");
        apis=Api.getInstance(dxApiBasePath);
        
        cacheServiceMock=mock(CacheService.class);
        jwtAuthenticationService = new JwtAuthenticationServiceImpl(vertx, jwtAuth, authConfig,cacheServiceMock,apis);
        jwtAuthImplSpy = spy(jwtAuthenticationService);

        LOGGER.info("Auth tests setup complete");
        testContext.completeNow();
    }

    /*  @Test
      @DisplayName("decode valid jwt")
      public void decodeJwtProviderSuccess(VertxTestContext testContext) {
          jwtAuthenticationService.decodeJwt(providerJwt).onComplete(handler -> {
              if (handler.succeeded()) {
                  assertEquals("provider", handler.result().getRole());
                  testContext.completeNow();
              } else {
                  testContext.failNow(handler.cause());
              }
          });
      }



         @Test
         @DisplayName("decode valid jwt - delegate")
         public void decodeJwtDelegateSuccess(VertxTestContext testContext) {
             jwtAuthenticationService.decodeJwt(delegateJwt).onComplete(handler -> {
                 if (handler.succeeded()) {
                     assertEquals("delegate", handler.result().getRole());
                     testContext.completeNow();
                 } else {
                     testContext.failNow(handler.cause());
                 }
             });
         }
  */
         @Test
         public void test(VertxTestContext testContext) {
           AuthorizationRequest authReq=new AuthorizationRequest(GET, apis.getEntitiesEndpoint());
           AuthorizationRequest authReq1=new AuthorizationRequest(GET, "/ngsi-ld/v1/entities");
           assertEquals(authReq, authReq1);
           testContext.completeNow();

         }

         @Test
         @DisplayName("decode valid jwt - consumer")
         public void decodeJwtConsumerSuccess(VertxTestContext testContext) {
             doAnswer(Answer -> Future.succeededFuture(new JsonObject())).when(jwtAuthImplSpy).getCatItem(any());

             jwtAuthImplSpy.decodeJwt(consumerJwt,handler -> {
                 System.out.println(handler.succeeded());
                 if (handler.succeeded()) {
                     assertEquals("consumer", handler.result().getRole());
                     testContext.completeNow();
                 } else {
                     testContext.failNow(handler.cause());
                 }
             });
         }

         @Test
         @DisplayName("decode invalid jwt")
         public void decodeJwtFailure(VertxTestContext testContext) {
             String jwt =
                     "eyJ0eXAiOiJKV1QiLCJbGciOiJFUzI1NiJ9.eyJzdWIiOiJhM2U3ZTM0Yy00NGJmLTQxZmYtYWQ4Ni0yZWUwNGE5NTQ0MTgiLCJpc3MiOiJhdXRoLnRlc3QuY29tIiwiYXVkIjoiZm9vYmFyLml1ZHguaW8iLCJleHAiOjE2Mjc2ODk5NDAsImlhdCI6MTYyNzY0Njc0MCwiaWlkIjoicmc6ZXhhbXBsZS5jb20vNzllN2JmYTYyZmFkNmM3NjViYWM2OTE1NGMyZjI0Yzk0Yzk1MjIwYS9yZXNvdXJjZS1ncm91cCIsInJvbGUiOiJkZWxlZ2F0ZSIsImNvbnMiOnt9fQ.eJjCUvWuGD3L3Dn2fKj8Ydl1byGoyRS59VfL6ZJcdKR3_eIhm6SOY-CW3p5XDSYVhRTlWvlPLjfXYo9t_PxgnA";
             jwtAuthenticationService.decodeJwt(jwt,handler -> {
                 if (handler.succeeded()) {
                     testContext.failNow(handler.cause());
                 } else {
                     testContext.completeNow();
                 }
             });
         }

         @Test
         @DisplayName("success - allow consumer access to /entities endpoint for access [api,subs]")
         public void access4ConsumerTokenEntitiesAPI(VertxTestContext testContext) {

             JsonObject authInfo = new JsonObject();

             String id = "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053";

             authInfo.put("token", consumerJwt);
             authInfo.put("id", id);
             authInfo.put("apiEndpoint", apis.getEntitiesEndpoint());
             authInfo.put("method", GET);

             JwtData jwtData = new JwtData();
             jwtData.setIss("auth.test.com");
             jwtData.setAud("rs.iudx.io");
             jwtData.setExp(1627408865);
             jwtData.setIat(1627408865);
             jwtData.setIid("ri:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
             jwtData.setRole("consumer");
             jwtData.setCons(new JsonObject().put("access", new JsonArray().add("file")));


             jwtAuthenticationService.validateAccess(jwtData,true, authInfo).onComplete(handler -> {
                 if (handler.succeeded()) {
                     testContext.completeNow();
                 } else {
                     testContext.failNow("invalid access");
                 }
             });
         }


         @Test
         @DisplayName("fail - allow consumer access to /entities endpoint for null access")
         public void failAccess4ConsumerTokenEntitiesAPI(VertxTestContext testContext) {

             JsonObject authInfo = new JsonObject();

             authInfo.put("token", consumerJwt);
             authInfo.put("id", "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
             authInfo.put("apiEndpoint", apis.getEntitiesEndpoint());
             authInfo.put("method", GET);

             JwtData jwtData = new JwtData();
             jwtData.setIss("auth.test.com");
             jwtData.setAud("rs.iudx.io");
             jwtData.setExp(1627408865);
             jwtData.setIat(1627408865);
             jwtData.setIid("ri:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
             jwtData.setRole("consumer");
             jwtData.setCons(null);


             jwtAuthenticationService.validateAccess(jwtData, false, authInfo).onComplete(handler -> {
                 if (handler.succeeded()) {
                     testContext.failNow("success for invalid access");
                 } else {
                     testContext.completeNow();

                 }
             });
         }

     @Test
     @DisplayName("fail - consumer access to /entities endpoint for access [subs]")
     public void fail4ConsumerTokenEntitiesAPI(VertxTestContext testContext) {

         JsonObject authInfo = new JsonObject();

         authInfo.put("token", consumerJwt);
         authInfo.put("id", "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
         authInfo.put("apiEndpoint", apis.getEntitiesEndpoint());
         authInfo.put("method", GET);

         JwtData jwtData = new JwtData();
         jwtData.setIss("auth.test.com");
         jwtData.setAud("file.iudx.io");
         jwtData.setExp(1627408865);
         jwtData.setIat(1627408865);
         jwtData.setIid("ri:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
         jwtData.setRole("consumer");
         jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api")));


         jwtAuthenticationService.validateAccess(jwtData, false, authInfo).onComplete(handler -> {
             if (handler.succeeded()) {
               testContext.completeNow();
             } else {
               testContext.failNow("invalid access allowed");
             }
         });
     }


         @Test
         @DisplayName("success - token interospection allow access")
         public void successTokenInterospect(VertxTestContext testContext) {
             JsonObject authInfo = new JsonObject();
             JwtData jwtData = new JwtData();
             jwtData.setIss("auth.test.com");
             jwtData.setAud("rs.iudx.io");
             jwtData.setExp(1627408865);
             jwtData.setIat(1627408865);
             jwtData.setIid("ri:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
             jwtData.setRole("consumer");
             jwtData.setCons(null);

             authInfo.put("token", consumerJwt);
             authInfo.put("id", "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
             authInfo.put("apiEndpoint", apis.getEntitiesEndpoint());
             authInfo.put("method", GET);

             JsonObject request = new JsonObject();


             doAnswer(Answer -> Future.succeededFuture(true)).when(jwtAuthImplSpy).isValidAudienceValue(any());
             doAnswer(Answer -> Future.succeededFuture("OPEN")).when(jwtAuthImplSpy).isOpenResource(any());


             AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
             when(asyncResult.succeeded()).thenReturn(false);


             Mockito.doAnswer(new Answer<AsyncResult<JwtData>>() {
                 @SuppressWarnings("unchecked")
                 @Override
                 public AsyncResult<JwtData> answer(InvocationOnMock arg0) throws Throwable {
                     ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                     return null;
                 }
             }).when(cacheServiceMock).get(any(), any());

             jwtAuthImplSpy.tokenIntrospect(request,authInfo, jwtData,handler -> {
                 if (handler.succeeded()) {
                     testContext.completeNow();
                 } else {
                     testContext.failNow("failed");
                 }
             });
         }
         @Test
         @DisplayName("success - token interospection deny access[invalid client id]")
         public void failureTokenInterospectRevokedClient(VertxTestContext testContext) {
             JsonObject authInfo = new JsonObject();

             authInfo.put("token", consumerJwt);
             authInfo.put("id", "example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
             authInfo.put("apiEndpoint", apis.getEntitiesEndpoint());
             authInfo.put("method", GET);

             JwtData jwtData = new JwtData();
             jwtData.setIss("auth.test.com");
             jwtData.setAud("rs.iudx.io");
             jwtData.setExp(1627408865);
             jwtData.setIat(1627408865);
             jwtData.setIid("ri:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
             jwtData.setRole("consumer");
             jwtData.setCons(null);

             JsonObject request = new JsonObject();


             doAnswer(Answer -> Future.succeededFuture(true)).when(jwtAuthImplSpy).isValidAudienceValue(any());

             JsonObject cacheresponse = new JsonObject();
             JsonArray responseArray = new JsonArray();
             cacheresponse.put("value", "2019-10-19T14:20:00");
             responseArray.add(cacheresponse);


             AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
             when(asyncResult.succeeded()).thenReturn(true);
             when(asyncResult.result()).thenReturn(new JsonObject().put("result", responseArray));


             Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
                 @SuppressWarnings("unchecked")
                 @Override
                 public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                     ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                     return null;
                 }
             }).when(cacheServiceMock).get(any(), any());

             jwtAuthImplSpy.tokenIntrospect(request, authInfo, jwtData, handler -> {
                 if (handler.succeeded()) {
                     testContext.failNow("failed");
                 } else {
                     testContext.completeNow();
                 }
             });
         }

         @Test
         @DisplayName("fail - token interospection deny access [invalid audience]")
         public void failureTokenInterospectInvalidAudience(VertxTestContext testContext) {
             JsonObject authInfo = new JsonObject();

             authInfo.put("token", consumerJwt);
             authInfo.put("id", "example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
             authInfo.put("apiEndpoint", apis.getEntitiesEndpoint());
             authInfo.put("method", GET);

             JsonObject request = new JsonObject();



             doAnswer(Answer -> Future.failedFuture("invalid audience value"))
                     .when(jwtAuthImplSpy)
                     .isValidAudienceValue(any());

             jwtAuthImplSpy.tokenIntrospect(request, authInfo,jwtData, handler -> {
                 if (handler.succeeded()) {
                     testContext.failNow("failed");
                 } else {
                     testContext.completeNow();
                 }
             });
         }

         @Test
        @DisplayName("fail - token interospection deny access [invalid resource]")
         public void failureTokenInterospectInvalidResource(VertxTestContext testContext) {
             JsonObject authInfo = new JsonObject();

             authInfo.put("token", consumerJwt);
             authInfo.put("id", "example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
             authInfo.put("apiEndpoint", apis.getEntitiesEndpoint());
             authInfo.put("method", GET);

             JsonObject request = new JsonObject();



             doAnswer(Answer -> Future.succeededFuture(true))
                     .when(jwtAuthImplSpy)
                     .isValidAudienceValue(any());

             AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
             when(asyncResult.succeeded()).thenReturn(false);


             Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
                 @SuppressWarnings("unchecked")
                 @Override
                 public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                     ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                     return null;
                 }
             }).when(cacheServiceMock).get(any(), any());

             jwtAuthImplSpy.tokenIntrospect(request, authInfo, jwtData, handler -> {
                 if (handler.succeeded()) {
                     testContext.failNow("failed");
                 } else {
                     testContext.completeNow();
                 }
             });
         }

         @Test
         @DisplayName("fail - invalid audience value")
         public void fail4InvalidAud(VertxTestContext testContext) {

             JwtData jwt = new JwtData();
             jwt.setAud("rs.iudx.in");
             jwtAuthenticationService.isValidAudienceValue(jwt).onComplete(handler -> {
                 if (handler.succeeded()) {
                     testContext.failNow("passed for invalid value");
                 } else {
                     testContext.completeNow();
                 }
             });
         }

         @Test
         @DisplayName("success - valid audience value")
         public void success4ValidAud(VertxTestContext testContext) {

             JwtData jwt = new JwtData();
             jwt.setAud("rs.iudx.io");
             jwtAuthenticationService.isValidAudienceValue(jwt).onComplete(handler -> {
                 if (handler.succeeded()) {
                     testContext.completeNow();
                 } else {
                     testContext.failNow("failed for valid value");
                 }
             });
         }

         @Test
         @DisplayName("success - is open resource")
         public void success4openResource(VertxTestContext testContext) {
             JsonObject authInfo = new JsonObject();

             String id = "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053";

             authInfo.put("token", consumerJwt);
             authInfo.put("id", id);
             authInfo.put("apiEndpoint", apis.getEntitiesEndpoint());
             authInfo.put("method", GET);

             JwtData jwtData = new JwtData();
             jwtData.setIss("auth.test.com");
             jwtData.setAud("rs.iudx.io");
             jwtData.setExp(1627408865);
             jwtData.setIat(1627408865);
             jwtData.setIid("ri:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
             jwtData.setRole("consumer");
             jwtData.setCons(new JsonObject().put("access", new JsonArray().add("file")));

             doAnswer(Answer -> Future.succeededFuture("OPEN")).when(jwtAuthImplSpy).isOpenResource(any());

             jwtAuthImplSpy.isOpenResource(id).onComplete(openResourceHandler -> {
                 if(openResourceHandler.succeeded() && openResourceHandler.result().equals("OPEN")) {
                     testContext.completeNow();
                 } else {
                     testContext.failNow("open resource validation failed");
                 }
             });
         }

        @Test
         @DisplayName("failure - is open resource")
         public void failure4openResource(VertxTestContext testContext) {
             JsonObject authInfo = new JsonObject();

             String id = "example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group";

             authInfo.put("token", consumerJwt);
             authInfo.put("id", id);
             authInfo.put("apiEndpoint", apis.getEntitiesEndpoint());
             authInfo.put("method", HttpSender.Method.GET);

             JwtData jwtData = new JwtData();
             jwtData.setIss("auth.test.com");
             jwtData.setAud("file.iudx.io");
             jwtData.setExp(1627408865);
             jwtData.setIat(1627408865);
             jwtData.setIid("ri:example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
             jwtData.setRole("consumer");
             jwtData.setCons(new JsonObject().put("access", new JsonArray().add("file")));

             jwtAuthenticationService.isOpenResource(id).onComplete(openResourceHandler -> {
                 if(openResourceHandler.succeeded()) {
                     testContext.failNow("open resource validation failed");
                 } else {
                     testContext.completeNow();
                 }
             });
         }
    @Test
    @DisplayName("authRequest should have same hashcode")
    public void authRequestShouldhaveSamehash() {
        AuthorizationRequest authR1= new AuthorizationRequest( GET, apis.getEntitiesEndpoint());
        AuthorizationRequest authR2= new AuthorizationRequest(GET, apis.getEntitiesEndpoint());
        assertEquals(authR1.hashCode(), authR2.hashCode());
    }

   @Test
    @DisplayName("authRequest should not equal")
    public void authRequestShouldNotEquals() {
        AuthorizationRequest authR1= new AuthorizationRequest(GET, apis.getTemporalEndpoint());
        AuthorizationRequest authR2= new AuthorizationRequest(GET, apis.getEntitiesEndpoint());
        Assertions.assertFalse(authR1.equals(authR2));

    }
  @Test
  @DisplayName("Testing Failure for isResourceExist method with List of String IDs")
  public void testIsResourceExistFailure(VertxTestContext vertxTestContext)
  {
      String id="Dummy id";
      String groupACL="Dummy id";
      JsonObject responseJSonObject=new JsonObject();
      responseJSonObject.put("type","urn:dx:cat:Success");
      responseJSonObject.put("totalHits", 10);
      JwtAuthenticationServiceImpl.catWebClient=mock(WebClient.class);
      when(JwtAuthenticationServiceImpl.catWebClient.get(anyInt(),anyString(),anyString())).thenReturn(httpRequest);
      when(httpRequest.addQueryParam(anyString(),anyString())).thenReturn(httpRequest);
      when(httpRequest.expect(any())).thenReturn(httpRequest);
      when(asyncResult.result()).thenReturn(httpResponse);
      when(httpResponse.bodyAsJsonObject()).thenReturn(responseJSonObject);
      when(httpResponse.statusCode()).thenReturn(400);
      doAnswer(new Answer<AsyncResult<io.vertx.ext.web.client.HttpResponse<Buffer>>>() {
          @Override
          public AsyncResult<io.vertx.ext.web.client.HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {

              ((Handler<AsyncResult<io.vertx.ext.web.client.HttpResponse<Buffer>>>)arg0.getArgument(0)).handle(asyncResult);
              return null;
          }
      }).when(httpRequest).send(any());
      jwtAuthenticationService.isResourceExist(id,groupACL).onComplete(handler -> {
          if (handler.succeeded())
          {
              vertxTestContext.failNow("false");
          }
          else
          {
              vertxTestContext.completeNow();
          }
      });
  }
    @Test
    @DisplayName("Testing Failure for isResourceExist method with List of String IDs")
    public void testIsResourceExistFailure2(VertxTestContext vertxTestContext)
    {
        String id="Dummy id";
        String groupACL="Dummy id";
        JsonObject responseJSonObject=new JsonObject();
        responseJSonObject.put("type","dummy");
        responseJSonObject.put("totalHits", 10);
        JwtAuthenticationServiceImpl.catWebClient=mock(WebClient.class);
        when(JwtAuthenticationServiceImpl.catWebClient.get(anyInt(),anyString(),anyString())).thenReturn(httpRequest);
        when(httpRequest.addQueryParam(anyString(),anyString())).thenReturn(httpRequest);
        when(httpRequest.expect(any())).thenReturn(httpRequest);
        when(asyncResult.result()).thenReturn(httpResponse);
        when(httpResponse.bodyAsJsonObject()).thenReturn(responseJSonObject);
        when(httpResponse.statusCode()).thenReturn(200);
        doAnswer(new Answer<AsyncResult<io.vertx.ext.web.client.HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<io.vertx.ext.web.client.HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {

                ((Handler<AsyncResult<io.vertx.ext.web.client.HttpResponse<Buffer>>>)arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(httpRequest).send(any());
        jwtAuthenticationService.isResourceExist(id,groupACL).onComplete(handler -> {
            if (handler.succeeded())
            {
                vertxTestContext.failNow("Not Found");
            }
            else
            {
                vertxTestContext.completeNow();
            }
        });
    }
   @Test
    @DisplayName("Testing Failure for isResourceExist method with List of String IDs")
    public void testIsResourceExistFailure3(VertxTestContext vertxTestContext) {
        String id = "Dummy id";
        String groupACL = "Dummy id";
        String resourceACL="SECURE";
        JsonObject responseJSonObject = new JsonObject();
        JsonArray jsonarray=new JsonArray();
        responseJSonObject.put("type", "wrong type");
        responseJSonObject.put("totalHits", 10);
//   resourceACL=responseJSonObject.getJsonArray("results").getJsonObject(0).getString("accessPolicy");
        JwtAuthenticationServiceImpl.catWebClient = mock(WebClient.class);
        when(JwtAuthenticationServiceImpl.catWebClient.get(anyInt(), anyString(), anyString())).thenReturn(httpRequest);
        when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
        when(httpRequest.expect(any())).thenReturn(httpRequest);
        when(asyncResult.result()).thenReturn(httpResponse);
        when(httpResponse.bodyAsJsonObject()).thenReturn(responseJSonObject);
        when(httpResponse.statusCode()).thenReturn(200);


        doAnswer(new Answer<AsyncResult<io.vertx.ext.web.client.HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<io.vertx.ext.web.client.HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {

                ((Handler<AsyncResult<io.vertx.ext.web.client.HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(httpRequest).send(any());
        jwtAuthenticationService.isResourceExist(id, groupACL).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow("Not Found");
            } else {
                vertxTestContext.completeNow();
            }
        });
    }
    @Test
    @DisplayName("Testing Failure for isResourceExist method with List of String IDs")
    public void testGroupAccessPolicyFailure2(VertxTestContext vertxTestContext) {
        String id = "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053";    String groupACL = "Dummy id";
        String resourceACL="SECURE";
        JsonObject responseJSonObject = new JsonObject();
        JsonArray jsonarray=new JsonArray();
        responseJSonObject.put("type", "urn:dx:cat:Success");
        responseJSonObject.put("totalHits", 10);
//   resourceACL=responseJSonObject.getJsonArray("results").getJsonObject(0).getString("accessPolicy");
        JwtAuthenticationServiceImpl.catWebClient = mock(WebClient.class);
        when(JwtAuthenticationServiceImpl.catWebClient.get(anyInt(), anyString(), anyString())).thenReturn(httpRequest);
        when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
        when(httpRequest.expect(any())).thenReturn(httpRequest);
        when(asyncResult.result()).thenReturn(httpResponse);
//    when(httpResponse.bodyAsJsonObject()).thenReturn(responseJSonObject);
        when(httpResponse.statusCode()).thenReturn(400);


        doAnswer(new Answer<AsyncResult<io.vertx.ext.web.client.HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<io.vertx.ext.web.client.HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {

                ((Handler<AsyncResult<io.vertx.ext.web.client.HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(httpRequest).send(any());
        jwtAuthenticationService.getGroupAccessPolicy(id).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow("Not Found");
            } else {
                vertxTestContext.completeNow();
            }
        });
    }
    @Test
    @DisplayName("Testing Failure for isResourceExist method with List of String IDs")
    public void testGroupAccessPolicyFailure(VertxTestContext vertxTestContext) {
        String id = "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053";    String groupACL = "Dummy id";
        String resourceACL="SECURE";
        JsonObject responseJSonObject = new JsonObject();
        JsonArray jsonarray=new JsonArray();
        responseJSonObject.put("type", "wrong type");
        responseJSonObject.put("totalHits", 10);
//   resourceACL=responseJSonObject.getJsonArray("results").getJsonObject(0).getString("accessPolicy");
        JwtAuthenticationServiceImpl.catWebClient = mock(WebClient.class);
        when(JwtAuthenticationServiceImpl.catWebClient.get(anyInt(), anyString(), anyString())).thenReturn(httpRequest);
        when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
        when(httpRequest.expect(any())).thenReturn(httpRequest);
        when(asyncResult.result()).thenReturn(httpResponse);
        when(httpResponse.bodyAsJsonObject()).thenReturn(responseJSonObject);
        when(httpResponse.statusCode()).thenReturn(200);


        doAnswer(new Answer<AsyncResult<io.vertx.ext.web.client.HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<io.vertx.ext.web.client.HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {

                ((Handler<AsyncResult<io.vertx.ext.web.client.HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(httpRequest).send(any());
        jwtAuthenticationService.getGroupAccessPolicy(id).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow("Not Found");
            } else {
                vertxTestContext.completeNow();
            }
        });
    }
}
