package iudx.rs.proxy.apiserver.query;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static iudx.rs.proxy.apiserver.util.ApiServerConstants.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class QueryMapperTest {


    private QueryMapper qm;

    @BeforeEach
    public void setup(Vertx vertx, VertxTestContext testContext) {
        qm = new QueryMapper();
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

    /*@Test
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
*/



}