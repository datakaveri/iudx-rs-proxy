package iudx.rs.proxy.authenticator;
import io.micrometer.core.ipc.http.HttpSender;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.rs.proxy.authenticator.authorization.Api;
import iudx.rs.proxy.authenticator.model.JwtData;
import iudx.rs.proxy.cache.CacheService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.module.Configuration;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
@Disabled
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

    private static String delegateJwt =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJhMTNlYjk1NS1jNjkxLTRmZDMtYjIwMC1mMThiYzc4ODEwYjUiLCJpc3MiOiJhdXRoLnRlc3QuY29tIiwiYXVkIjoiZm9vYmFyLml1ZHguaW8iLCJleHAiOjE2MjgxODIzMjcsImlhdCI6MTYyODEzOTEyNywiaWlkIjoicmk6ZXhhbXBsZS5jb20vNzllN2JmYTYyZmFkNmM3NjViYWM2OTE1NGMyZjI0Yzk0Yzk1MjIwYS9yZXNvdXJjZS1ncm91cC9yZXNvdXJjZSIsInJvbGUiOiJkZWxlZ2F0ZSIsImNvbnMiOnsiYWNjZXNzIjpbImFwaSIsInN1YnMiLCJpbmdlc3QiLCJmaWxlIl19fQ.tUoO1L-tXByxNtjY_iK41neeshCiYrNr505wWn1hC1ACwoeL9frebABeFiCqJQGrsBsGOZ1-OACZdHBNcetwyw";
    private static String consumerJwt =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiIzMmE0Yjk3OS00ZjRhLTRjNDQtYjBjMy0yZmUxMDk5NTJiNWYiLCJpc3MiOiJhdXRoLnRlc3QuY29tIiwiYXVkIjoiZm9vYmFyLml1ZHguaW8iLCJleHAiOjE2MjgxODUzNTksImlhdCI6MTYyODE0MjE1OSwiaWlkIjoicmc6ZXhhbXBsZS5jb20vNzllN2JmYTYyZmFkNmM3NjViYWM2OTE1NGMyZjI0Yzk0Yzk1MjIwYS9yZXNvdXJjZS1ncm91cCIsInJvbGUiOiJjb25zdW1lciIsImNvbnMiOnsiYWNjZXNzIjpbImFwaSIsInN1YnMiLCJpbmdlc3QiLCJmaWxlIl19fQ.NoEiJB_5zwTU-zKbFHTefMuqDJ7L6mA11mfckzA4IZOSrdweSmR6my0zGcf7hEVljX9OOFm4tToZQYfCtPg4Uw";
    private static String providerJwt =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJhMTNlYjk1NS1jNjkxLTRmZDMtYjIwMC1mMThiYzc4ODEwYjUiLCJpc3MiOiJhdXRoLnRlc3QuY29tIiwiYXVkIjoiZm9vYmFyLml1ZHguaW8iLCJleHAiOjE2MjgxODU4MjEsImlhdCI6MTYyODE0MjYyMSwiaWlkIjoicmc6ZXhhbXBsZS5jb20vNzllN2JmYTYyZmFkNmM3NjViYWM2OTE1NGMyZjI0Yzk0Yzk1MjIwYS9yZXNvdXJjZS1ncm91cCIsInJvbGUiOiJwcm92aWRlciIsImNvbnMiOnsiYWNjZXNzIjpbImFwaSIsInN1YnMiLCJpbmdlc3QiLCJmaWxlIl19fQ.BSoCQPUT8_YA-6p7-_OEUBOfbbvQZs8VKwDzdnubT3gutVueRe42a9d9mhszhijMQK7Qa0ww_rmAaPhA_2jP6w";

    private static String closedResourceToken =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJhMTNlYjk1NS1jNjkxLTRmZDMtYjIwMC1mMThiYzc4ODEwYjUiLCJpc3MiOiJhdXRoLnRlc3QuY29tIiwiYXVkIjoicnMuaXVkeC5pbyIsImV4cCI6MTYyODYxMjg5MCwiaWF0IjoxNjI4NTY5NjkwLCJpaWQiOiJyZzppaXNjLmFjLmluLzg5YTM2MjczZDc3ZGFjNGNmMzgxMTRmY2ExYmJlNjQzOTI1NDdmODYvcnMuaXVkeC5pby9zdXJhdC1pdG1zLXJlYWx0aW1lLWluZm9ybWF0aW9uL3N1cmF0LWl0bXMtbGl2ZS1ldGEiLCJyb2xlIjoiY29uc3VtZXIiLCJjb25zIjp7ImFjY2VzcyI6WyJhcGkiLCJzdWJzIiwiaW5nZXN0IiwiZmlsZSJdfX0.OBJZUc15s8gDA6PB5IK3KkUGmjvJQWr7RvByhMXmmrCULmPGgtesFmNDVG2gqD4WXZob5OsjxZ1vxRmgMBgLxw";

    private String id = "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053";

    @BeforeAll
    @DisplayName("Initialize Vertx and deploy Auth Verticle")
    static void init(Vertx vertx, VertxTestContext testContext) {


        authConfig = new JsonObject();
        authConfig.put("host", "rs.iudx.io");
        authConfig.put("audience", "");
        authConfig.put("port", "1");


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

       /* cacheServiceMock=mock(CacheService.class);
        jwtAuthenticationService = new JwtAuthenticationServiceImpl(vertx, jwtAuth, authConfig,cacheServiceMock);
        jwtAuthImplSpy = spy(jwtAuthenticationService);

        LOGGER.info("Auth tests setup complete");*/
        testContext.completeNow();
    }


    @Test
    @DisplayName("decode valid jwt")
    public void decodeJwtProviderSuccess(VertxTestContext testContext) {
        System.out.println("providerJwt####" + providerJwt);

             jwtAuthenticationService.decodeJwt(providerJwt).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals("provider", handler.result().getRole());

            } else {   testContext.completeNow();
                testContext.failNow(handler.cause());
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
        authInfo.put("apiEndpoint", Api.ENTITIES.getApiEndpoint());
        authInfo.put("method", HttpSender.Method.GET);

        JwtData jwtData = new JwtData();
        jwtData.setIss("auth.test.com");
        jwtData.setAud("rs.iudx.io");
        jwtData.setExp(123456);
        jwtData.setIat(123456);
        jwtData.setIid("ri:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
        jwtData.setRole("consumer");
        jwtData.setCons(new JsonObject().put("access", new JsonArray().add("file")));


        jwtAuthenticationService.validateAccess(jwtData, true, authInfo).onComplete(handler -> {
            if (handler.succeeded()) {
                testContext.completeNow();
            } else {
                testContext.failNow("invalid access");
            }
        });
    }

    @Test
    public  void isOpenResourceTest(Vertx vertx){


    }
}
