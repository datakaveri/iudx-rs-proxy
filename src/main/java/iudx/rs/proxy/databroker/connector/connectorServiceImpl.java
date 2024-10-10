package iudx.rs.proxy.databroker.connector;

import static iudx.rs.proxy.apiserver.util.ApiServerConstants.USER_ID;
import static iudx.rs.proxy.databroker.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.rs.proxy.databroker.RabbitClient;
import iudx.rs.proxy.databroker.util.PermissionOpType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class connectorServiceImpl implements ConnectorService {
  private final String amqpUrl;
  private final int amqpsPort;
  private final String vHost;
  Logger LOGGER = LogManager.getLogger(connectorServiceImpl.class);
  RabbitClient rabbitClient;

  public connectorServiceImpl(
      RabbitClient rabbitClient, int amqpsPort, String amqpUrl, String vHost) {
    this.rabbitClient = rabbitClient;
    this.amqpUrl = amqpUrl;
    this.amqpsPort = amqpsPort;
    this.vHost = vHost;
  }

  public static JsonObject getResponseJson(int type, String title, String detail) {
    JsonObject json = new JsonObject();
    json.put(TYPE, type);
    json.put(TITLE, title);
    json.put(DETAIL, detail);
    return json;
  }

  @Override
  public Future<JsonObject> registerConnector(JsonObject request, String vhost) {
    Promise promise = Promise.promise();
    LOGGER.trace("Info : connectorServiceImpl#registerConnector() started");
    String userid = request.getString(USER_ID);
    String resourceId = request.getString("resourceId");
    ConnectorResultContainer requestParams = new ConnectorResultContainer();
    rabbitClient
        .createUserIfNotExist(userid, vhost)
        .compose(
            createUserResult -> {
              requestParams.userid = createUserResult.getString("userid");
              requestParams.apiKey = createUserResult.getString("apiKey");
              return rabbitClient.createQueue(request, vhost);
            })
        .compose(
            createQueResult -> {
              if (createQueResult.containsKey("detail")) {
                LOGGER.error("Error : Connector/queue creation failed. ");
                return Future.failedFuture(createQueResult.toString());
              }
              request.mergeIn(createQueResult);
              requestParams.connectorId = createQueResult.getString(QUEUE_NAME);
              requestParams.isQueueCreated = true;
              request.mergeIn(createQueResult);
              return rabbitClient.bindQueue(request, vhost);
            })
        .compose(
            bindQueueResult -> {
              return rabbitClient.updateUserPermissions(
                  vhost, userid, PermissionOpType.ADD_READ, resourceId);
            })
        .onSuccess(
            successHandler -> {
              JsonObject response =
                  new JsonObject()
                      .put(USER_NAME, requestParams.userid)
                      .put(APIKEY, requestParams.apiKey)
                      .put(QUEUE_NAME, requestParams.connectorId)
                      .put(URL, this.amqpUrl)
                      .put(PORT, this.amqpsPort)
                      .put(VHOST, vhost);
              promise.complete(response);
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error("error : " + failureHandler.getMessage());
              LOGGER.trace("isQueueCreate: {}", requestParams.isQueueCreated);
              if (requestParams.isQueueCreated) {
                Future.future(fu -> rabbitClient.deleteQueue(request, vhost));
              }
              promise.fail(failureHandler.getMessage());
            });

    return promise.future();
  }

  @Override
  public Future<JsonObject> deleteConnectors(JsonObject request, String vHost) {
    LOGGER.trace("Info : ConnectorService#deleteConnectorsstarted");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject deleteStreamingSubscription = new JsonObject();
    String queueName = request.getString(CONNECTOR_ID);
    String userid = request.getString(USER_ID);
    JsonObject requestBody = new JsonObject();
    requestBody.put(QUEUE_NAME, queueName);
    Future<JsonObject> result = rabbitClient.deleteQueue(requestBody, this.vHost);
    result.onComplete(
        resultHandler -> {
          if (resultHandler.succeeded()) {
            JsonObject deleteQueueResponse = (JsonObject) resultHandler.result();
            if (deleteQueueResponse.containsKey(TYPE)) {
              LOGGER.error("failed : Connector/Queue deletion failed " + deleteQueueResponse);
              promise.fail(deleteQueueResponse.encode());
            } else {
              deleteStreamingSubscription.put(
                  DETAILS, "Connector/queue deleted Successfully [" + queueName + "]");
              Future.future(
                  fu ->
                      rabbitClient.updateUserPermissions(
                          vHost, userid, PermissionOpType.DELETE_READ, queueName));
              promise.complete(deleteStreamingSubscription);
            }
          }
          if (resultHandler.failed()) {
            LOGGER.error("failed ::" + resultHandler.cause());
            promise.fail(
                getResponseJson(INTERNAL_ERROR_CODE, ERROR, QUEUE_DELETE_ERROR).toString());
          }
        });
    return promise.future();
  }

  public class ConnectorResultContainer {
    public String apiKey;
    public String id;
    public String userid;
    public String connectorId;
    public String vhost;
    public boolean isQueueCreated;
  }
}
