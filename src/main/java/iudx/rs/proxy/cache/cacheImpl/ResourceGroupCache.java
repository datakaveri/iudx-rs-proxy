package iudx.rs.proxy.cache.cacheImpl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import iudx.rs.proxy.authenticator.Constants;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpStatus;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;


public class ResourceGroupCache implements IudxCache {

  private static final Logger LOGGER = LogManager.getLogger(ResourceGroupCache.class);
  private static final CacheType cacheType = CacheType.RESOURCE_GROUP;
  private Cache<String, String> cache =
      CacheBuilder.newBuilder().maximumSize(5000).expireAfterWrite(1L, TimeUnit.DAYS).build();
  private final WebClient catWebClient;

  private final JsonObject config;
  int port;
  String host;
  String searchPath = Constants.CAT_RSG_PATH;
  String resourceGroupPath = Constants.CAT_RESOURCE_GROUP_PATH;

  public ResourceGroupCache(Vertx vertx, WebClient webClient, JsonObject config) {
    this.config = config;
    this.catWebClient = webClient;
    this.port = config.getInteger("catServerPort");
    this.host = config.getString("catServerHost");
    refreshCache();
    vertx.setPeriodic(
        TimeUnit.HOURS.toMillis(1),
        handler -> {
          refreshCache();
        });


  }

  @Override
  public void put(String key, String value) {
    cache.put(key, value);
  }

  @Override
  public String get(JsonObject entries) {
    LOGGER.info("THIS CACHE GROUP "+ this.cache.asMap());
    String groupId = entries.getString("key");
    String groupAccessPolicy = cache.getIfPresent(groupId);
    if (groupAccessPolicy != null) return groupAccessPolicy;
    else {
      // vertx.blocking
      Future<String> getGroup = getGroupAccessPolicy(groupId);
      JsonObject jsonObject = new JsonObject();
      getGroup.onComplete(
          getGroupResult -> {
            if (getGroupResult.succeeded()) {

              String ans = getGroupResult.result();
              jsonObject.put("resourceACL ", ans);
            }
          });

      return null;
    }
  }

  @Override
  public JsonObject updateCache(JsonObject request) {

    String groupId = request.getString("key");

    Future<String> getGroup = getGroupAccessPolicy(groupId);
    JsonObject jsonObject = new JsonObject();
    getGroup.onComplete(
        getGroupResult -> {
          if (getGroupResult.succeeded()) {
            String ans = getGroupResult.result();
            jsonObject.put("resourceACL ", ans);
          }
        });
    return jsonObject;
  }

  private Future<String> getGroupAccessPolicy(String groupId) {
    LOGGER.info("getGroupAccessPolicy Resource group started()");
    JsonObject result = new JsonObject();
    Promise<String> promise = Promise.promise();
    catWebClient
        .get(port, host, searchPath)
        .addQueryParam("property", "[id]")
        .addQueryParam("value", "[[" + groupId + "]]")
        .addQueryParam("filter", "[id,accessPolicy]")
        .expect(ResponsePredicate.JSON)
        .send(
            httpResponseAsyncResult -> {
              if (httpResponseAsyncResult.failed()) {
                LOGGER.info("Failing Cause "+httpResponseAsyncResult.cause());
                result.put("error", "Resource not found");
                promise.fail("Resource Not Found");
              }
              HttpResponse<Buffer> response = httpResponseAsyncResult.result();
              if (response.statusCode() != HttpStatus.SC_OK) {
                result.put("error", "Resource not found");
                promise.fail("Resource Not Found");
              }
              JsonObject responseBody = response.bodyAsJsonObject();
              if (!responseBody.getString("type").equals("urn:dx:cat:Success")) {
                result.put("error", "Resource not found");
                promise.fail("Resource Not Found");
              }
              String resourceACL;
              try {
                resourceACL =
                    responseBody.getJsonArray("results").getJsonObject(0).getString("accessPolicy");
                cache.put(groupId, resourceACL);
                result.put("resourceACL", resourceACL);
                promise.complete(resourceACL);
              } catch (Exception ignored) {
                LOGGER.error(
                    "Info: Group ID invalid : Empty response in results from Catalogue", ignored);
                result.put(
                    "error", "Info: Group ID invalid : Empty response in results from Catalogue");
                promise.fail("Resource Not Found");
              }
            });

    return promise.future();
  }

  @Override
  public void refreshCache() {

    cache.invalidateAll();

    catWebClient
        .get(port, host, resourceGroupPath)
        .expect(ResponsePredicate.JSON)
        .send(
            httpResponseAsyncResult -> {
              JsonArray entries =
                  httpResponseAsyncResult.result().bodyAsJsonObject().getJsonArray("results");
              entries.forEach(
                  json -> {
                    populateCache(json.toString());

                  });
            });
    LOGGER.info("THIS CACHE "+ this.cache.asMap());
  }


  private void populateCache(String groupId) {
    catWebClient
        .get(port, host, searchPath)
        .addQueryParam("property", "[id]")
        .addQueryParam("value", "[[" + groupId + "]]")
        .addQueryParam("filter", "[accessPolicy]")
        .expect(ResponsePredicate.JSON)
        .send(
            populateCacheHandler -> {
              HttpResponse<Buffer> response = populateCacheHandler.result();
              JsonObject responseBody = response.bodyAsJsonObject();
              String resourceACL=responseBody.getJsonArray("results").getJsonObject(0).getString("accessPolicy");
              if(resourceACL!=null) {
                this.cache.put(groupId, resourceACL);
              }
            });
  }
}
