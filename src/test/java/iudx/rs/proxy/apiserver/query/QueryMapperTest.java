package iudx.rs.proxy.apiserver.query;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.stream.Stream;

import static iudx.rs.proxy.apiserver.util.ApiServerConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
class QueryMapperTest {

    private static final Logger LOGGER = LogManager.getLogger(QueryMapperTest.class);
    private QueryMapper qm;
    @Mock
    RoutingContext context;
    boolean isTimeLimitEnabled = true;

    @BeforeEach
    public void setup(Vertx vertx, VertxTestContext testContext) {
        qm = new QueryMapper(context, isTimeLimitEnabled);
        testContext.completeNow();
    }

    @Test
    public void testGetQueryTerms(Vertx vertx, VertxTestContext testContext) {
        String q = "speed>=300";
        JsonObject json = qm.getQueryTerms(q);
        assertEquals("speed", json.getString(JSON_ATTRIBUTE));
        assertEquals(">=", json.getString(JSON_OPERATOR));
        assertEquals("300", json.getString(JSON_VALUE));
        testContext.completeNow();

    }

@Test
public void testToJson(Vertx vertx, VertxTestContext testContext) {
    MultiMap map = MultiMap.caseInsensitiveMultiMap();
    map.add(NGSILDQUERY_ID, "id1");
    map.add(NGSILDQUERY_ATTRIBUTE, "attr1");
    NGSILDQueryParams params = new NGSILDQueryParams(map);

    JsonObject json = qm.toJson(params, false);

    assertTrue(json.containsKey(NGSILDQUERY_ID));
    assertTrue(json.containsKey(NGSILDQUERY_ATTRIBUTE));
    assertTrue(json.getJsonArray(NGSILDQUERY_ID) instanceof JsonArray);
    assertTrue(json.getJsonArray(NGSILDQUERY_ATTRIBUTE) instanceof JsonArray);
    testContext.completeNow();
}
    @Test
    @Description("QueryMapper test for Circle geo-query")
    public void testToJson4CircleQuery(Vertx vertx, VertxTestContext testContext) {
        // georel=near;maxDistance==360&geometry=Point&coordinates=%5B8.684783577919006%2C49.406131991436396%5D
        MultiMap map = MultiMap.caseInsensitiveMultiMap();
        map.add(NGSILDQUERY_ID, "id1");
        map.add(NGSILDQUERY_ATTRIBUTE, "attr1");
        map.add(NGSILDQUERY_GEOREL, "near;maxdistance=360");
        map.add(NGSILDQUERY_GEOMETRY, "point");
        map.add(NGSILDQUERY_COORDINATES, "[8.6846,49.40606]");
        map.add(NGSILDQUERY_GEOPROPERTY, "location");
        map.add("attrs", "id");
        NGSILDQueryParams params = new NGSILDQueryParams(map);

        JsonObject json = qm.toJson(params, false);
        assertTrue(json.containsKey("geo-query"));
        JsonObject geoJson=json.getJsonObject("geo-query");
        assertTrue(geoJson.containsKey(JSON_LAT));
        assertTrue(geoJson.containsKey(JSON_LON));
        assertTrue(geoJson.containsKey(JSON_RADIUS));
        assertFalse(geoJson.containsKey(NGSILDQUERY_TIMEREL));
        assertFalse(geoJson.containsKey(NGSILDQUERY_TIME));
        assertFalse(geoJson.containsKey(NGSILDQUERY_ENDTIME));
        testContext.completeNow();
    }

    @Test
    @Description("QueryMapper test for geo-query")
    public void testToJson4GeoQuery(Vertx vertx, VertxTestContext testContext) {
        MultiMap map = MultiMap.caseInsensitiveMultiMap();
        map.add(NGSILDQUERY_ID, "id1,id2");
        map.add(NGSILDQUERY_ATTRIBUTE, "attr1,attr2");
        map.add(NGSILDQUERY_GEOREL, "within");
        map.add(NGSILDQUERY_GEOMETRY, "Polygon");
        map.add(NGSILDQUERY_COORDINATES,
                "[[[8.68462,49.40606],[8.68550,49.40622],[8.68545,49.406344],[8.68457,49.40617],[8.6846,49.4060]]]");
        map.add(NGSILDQUERY_GEOPROPERTY, "location");
        NGSILDQueryParams params = new NGSILDQueryParams(map);

        JsonObject json = qm.toJson(params, false);
        assertTrue(json.containsKey("geo-query"));
        JsonObject geoJson=json.getJsonObject("geo-query");
        assertTrue(json.containsKey(NGSILDQUERY_ID));
        assertTrue(json.containsKey(NGSILDQUERY_ATTRIBUTE));
        assertTrue(geoJson.containsKey(NGSILDQUERY_COORDINATES));
        assertTrue(geoJson.containsKey(NGSILDQUERY_GEOREL));
        assertTrue(geoJson.containsKey(NGSILDQUERY_GEOMETRY));
        assertFalse(json.containsKey(NGSILDQUERY_TIMEREL));
        assertFalse(json.containsKey(NGSILDQUERY_TIME));
        assertFalse(json.containsKey(NGSILDQUERY_ENDTIME));
        testContext.completeNow();
    }
  @Test
  @Description("QueryMapper test for temporal queries(during)")
  public void testToJson4TemporalDuringQuery(Vertx vertx, VertxTestContext testContext) {
      MultiMap map = MultiMap.caseInsensitiveMultiMap();
      map.add(NGSILDQUERY_ID, "id1,id2");
      map.add(NGSILDQUERY_ATTRIBUTE, "attr1,attr2");
      map.add(NGSILDQUERY_TIMEREL, "during");
      map.add(NGSILDQUERY_TIME, "2020-01-23T14:20:00Z");
      map.add(NGSILDQUERY_ENDTIME, "2020-01-24T14:40:00Z");
      NGSILDQueryParams params = new NGSILDQueryParams(map);

      JsonObject json = qm.toJson(params, true);
      assertTrue(json.containsKey("temporal-query"));
      JsonObject temporalQuery=json.getJsonObject("temporal-query");

      assertTrue(json.containsKey(NGSILDQUERY_ID));
      assertTrue(json.containsKey(NGSILDQUERY_ATTRIBUTE));
      assertTrue(temporalQuery.containsKey(NGSILDQUERY_TIMEREL));
      assertTrue(temporalQuery.containsKey(NGSILDQUERY_TIME));
      assertTrue(temporalQuery.containsKey(NGSILDQUERY_ENDTIME));
      testContext.completeNow();
  }

    @Test
    @Description("QueryMapper test for temporal queries(before)")
    public void testToJson4TemporalQuery(Vertx vertx, VertxTestContext testContext) {
        MultiMap map = MultiMap.caseInsensitiveMultiMap();
        map.add(NGSILDQUERY_ID, "id1,id2");
        map.add(NGSILDQUERY_ATTRIBUTE, "attr1,attr2");
        map.add(NGSILDQUERY_TIMEREL, "before");
        map.add(NGSILDQUERY_TIME, "2020-01-23T14:20:00Z");
        map.add(NGSILDQUERY_TIMEPROPERTY, "observationTimeRel");
        NGSILDQueryParams params = new NGSILDQueryParams(map);

        JsonObject json = qm.toJson(params, true);
        assertTrue(json.containsKey("temporal-query"));
        JsonObject temporalQuery=json.getJsonObject("temporal-query");
        assertTrue(json.containsKey(NGSILDQUERY_ID));
        assertTrue(json.containsKey(NGSILDQUERY_ATTRIBUTE));
        assertTrue(temporalQuery.containsKey(NGSILDQUERY_TIMEREL));
        assertTrue(temporalQuery.containsKey(NGSILDQUERY_TIME));
        testContext.completeNow();
    }

    @Test
    @Description("QueryMapper test for temporal queries(Invalid time format)")
    public void testToJson4TemporalInvalidTimeQuery(Vertx vertx, VertxTestContext testContext) {
        MultiMap map = MultiMap.caseInsensitiveMultiMap();
        map.add(NGSILDQUERY_ID, "id1,id2");
        map.add(NGSILDQUERY_ATTRIBUTE, "attr1,attr2");
        map.add(NGSILDQUERY_TIMEREL, "during");
        map.add(NGSILDQUERY_TIME, "2020-01-23X14:20:00Z");
        map.add(NGSILDQUERY_ENDTIME, "2020-01-24T14:40:00Z");
        map.add(NGSILDQUERY_TIMEPROPERTY, "observationTimeRel");
        NGSILDQueryParams params = new NGSILDQueryParams(map);

        qm.toJson(params,true);
        verify(context,atLeast(1)).fail(anyInt(),any());
        testContext.completeNow();
    }

    @Test
    @Description("QueryMapper test for simple attribute query")
    public void testToJson4SimpleAttributeQuery(Vertx vertx, VertxTestContext testContext) {
        MultiMap map = MultiMap.caseInsensitiveMultiMap();
        map.add(NGSILDQUERY_ID, "id1,id2");
        map.add(NGSILDQUERY_ATTRIBUTE, "attr1,attr2");
        map.add(NGSLILDQUERY_Q, "speed>=300;temperature==35");
        NGSILDQueryParams params = new NGSILDQueryParams(map);

        JsonObject json = qm.toJson(params, false);

        assertTrue(json.containsKey(NGSILDQUERY_ID));
        assertTrue(json.containsKey(NGSILDQUERY_ATTRIBUTE));
        assertTrue(json.containsKey(JSON_ATTR_QUERY));
        assertTrue(json.getString(JSON_ATTR_QUERY) != null);
        testContext.completeNow();
    }

    static Stream<Arguments> invalidQTermsValues() {
        // Add any valid value which will pass successfully.
        return Stream.of(
                Arguments.of("refrenceLevel+10", "Operator not allowed."),
                Arguments.of("refrenceLevel/-10", "Operator not allowed."),
                Arguments.of("refrenceLevel<>10", "Operator not allowed."),
                Arguments.of("refrenceLevel><10", "Operator not allowed."),
                Arguments.of("refrenceLevel>+10", "Operator not allowed."),
                Arguments.of("refrenceLevel+<10", "Operator not allowed."));

    }

    @ParameterizedTest
    @MethodSource("invalidQTermsValues")
    @Description("coordinates type parameter invalid values.")
    public void testInvalidQTermValue(String value, String result, Vertx vertx,
                                      VertxTestContext testContext) {
      /*  RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            qm.getQueryTerms(value);
        });*/
        qm.getQueryTerms(value);
        verify(context,atLeast(1)).fail(anyInt(),any());
        testContext.completeNow();
    }

    @Test
    @Description("test temporal between without end time")
    public void testInvalidTemporalQuery(Vertx vertx, VertxTestContext testContext) {
        MultiMap map = MultiMap.caseInsensitiveMultiMap();
        map.add(NGSILDQUERY_ID, "id1,id2");
        map.add(NGSILDQUERY_ATTRIBUTE, "attr1,attr2");
        map.add(NGSILDQUERY_TIMEREL, "during");
        map.add(NGSILDQUERY_TIME, "2020-01-23T14:20:00Z");
        NGSILDQueryParams params = new NGSILDQueryParams(map);

        qm.toJson(params,true);
        verify(context,atLeast(1)).fail(anyInt(),any());
        testContext.completeNow();
    }

    @Test
    @Description("test temporal interval for between")
    public void testInvalidTemporalIntervalQuery(Vertx vertx, VertxTestContext testContext) {
        MultiMap map = MultiMap.caseInsensitiveMultiMap();
        map.add(NGSILDQUERY_ID, "id1,id2");
        map.add(NGSILDQUERY_ATTRIBUTE, "attr1,attr2");
        map.add(NGSILDQUERY_TIMEREL, "during");
        map.add(NGSILDQUERY_TIME, "2020-01-13T14:20:00Z");
        map.add(NGSILDQUERY_ENDTIME, "2020-01-30T14:40:00Z");
        NGSILDQueryParams params = new NGSILDQueryParams(map);

        qm.toJson(params,true);
        verify(context,atLeast(1)).fail(anyInt(),any());
        testContext.completeNow();
    }

   @Test
    @Description("QueryMapper test for invalid geo-query")
    public void testIncompleteGeoQuery(Vertx vertx, VertxTestContext testContext) {
        // georel=near;maxDistance==360&geometry=Point&coordinates=%5B8.684783577919006%2C49.406131991436396%5D
        MultiMap map = MultiMap.caseInsensitiveMultiMap();
        map.add(NGSILDQUERY_ID, "id1");
        map.add(NGSILDQUERY_ATTRIBUTE, "attr1");
        map.add(NGSILDQUERY_GEOREL, "near;maxdistance=360");
        map.add(NGSILDQUERY_GEOMETRY, "point");
        map.add(NGSILDQUERY_COORDINATES, "[8.6846,49.40606]");

        NGSILDQueryParams params = new NGSILDQueryParams(map);

       qm.toJson(params,false);
       verify(context,atLeast(1)).fail(anyInt(),any());
       testContext.completeNow();
    }
    @AfterEach
    public void teardown() {

    }
}