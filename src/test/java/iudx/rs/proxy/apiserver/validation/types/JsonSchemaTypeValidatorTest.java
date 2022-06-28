package iudx.rs.proxy.apiserver.validation.types;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import io.vertx.json.schema.Schema;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
class JsonSchemaTypeValidatorTest {
    @Mock
    Schema schema;
    @Mock
    JsonObject jsonObject;

@BeforeEach
public  void setup(Vertx vertx, VertxTestContext testContext){

    testContext.completeNow();
}
 @Test
    public void testIsvalid(Vertx vertx ){
     JsonSchemaTypeValidator JsonSchemaTypeValidator = new JsonSchemaTypeValidator(jsonObject, schema);
     assertTrue(JsonSchemaTypeValidator.isValid());

 }





}