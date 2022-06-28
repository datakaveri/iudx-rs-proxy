package iudx.rs.proxy.database.example.elasticsearch;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
class ElasticsearchServiceImplTest {

    @Mock
    JsonObject config;
    @Mock
    Handler<AsyncResult<JsonObject>> handler;

    @BeforeEach
    public void setUp(Vertx vertx,VertxTestContext testContext){
        config = new JsonObject();
        testContext.completeNow();

    }




}