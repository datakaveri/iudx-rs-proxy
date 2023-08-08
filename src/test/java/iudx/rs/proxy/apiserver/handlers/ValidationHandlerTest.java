package iudx.rs.proxy.apiserver.handlers;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import iudx.rs.proxy.apiserver.exceptions.DxRuntimeException;
import iudx.rs.proxy.apiserver.util.RequestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
class ValidationHandlerTest {
   @Mock
    Vertx vertx;
    @Mock
    RoutingContext event;
    @Mock
    HttpServerRequest request;
    @Mock
    HttpServerResponse response;
    @Mock
    MultiMap headers;
    MultiMap parameters;

    @BeforeEach
    public void setup() {
        Mockito.doReturn(request).when(event).request();
        lenient().doReturn(response).when(event).response();
    }

    @Test
    public void validationHandlerSuccess() {
        parameters = MultiMap.caseInsensitiveMultiMap();
        parameters.set("id", "83c2e5c2-3574-4e11-9530-2b1fbdfce832");

        lenient().doReturn(parameters).when(request).params();
        lenient().doReturn(headers).when(request).headers();

        new ValidationHandler(vertx,RequestType.ENTITY).handle(event);
        Mockito.verify(event, times(1)).next();
    }
    @Test
    public void validationHandlerFailed() {
        parameters = MultiMap.caseInsensitiveMultiMap();
        parameters.set("id", "aaaa/aaaa");

        Mockito.doReturn(parameters).when(request).params();
       lenient().doReturn(headers).when(request).headers();

        ValidationHandler validationsHandler = new ValidationHandler(vertx,RequestType.ENTITY);
        assertThrows(DxRuntimeException.class, () -> validationsHandler.handle(event));
    }


}