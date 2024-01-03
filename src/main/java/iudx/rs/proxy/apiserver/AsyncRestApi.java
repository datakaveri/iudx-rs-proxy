package iudx.rs.proxy.apiserver;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.rs.proxy.apiserver.handlers.AuthHandler;
import iudx.rs.proxy.apiserver.handlers.FailureHandler;
import iudx.rs.proxy.apiserver.handlers.TokenDecodeHandler;
import iudx.rs.proxy.apiserver.handlers.ValidationHandler;
import iudx.rs.proxy.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.rs.proxy.apiserver.util.ApiServerConstants.SEARCH;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.STATUS;
import static iudx.rs.proxy.apiserver.util.RequestType.ASYNC_SEARCH;
import static iudx.rs.proxy.apiserver.util.RequestType.ASYNC_STATUS;


public class AsyncRestApi {
    private static final Logger LOGGER = LogManager.getLogger(AsyncRestApi.class);

    private final Vertx vertx;
    private final Router router;
    //private final ParamsValidator validator;
/*    private final CatalogueService catalogueService;
    private final PostgresService postgresService;
    private final DataBrokerService databroker;
    private final CacheService cacheService;
    private AsyncService asyncService;
    private EncryptionService encryptionService;*/
    private Api api;


    AsyncRestApi(Vertx vertx, Router router, Api api) {
        this.vertx = vertx;
        this.router = router;
      /*  this.databroker = DataBrokerService.createProxy(vertx, BROKER_SERVICE_ADDRESS);
        this.cacheService = CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);
        this.catalogueService = new CatalogueService(cacheService);
        this.validator = new ParamsValidator(catalogueService);
        this.postgresService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);
        this.encryptionService = EncryptionService.createProxy(vertx, ENCRYPTION_SERVICE_ADDRESS);*/
        this.api = api;
    }

    Router init() {
        FailureHandler validationsFailureHandler = new FailureHandler();

        //asyncService = AsyncService.createProxy(vertx, ASYNC_SERVICE_ADDRESS);

        ValidationHandler asyncSearchValidation = new ValidationHandler(vertx, ASYNC_SEARCH);
        router
                .get(SEARCH)
              //  .handler(asyncSearchValidation)
                .handler(TokenDecodeHandler.create(vertx))
                // todo call consent loger
                .handler(AuthHandler.create(vertx,api))
                .handler(this::handleAsyncSearchRequest)
                .failureHandler(validationsFailureHandler);

        ValidationHandler asyncStatusValidation = new ValidationHandler(vertx, ASYNC_STATUS);
        router
                .get(STATUS)
               // .handler(asyncStatusValidation)
                .handler(TokenDecodeHandler.create(vertx))
                // todo call consent loger
                .handler(AuthHandler.create(vertx,api))
                .handler(this::handleAsyncStatusRequest)
                .failureHandler(validationsFailureHandler);

        return this.router;
    }
    private void handleAsyncSearchRequest(RoutingContext routingContext) {
        LOGGER.trace("handleAsyncSearchRequest started");

    }
    private void handleAsyncStatusRequest(RoutingContext routingContext) {
        LOGGER.trace("handleAsyncStatusRequest started");

    }
}
