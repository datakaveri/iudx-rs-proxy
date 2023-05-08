package iudx.rs.proxy.metering;

import static iudx.rs.proxy.apiserver.util.ApiServerConstants.EPOCH_TIME;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.ISO_TIME;
import static iudx.rs.proxy.metering.util.Constants.API;
import static iudx.rs.proxy.metering.util.Constants.CONSUMER_ID;
import static iudx.rs.proxy.metering.util.Constants.DATABASE_IP;
import static iudx.rs.proxy.metering.util.Constants.DATABASE_NAME;
import static iudx.rs.proxy.metering.util.Constants.DATABASE_PASSWORD;
import static iudx.rs.proxy.metering.util.Constants.DATABASE_PORT;
import static iudx.rs.proxy.metering.util.Constants.DATABASE_TABLE_NAME;
import static iudx.rs.proxy.metering.util.Constants.DATABASE_USERNAME;
import static iudx.rs.proxy.metering.util.Constants.DETAIL;
import static iudx.rs.proxy.metering.util.Constants.DURING;
import static iudx.rs.proxy.metering.util.Constants.ENDPOINT;
import static iudx.rs.proxy.metering.util.Constants.END_TIME;
import static iudx.rs.proxy.metering.util.Constants.ID;
import static iudx.rs.proxy.metering.util.Constants.IID;
import static iudx.rs.proxy.metering.util.Constants.INVALID_DATE_DIFFERENCE;
import static iudx.rs.proxy.metering.util.Constants.INVALID_DATE_TIME;
import static iudx.rs.proxy.metering.util.Constants.INVALID_PROVIDER_ID;
import static iudx.rs.proxy.metering.util.Constants.INVALID_PROVIDER_REQUIRED;
import static iudx.rs.proxy.metering.util.Constants.POOL_SIZE;
import static iudx.rs.proxy.metering.util.Constants.PROVIDER_ID;
import static iudx.rs.proxy.metering.util.Constants.RESOURCE_ID;
import static iudx.rs.proxy.metering.util.Constants.RESPONSE_LIMIT_EXCEED;
import static iudx.rs.proxy.metering.util.Constants.RESPONSE_SIZE;
import static iudx.rs.proxy.metering.util.Constants.START_TIME;
import static iudx.rs.proxy.metering.util.Constants.SUCCESS;
import static iudx.rs.proxy.metering.util.Constants.TIME_NOT_FOUND;
import static iudx.rs.proxy.metering.util.Constants.TIME_RELATION;
import static iudx.rs.proxy.metering.util.Constants.TIME_RELATION_NOT_FOUND;
import static iudx.rs.proxy.metering.util.Constants.TITLE;
import static iudx.rs.proxy.metering.util.Constants.USERID_NOT_FOUND;
import static iudx.rs.proxy.metering.util.Constants.USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rabbitmq.RabbitMQClient;
import iudx.rs.proxy.common.Api;
import iudx.rs.proxy.configuration.Configuration;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import iudx.rs.proxy.database.DatabaseService;
import iudx.rs.proxy.databroker.DatabrokerService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import javax.xml.crypto.Data;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class MeteringServiceImplTest {

    private static final Logger LOGGER = LogManager.getLogger(MeteringServiceImplTest.class);
    public static String userId;
    public static String id;
    private static MeteringService meteringService;
    private static Vertx vertxObj;
    private static String databaseIP;
    private static int databasePort;
    private static String databaseName;
    private static String databaseUserName;
    private static String databasePassword;
    private static int databasePoolSize;
    private static String databaseTableName;
    private static Configuration config;
    static JsonObject dbConfig;

    @BeforeAll
    @DisplayName("Deploying Verticle")
    static void startVertex(Vertx vertx, VertxTestContext vertxTestContext) {
        vertxObj = vertx;
        config = new Configuration();
        dbConfig = config.configLoader(4, vertx);
        databaseIP = dbConfig.getString(DATABASE_IP);
        databasePort = dbConfig.getInteger(DATABASE_PORT);
        databaseName = dbConfig.getString(DATABASE_NAME);
        databaseUserName = dbConfig.getString(DATABASE_USERNAME);
        databasePassword = dbConfig.getString(DATABASE_PASSWORD);
        databaseTableName = dbConfig.getString(DATABASE_TABLE_NAME);
        databasePoolSize = dbConfig.getInteger(POOL_SIZE);
        meteringService = new MeteringServiceImpl(dbConfig, vertxObj, Api.getInstance("/ngsi-ld/v1"));
        userId = UUID.randomUUID().toString();
        id = "89a36273d77dac4cf38114fca1bbe64392547f86";
        vertxTestContext.completeNow();
    }

    private JsonObject readConsumerRequest() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
        jsonObject.put(RESOURCE_ID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-test-alias/");
        jsonObject.put(START_TIME, "2022-05-29T05:30:00+05:30[Asia/Kolkata]");
        jsonObject.put(END_TIME, "2022-06-04T02:00:00+05:30[Asia/Kolkata]");
        jsonObject.put(TIME_RELATION, DURING);
        jsonObject.put(API, "/ngsi-ld/v1/subscription");
        jsonObject.put(ENDPOINT, "/ngsi-ld/v1/consumer/audit");

        return jsonObject;
    }

    private JsonObject readProviderRequest() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
        jsonObject.put(RESOURCE_ID,
                "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR055");
        jsonObject.put(START_TIME, "2022-03-20T05:30:00+05:30[Asia/Kolkata]");
        jsonObject.put(END_TIME, "2022-03-30T02:00:00+05:30[Asia/Kolkata]");
        jsonObject.put(TIME_RELATION, DURING);
        jsonObject.put(API, "/ngsi-ld/v1/entities");
        jsonObject.put(PROVIDER_ID, "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86");
        jsonObject.put(CONSUMER_ID, "844e251b-574b-46e6-9247-f76f1f70a637");
        jsonObject.put(IID,
                "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR055");
        jsonObject.put(ENDPOINT, "/ngsi-ld/v1/provider/audit");
        return jsonObject;
    }
    private JsonObject read() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put(START_TIME, "2022-06-20T00:00:00Z");
        jsonObject.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
        jsonObject.put(END_TIME, "2022-06-21T16:00:00Z");
        jsonObject.put(TIME_RELATION, "between");
        jsonObject.put(API, "/ngsi-ld/v1/subscription");
        jsonObject.put(PROVIDER_ID, "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86");
        jsonObject.put(ENDPOINT, "/ngsi-ld/v1/provider/audit");
        jsonObject.put(IID, "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR055");
        return jsonObject;
    }

    @Test
    @DisplayName("Testing read query with given Time Interval")
    void readFromValidTimeInterval(VertxTestContext vertxTestContext) {
        JsonObject responseJson= new JsonObject().put(SUCCESS,"successful operations");
        AsyncResult<JsonObject> asyncResult= mock(AsyncResult.class);
        MeteringServiceImpl.postgresService =mock(DatabaseService.class);
        JsonObject json= mock(JsonObject.class);
        JsonArray jsonArray= mock(JsonArray.class);

        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json,responseJson);
        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(39);

        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(MeteringServiceImpl.postgresService).executeQuery(any(), any());

        JsonObject request = read();
        request.put(RESOURCE_ID, "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR055");
        request.put(CONSUMER_ID,"125-fc132-23rwsd");
        meteringService.executeReadQuery(
                request,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            LOGGER.info(response);
                                            assertEquals(SUCCESS, response.getString(SUCCESS));
                                            vertxTestContext.completeNow();
                                        })));
    }
    @Test
    @DisplayName("Testing read query with given Time Interval")
    void readFromValidTimeIntervalConsumer(VertxTestContext vertxTestContext) {
        JsonObject responseJson= new JsonObject().put(SUCCESS,"successful operations");
        AsyncResult<JsonObject> asyncResult= mock(AsyncResult.class);
        MeteringServiceImpl.postgresService =mock(DatabaseService.class);
        JsonObject json= mock(JsonObject.class);
        JsonArray jsonArray= mock(JsonArray.class);

        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json,responseJson);
        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(39);

        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(MeteringServiceImpl.postgresService).executeQuery(any(), any());

        JsonObject request = read();
        request.remove(PROVIDER_ID);
        request.put(RESOURCE_ID, "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR055");
        meteringService.executeReadQuery(
                request,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            LOGGER.info(response);
                                            assertEquals(SUCCESS, response.getString(SUCCESS));
                                            vertxTestContext.completeNow();
                                        })));
    }
    @Test
    @DisplayName("Testing read query for given time,api and id.")
    void readForGivenTimeApiIdConsumerProviderIDZero(VertxTestContext vertxTestContext) {
        JsonObject responseJson= new JsonObject().put(SUCCESS,"Success");
        AsyncResult<JsonObject> asyncResult= mock(AsyncResult.class);
        MeteringServiceImpl.postgresService =mock(DatabaseService.class);
        JsonObject json= mock(JsonObject.class);
        JsonArray jsonArray= mock(JsonArray.class);


        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json,responseJson);

        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(0);

        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(MeteringServiceImpl.postgresService).executeQuery(any(), any());
        JsonObject jsonObject = readProviderRequest();

        meteringService.executeReadQuery(
                jsonObject,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(SUCCESS, response.getString("title"));
                                            vertxTestContext.completeNow();
                                        })));

    }
    @Test
    @DisplayName("Testing read query for missing userId")
    void readForMissingUserId(VertxTestContext vertxTestContext) {
        JsonObject request = readConsumerRequest();
        request.remove(USER_ID);


        meteringService.executeReadQuery(
                request,
                vertxTestContext.failing(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(
                                                    USERID_NOT_FOUND,
                                                    new JsonObject(response.getMessage()).getString(DETAIL));
                                            vertxTestContext.completeNow();
                                        })));

    }
    @Test
    @DisplayName("Testing read query for missing time Relation")
    void readForMissingTimeRel(VertxTestContext vertxTestContext) {
        JsonObject request = readConsumerRequest();
        request.remove(TIME_RELATION);

        meteringService.executeReadQuery(
                request,
                vertxTestContext.failing(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(
                                                    TIME_RELATION_NOT_FOUND,
                                                    new JsonObject(response.getMessage()).getString(DETAIL));
                                            vertxTestContext.completeNow();
                                        })));

    }

    @Test
    @DisplayName("Testing read query for missing time")
    void readForMissingTime(VertxTestContext vertxTestContext) {
        JsonObject request = readConsumerRequest();
        request.remove(START_TIME);

        meteringService.executeReadQuery(
                request,
                vertxTestContext.failing(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(
                                                    TIME_NOT_FOUND, new JsonObject(response.getMessage()).getString(DETAIL));
                                            vertxTestContext.completeNow();
                                        })));
    }

    @Test
    @DisplayName("Testing read query with invalid start/end time")
    void readForInvalidStartTime(VertxTestContext vertxTestContext) {
        JsonObject request = readConsumerRequest();
        request.put(START_TIME, "2021-009-18T00:30:00+05:30[Asia/Kolkata]");

        meteringService.executeReadQuery(
                request,
                vertxTestContext.failing(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(
                                                    INVALID_DATE_TIME,
                                                    new JsonObject(response.getMessage()).getString(DETAIL));
                                            vertxTestContext.completeNow();
                                        })));

    }

    @Test
    @DisplayName("Testing read query with invalid providerId.")
    void readForInvalidProviderId(VertxTestContext vertxTestContext) {
        JsonObject request = readProviderRequest();
        request.put(PROVIDER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-tsst-alias");

        meteringService.executeReadQuery(
                request,
                vertxTestContext.failing(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(
                                                    INVALID_PROVIDER_ID,
                                                    new JsonObject(response.getMessage()).getString(DETAIL));
                                            vertxTestContext.completeNow();
                                        })));

    }

    @Test
    @DisplayName("Testing count query for given time.")
    void readCountForGivenTime(VertxTestContext vertxTestContext) {
        AsyncResult<JsonObject> asyncResult= mock(AsyncResult.class);
        MeteringServiceImpl.postgresService = mock(DatabaseService.class);
        JsonObject json= mock(JsonObject.class);
        JsonArray jsonArray= mock(JsonArray.class);

        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json);

        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(300);

        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(MeteringServiceImpl.postgresService).executeQuery(any(),any());

        JsonObject jsonObject = read();
        jsonObject.put("options","count");
        jsonObject.remove(API);
        meteringService.executeReadQuery(
                jsonObject,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(SUCCESS, response.getString("title"));
                                            vertxTestContext.completeNow();
                                        })));
    }
    @Test
    @DisplayName("Testing count query for given time for Consumer")
    void readCountForGivenTimeConsumer(VertxTestContext vertxTestContext) {
        AsyncResult<JsonObject> asyncResult= mock(AsyncResult.class);
        MeteringServiceImpl.postgresService = mock(DatabaseService.class);
        JsonObject json= mock(JsonObject.class);
        JsonArray jsonArray= mock(JsonArray.class);

        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json);
        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(300);

        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(MeteringServiceImpl.postgresService).executeQuery(any(),any());

        JsonObject jsonObject = read();
        jsonObject.put("options","count");
        jsonObject.remove(PROVIDER_ID);
        jsonObject.put(RESOURCE_ID, "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR055");
        meteringService.executeReadQuery(
                jsonObject,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(SUCCESS, response.getString("title"));
                                            vertxTestContext.completeNow();
                                        })));
    }
    @Test
    @DisplayName("Testing count query for given time.")
    void readCountForGivenTimeForZero(VertxTestContext vertxTestContext) {
        AsyncResult<JsonObject> asyncResult= mock(AsyncResult.class);
        MeteringServiceImpl.postgresService = mock(DatabaseService.class);
        JsonObject json= mock(JsonObject.class);
        JsonArray jsonArray= mock(JsonArray.class);

        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json);
        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(0);

        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(MeteringServiceImpl.postgresService).executeQuery(any(), any());
        JsonObject jsonObject = read();
        jsonObject.put("options","count");
        jsonObject.remove(API);
        meteringService.executeReadQuery(
                jsonObject,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(SUCCESS, response.getString("title"));
                                            vertxTestContext.completeNow();
                                        })));
    }

    @Test
    @DisplayName("Testing Write Query Successful")
    void writeDataSuccessful(VertxTestContext vertxTestContext) {
        JsonObject request = new JsonObject();
        ZonedDateTime zst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        long time = zst.toInstant().toEpochMilli();
        String isoTime = zst.truncatedTo(ChronoUnit.SECONDS).toString();
        request.put(EPOCH_TIME,time);
        request.put(ISO_TIME,isoTime);
        request.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
        request.put(ID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-test-alias/");
        request.put(API, "/ngsi-ld/v1/subscription");
        request.put(RESPONSE_SIZE,12);
        DatabaseService postgresService = mock(DatabaseService.class);

        AsyncResult<JsonObject> asyncResult =mock(AsyncResult.class);
        MeteringServiceImpl.rmqService = mock(DatabrokerService.class);

        when(asyncResult.succeeded()).thenReturn(true);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(3)).handle(asyncResult);
                return null;
            }
        }).when(MeteringServiceImpl.rmqService).publishMessage(any(),anyString(),anyString(),any());

        meteringService.insertMeteringValuesInRMQ(
                request,handler->{
                    if (handler.succeeded()){
                        vertxTestContext.completeNow();
                    }else {
                        vertxTestContext.failNow("Failed");
                    }
                });
    }
    @Test
    @DisplayName("Testing Write Query Failure")
    void writeDataFailure(VertxTestContext vertxTestContext) {
        JsonObject request = new JsonObject();
        ZonedDateTime zst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        long time = zst.toInstant().toEpochMilli();
        String isoTime = zst.truncatedTo(ChronoUnit.SECONDS).toString();
        request.put(EPOCH_TIME,time);
        request.put(ISO_TIME,isoTime);
        request.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
        request.put(ID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-test-alias/");
        request.put(API, "/ngsi-ld/v1/subscription");
        request.put(RESPONSE_SIZE,12);

        RabbitMQClient rabbitMQClient = mock(RabbitMQClient.class);
        AsyncResult<JsonObject> asyncResult =mock(AsyncResult.class);

        meteringService.insertMeteringValuesInRMQ(
                request,handler->{
                    if (handler.failed()){
                        vertxTestContext.completeNow();
                    }else {
                        vertxTestContext.failNow("Failed");
                    }
                });
    }

}