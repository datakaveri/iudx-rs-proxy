package iudx.rs.proxy.apiserver.util;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.text.Utilities;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class UtilTest {


    private Utilities utilities;

    @BeforeEach
    public void setup(Vertx vertx, VertxTestContext testContext) {
        utilities = new Utilities();
        testContext.completeNow();
    }



}