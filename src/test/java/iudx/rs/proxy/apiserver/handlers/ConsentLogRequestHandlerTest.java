package iudx.rs.proxy.apiserver.handlers;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.rs.proxy.optional.consentlogs.ConsentLoggingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
class ConsentLogRequestHandlerTest {

    @Mock
    private ConsentLoggingService consentLoggingService;

    @Mock
    private RoutingContext routingContext;

    @Mock
    private HttpServerRequest httpServerRequest;

    private ConsentLogRequestHandler consentLogRequestHandler;

    @BeforeEach
    void setUp(VertxTestContext vertxTestContext) {
        MockitoAnnotations.openMocks(this);
        Vertx vertx1 = Vertx.vertx();
        consentLogRequestHandler = new ConsentLogRequestHandler(vertx1, true);
        consentLogRequestHandler.consentLoggingService = consentLoggingService;
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("test handle method for ConsentLogRequestHandler for Adex")
    void testHandle(VertxTestContext vertxTestContext) {
        when(consentLoggingService.log(any(), any())).thenReturn(Future.succeededFuture(new JsonObject()));
        consentLogRequestHandler.handle(routingContext);
        vertxTestContext.completeNow();
        verify(routingContext, times(2)).next();
    }

}