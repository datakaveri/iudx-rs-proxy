package iudx.rs.proxy.apiserver.validation;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.rs.proxy.apiserver.util.RequestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
class ValidatorsHandlersFactoryTest {

    ValidatorsHandlersFactory validatorsHandlersFactory;
    Map<String, String> jsonSchemaMap = new HashMap<>();
    @Mock
    Vertx vertx;


    @BeforeEach
    public void setUp(){
        validatorsHandlersFactory = new ValidatorsHandlersFactory();
    }

    @Test
    @DisplayName("getAdminCrudPathDeleteValidations Test")
    public void getAdminCrudPathDeleteValidationsTest(VertxTestContext vertxTestContext){
        MultiMap params = MultiMap.caseInsensitiveMultiMap();
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        JsonObject jsonObject = mock(JsonObject.class);

        var validator =validatorsHandlersFactory.build(vertx,RequestType.ENTITY,params,headers,jsonObject);

        assertEquals(13,validator.size());
        vertxTestContext.completeNow();
    }

  @Test
  @DisplayName("getPostEntityQueryValidations Test")
  public void getPostEntityQueryValidationsTest(VertxTestContext vertxTestContext){
      MultiMap params = MultiMap.caseInsensitiveMultiMap();
      MultiMap headers = MultiMap.caseInsensitiveMultiMap();
      JsonObject jsonObject = mock(JsonObject.class);

      var validator =validatorsHandlersFactory.build(vertx, RequestType.POST_ENTITIES,params,headers,jsonObject);

      assertEquals(3,validator.size());
      vertxTestContext.completeNow();
  }
    @Test
    @DisplayName("getPostTemporalPathValidations Test")
    public void getPostTemporalPathValidationsTest(VertxTestContext vertxTestContext){
        MultiMap params = MultiMap.caseInsensitiveMultiMap();
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        JsonObject jsonObject = mock(JsonObject.class);

        var validator =validatorsHandlersFactory.build(vertx, RequestType.POST_TEMPORAL,params,headers,jsonObject);
        assertEquals(3,validator.size());
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("getTemporalQueryValidations Test")
    public void getTemporalValidationsTest(VertxTestContext vertxTestContext){
        MultiMap params = MultiMap.caseInsensitiveMultiMap();
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        JsonObject jsonObject = mock(JsonObject.class);

        var validator =validatorsHandlersFactory.build(vertx, RequestType.TEMPORAL,params,headers,jsonObject);

        assertEquals(14,validator.size());
        vertxTestContext.completeNow();
    }

}