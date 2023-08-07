package iudx.rs.proxy.apiserver.service;

import static iudx.rs.proxy.apiserver.util.ApiServerConstants.ITEM_TYPES;
import static iudx.rs.proxy.apiserver.util.Util.toList;
import static iudx.rs.proxy.authenticator.Constants.CAT_ITEM_PATH;
import static iudx.rs.proxy.authenticator.Constants.CAT_SEARCH_PATH;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import iudx.rs.proxy.authenticator.Constants;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** catalogue service to fetch calatogue items and groups for the purpose of cache */
public class CatalogueService {

  private static final Logger LOGGER = LogManager.getLogger(CatalogueService.class);

  public static WebClient catWebClient;
  private static String catHost;
  private static int catPort;
  private static Cache<String, JsonObject> catalogueItemCache =
      CacheBuilder.newBuilder()
          .maximumSize(10000)
          .expireAfterAccess(Constants.CACHE_TIMEOUT_AMOUNT, TimeUnit.MINUTES)
          .build();
  ;
  private final Cache<String, List<String>> applicableFilterCache =
      CacheBuilder.newBuilder()
          .maximumSize(1000)
          .expireAfterAccess(Constants.CACHE_TIMEOUT_AMOUNT, TimeUnit.MINUTES)
          .build();
  private long cacheTimerid;
  private String catBasePath;
  private String catItemPath;
  private String catSearchPath;
  private Vertx vertx;

  public CatalogueService(Vertx vertx, JsonObject config) {
    this.vertx = vertx;
    catHost = config.getString("catServerHost");
    catPort = config.getInteger("catServerPort");
    catBasePath = config.getString("dxCatalogueBasePath");
    catItemPath = catBasePath + CAT_ITEM_PATH;
    catSearchPath = catBasePath + CAT_SEARCH_PATH;

    WebClientOptions options =
        new WebClientOptions().setTrustAll(true).setVerifyHost(false).setSsl(true);
    // catWebClient = WebClient.create(vertx, options); // original code
    if (catWebClient == null) {
      catWebClient = WebClient.create(vertx, options);
    }
    populateCache();
    cacheTimerid =
        vertx.setPeriodic(
            TimeUnit.DAYS.toMillis(1),
            handler -> {
              populateCache();
            });
  }

  public static JsonObject getCatalogueItemJson(String id) {
    return catalogueItemCache.getIfPresent(id);
  }

  /**
   * populate
   *
   * @return
   */
  private Future<Boolean> populateCache() {
    Promise<Boolean> promise = Promise.promise();
    catWebClient
        .get(catPort, catHost, catSearchPath)
        .addQueryParam("property", "[itemStatus]")
        .addQueryParam("value", "[[ACTIVE]]")
        .addQueryParam(
            "filter",
            "[id,provider,name,description,authControlGroup,accessPolicy,type,"
                + "iudxResourceAPIs,instance,resourceGroup]")
        .expect(ResponsePredicate.JSON)
        .send(
            handler -> {
              if (handler.succeeded()) {
                JsonArray response = handler.result().bodyAsJsonObject().getJsonArray("results");
                response.forEach(
                    json -> {
                      JsonObject res = (JsonObject) json;
                      String id = res.getString("id");

                      Set<String> type = new HashSet<String>(res.getJsonArray("type").getList());
                      Set<String> itemTypeSet =
                          type.stream().map(e -> e.split(":")[1]).collect(Collectors.toSet());
                      itemTypeSet.retainAll(ITEM_TYPES);

                      res.put("type", itemTypeSet.iterator().next());

                      catalogueItemCache.put(id, res);

                      if (itemTypeSet.contains("resourceGroup")
                          && res.containsKey("iudxResourceAPIs")) {
                        applicableFilterCache.put(
                            id + "/*", toList(res.getJsonArray("iudxResourceAPIs")));
                      } else if (itemTypeSet.contains("Resource")
                          && res.containsKey("iudxResourceAPIs")) {
                        applicableFilterCache.put(id, toList(res.getJsonArray("iudxResourceAPIs")));
                      }
                    });
                promise.complete(true);
              } else if (handler.failed()) {
                promise.fail(handler.cause());
              }
            });
    return promise.future();
  }

  public Future<List<String>> getApplicableFilters(String id) {
    Promise<List<String>> promise = Promise.promise();
    // Note: id should be a complete id not a group id (ex : domain/SHA/rs/rs-group/itemId)
    JsonObject jsonObject = getCatalogueItemJson(id);
    String groupId = null;

    if (jsonObject != null) {
      groupId =
          jsonObject.containsKey("resourceGroup") ? jsonObject.getString("resourceGroup") : id;
    } else {
      LOGGER.debug("failed : id not exists");
      return Future.failedFuture("Not Found");
    }
    // check for item in cache.
    List<String> filters = applicableFilterCache.getIfPresent(id);
    if (filters == null) {
      // check for group if not present by item key.
      filters = applicableFilterCache.getIfPresent(groupId + "/*");
    }
    if (filters == null) {
      // filters = fetchFilters4Item(id, groupId);
      fetchFilters4Item(id, groupId)
          .onComplete(
              handler -> {
                if (handler.succeeded()) {
                  promise.complete(handler.result());
                } else {
                  promise.fail("failed to fetch filters.");
                }
              });
    } else {
      promise.complete(filters);
    }
    return promise.future();
  }

  private Future<List<String>> fetchFilters4Item(String id, String groupId) {
    Promise<List<String>> promise = Promise.promise();
    Future<List<String>> getItemFilters = getFilterFromItemId(id);
    Future<List<String>> getGroupFilters = getFilterFromGroupId(groupId);
    getItemFilters.onComplete(
        itemHandler -> {
          if (itemHandler.succeeded()) {
            List<String> filters4Item = itemHandler.result();
            if (filters4Item.isEmpty()) {
              // Future<List<String>> getGroupFilters = getFilterFromGroupId(groupId);
              getGroupFilters.onComplete(
                  groupHandler -> {
                    if (groupHandler.succeeded()) {
                      List<String> filters4Group = groupHandler.result();
                      applicableFilterCache.put(groupId + "/*", filters4Group);
                      promise.complete(filters4Group);
                    } else {
                      LOGGER.error(
                          "Failed to fetch applicable filters for id: "
                              + id
                              + "or group id : "
                              + groupId);
                    }
                  });
            } else {
              applicableFilterCache.put(id, filters4Item);
              promise.complete(filters4Item);
            }
          } else {

          }
        });
    return promise.future();
  }

  private Future<List<String>> getFilterFromGroupId(String groupId) {
    Promise<List<String>> promise = Promise.promise();
    callCatalogueAPI(
        groupId,
        handler -> {
          if (handler.succeeded()) {
            promise.complete(handler.result());
          } else {
            promise.fail("failed to fetch filters for group");
          }
        });
    return promise.future();
  }

  private Future<List<String>> getFilterFromItemId(String itemId) {
    Promise<List<String>> promise = Promise.promise();
    callCatalogueAPI(
        itemId,
        handler -> {
          if (handler.succeeded()) {
            promise.complete(handler.result());
          } else {
            promise.fail("failed to fetch filters for group");
          }
        });
    return promise.future();
  }

  private void callCatalogueAPI(String id, Handler<AsyncResult<List<String>>> handler) {
    List<String> filters = new ArrayList<String>();
    catWebClient
        .get(catPort, catHost, catItemPath)
        .addQueryParam("id", id)
        .send(
            catHandler -> {
              if (catHandler.succeeded()) {
                JsonArray response = catHandler.result().bodyAsJsonObject().getJsonArray("results");
                response.forEach(
                    json -> {
                      JsonObject res = (JsonObject) json;
                      if (res.containsKey("iudxResourceAPIs")) {
                        filters.addAll(toList(res.getJsonArray("iudxResourceAPIs")));
                      }
                    });
                handler.handle(Future.succeededFuture(filters));
              } else if (catHandler.failed()) {
                LOGGER.error("catalogue call (" + catItemPath + ") failed for id" + id);
                handler.handle(
                    Future.failedFuture("catalogue call(" + catItemPath + ") failed for id" + id));
              }
            });
  }

  public Future<Boolean> isItemExist(String id) {
    LOGGER.trace("isItemExist() started");
    Promise<Boolean> promise = Promise.promise();
    LOGGER.info("id : " + id);
    catWebClient
        .get(catPort, catHost, catItemPath)
        .addQueryParam("id", id)
        .expect(ResponsePredicate.JSON)
        .send(
            responseHandler -> {
              if (responseHandler.succeeded()) {
                HttpResponse<Buffer> response = responseHandler.result();
                JsonObject responseBody = response.bodyAsJsonObject();
                if (responseBody.getString("type").equalsIgnoreCase("urn:dx:cat:Success")
                    && responseBody.getInteger("totalHits") > 0) {
                  promise.complete(true);
                } else {
                  promise.fail(responseHandler.cause());
                }
              } else {
                promise.fail(responseHandler.cause());
              }
            });
    return promise.future();
  }

  public Future<Boolean> isItemExist(List<String> ids) {
    Promise<Boolean> promise = Promise.promise();
    List<Future> futures = new ArrayList<Future>();
    for (String id : ids) {
      futures.add(isItemExist(id));
    }

    CompositeFuture.all(futures)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                promise.complete();
              } else {
                promise.fail(handler.cause());
              }
            });
    return promise.future();
  }
}
