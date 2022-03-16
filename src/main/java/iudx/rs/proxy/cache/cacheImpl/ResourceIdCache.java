package iudx.rs.proxy.cache.cacheImpl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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

public class ResourceIdCache implements IudxCache {

  private static final Logger LOGGER = LogManager.getLogger(ResourceGroupCache.class);
  private static final CacheType cacheType = CacheType.RESOURCE_ID;
  private final Cache<String, String> cache =
      CacheBuilder.newBuilder().maximumSize(1000).expireAfterWrite(30, TimeUnit.MINUTES).build();
  private final JsonObject config;
  private final WebClient catWebClient ;
  int port;
  String host;
  String searchPath = Constants.CAT_RSG_PATH;
  String resourceGroupPath = Constants.CAT_RESOURCE_GROUP_PATH;

  public ResourceIdCache(Vertx vertx, WebClient webClient, JsonObject config) {
    this.config = config;
    this.catWebClient=webClient;
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
    LOGGER.info("THIS CACHE ID "+ this.cache.asMap());
      String key=entries.getString("key");
      String resourceId = cache.getIfPresent(key);
      if (resourceId != null) return resourceId;
      return null;
    }


  @Override
  public JsonObject updateCache(JsonObject request) {

    int port = config.getInteger("catServerPort");
    String host = config.getString("catServerHost");
    String path = Constants.CAT_RSG_PATH;
    String resourceId = request.getString("key");
    String groupACL = request.getString("groupACL");
    JsonObject result = new JsonObject();

    catWebClient
        .get(port, host, path)
        .addQueryParam("property", "[id]")
        .addQueryParam("value", "[[" + resourceId + "]]")
        .addQueryParam("filter", "[accessPolicy]")
        .expect(ResponsePredicate.JSON)
        .send(
            httpResponseAsyncResult -> {
              if (httpResponseAsyncResult.failed()) {
                result.put("error", false);
              }
              HttpResponse<Buffer> response = httpResponseAsyncResult.result();
              JsonObject responseBody = response.bodyAsJsonObject();
              if (response.statusCode() != HttpStatus.SC_OK) {
                result.put("error", false);
              } else if (!responseBody.getString("type").equals("urn:dx:cat:Success")) {
                result.put("error", false);
              } else if (responseBody.getInteger("totalHits") == 0) {
                LOGGER.error("Info: Resource ID invalid : Catalogue item Not Found");
                result.put("error", false);
              } else {
                LOGGER.info("is Exist response : " + responseBody);
                LOGGER.info("resourceId "+resourceId+" groupACL "+groupACL);
                cache.put(resourceId, groupACL);
                result.put("result", true);
              }
            });
    return result;
  }

  @Override
  public void refreshCache() { cache.invalidateAll();
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
  }

  private void populateCache(String groupId) {
    catWebClient
        .get(port, host, searchPath)
        .addQueryParam("property", "[id]")
        .addQueryParam("value", "[[" + groupId + "]]")
        .addQueryParam("filter", "[id,accessPolicy]")

        .expect(ResponsePredicate.JSON)
        .send(
            populateCacheHandler -> {

              HttpResponse<Buffer> response = populateCacheHandler.result();
              JsonObject responseBody = response.bodyAsJsonObject();
              String resourceACL =
                  responseBody.getJsonArray("results").getJsonObject(0).getString("accessPolicy");
              String resourceId =responseBody.getJsonArray("results").getJsonObject(0).getString("id");
              if (resourceACL != null) {
                this.cache.put(resourceId, resourceACL);
                populateCacheHandler.succeeded();
              }
            });
  }

}
