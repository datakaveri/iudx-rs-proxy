package iudx.rs.proxy.common;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
class ResponseTest {

    private String type;
    private int status;
    private String title;
    private String detail;
    @Mock
    Throwable failureHandler;


    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext) {
        type = ResponseUrn.MISSING_TOKEN_URN.getUrn();
        status = HttpStatus.SC_NOT_FOUND;
        title = ResponseUrn.MISSING_TOKEN_URN.getMessage();
        detail = failureHandler.getLocalizedMessage();
        vertxTestContext.completeNow();
    }


    @DisplayName("Test withUrn in Builder")
    @Test
   public void testWithUrn(VertxTestContext vertxTestContext) {

        Response response = new Response.Builder()
                .withUrn(type).build();

        JsonObject expected = new JsonObject()
                .put("type", response.getType())
                .put("status", response.getStatus())
                .put("title",response.getTitle())
                .put("detail", response.getDetail());


        String actual = response.toString();
        Assertions.assertEquals(expected.encode(), actual);
        vertxTestContext.completeNow();
    }


}