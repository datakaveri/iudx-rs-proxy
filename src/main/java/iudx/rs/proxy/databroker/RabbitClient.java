package iudx.rs.proxy.databroker;

import static iudx.rs.proxy.apiserver.util.ApiServerConstants.FAILED;
import static iudx.rs.proxy.databroker.util.Constants.*;
import static iudx.rs.proxy.databroker.util.Util.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import iudx.rs.proxy.common.HttpStatusCode;
import iudx.rs.proxy.common.Response;
import iudx.rs.proxy.common.ResponseUrn;
import iudx.rs.proxy.databroker.util.Constants;
import iudx.rs.proxy.databroker.util.PermissionOpType;
import iudx.rs.proxy.metering.util.ResponseBuilder;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RabbitClient {

  private static final Logger LOGGER = LogManager.getLogger(RabbitClient.class);

  private final RabbitMQClient client;
  private final RabbitWebClient webClient;

  public RabbitClient(
      Vertx vertx, RabbitMQOptions rabbitConfigs, RabbitWebClient webClient, JsonObject configs) {
    String internalVhost = configs.getString("internalVhost");
    rabbitConfigs.setVirtualHost(internalVhost);
    this.client = getRabbitmqClient(vertx, rabbitConfigs);
    this.webClient = webClient;
    client.start(
        clientStartupHandler -> {
          if (clientStartupHandler.succeeded()) {
            LOGGER.info("Info : rabbit MQ client started");
          } else if (clientStartupHandler.failed()) {
            LOGGER.fatal("Fail : rabbit MQ client startup failed.");
          }
        });
  }

  private RabbitMQClient getRabbitmqClient(Vertx vertx, RabbitMQOptions rabbitConfigs) {
    return RabbitMQClient.create(vertx, rabbitConfigs);
  }

  /**
   * The createQueue implements the create queue operation.
   *
   * @param request which is a Json object
   * @param vhost virtual-host
   * @return response which is a Future object of promise of Json type
   */
  public Future<JsonObject> createQueue(JsonObject request, String vhost) {
    LOGGER.trace("Info : RabbitClient#createQueue() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      JsonObject configProp = new JsonObject();
      JsonObject arguments = new JsonObject();
      arguments
          .put(X_MESSAGE_TTL_NAME, X_MESSAGE_TTL_VALUE)
          .put(X_MAXLENGTH_NAME, X_MAXLENGTH_VALUE)
          .put(X_QUEUE_MODE_NAME, X_QUEUE_MODE_VALUE);
      configProp.put(X_QUEUE_TYPE, true);
      configProp.put(X_QUEUE_ARGUMENTS, arguments);
      String queueName = request.getString("resourceId");
      String url = "/api/queues/" + vhost + "/" + encodeValue(queueName); // "durable":true
      webClient
          .requestAsync(REQUEST_PUT, url, configProp)
          .onComplete(
              ar -> {
                if (ar.succeeded()) {
                  HttpResponse<Buffer> response = ar.result();
                  if (response != null && !response.equals(" ")) {
                    int status = response.statusCode();
                    if (status == HttpStatus.SC_CREATED) {
                      finalResponse.put(Constants.QUEUE_NAME, queueName);
                    } else if (status == HttpStatus.SC_NO_CONTENT) {

                      finalResponse.mergeIn(
                          getResponseJson(
                              HttpStatus.SC_CONFLICT,
                              HttpStatusCode.CONFLICT.getUrn(),
                              QUEUE_ALREADY_EXISTS),
                          true);
                    } else if (status == HttpStatus.SC_BAD_REQUEST) {
                      finalResponse.mergeIn(
                          getResponseJson(
                              status, FAILURE, QUEUE_ALREADY_EXISTS_WITH_DIFFERENT_PROPERTIES),
                          true);
                    }
                  }
                  promise.complete(finalResponse);
                } else {
                  LOGGER.error("Fail : Creation of Queue failed - ", ar.cause());
                  finalResponse.mergeIn(getResponseJson(500, FAILURE, QUEUE_CREATE_ERROR));
                  promise.fail(finalResponse.toString());
                }
              });
    }
    return promise.future();
  }

  /**
   * The deleteQueue implements the delete queue operation.
   *
   * @param request which is a Json object
   * @param vhost virtual-host
   * @return response which is a Future object of promise of Json type
   */
  public Future<JsonObject> deleteQueue(JsonObject request, String vhost) {
    LOGGER.trace("Info : RabbitClient#deleteQueue() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String queueName = request.getString(QUEUE_NAME);
      String url = "/api/queues/" + vhost + "/" + encodeValue(queueName);
      webClient
          .requestAsync(REQUEST_DELETE, url)
          .onComplete(
              ar -> {
                if (ar.succeeded()) {
                  HttpResponse<Buffer> response = ar.result();

                  if (response != null && !response.equals(" ")) {
                    int status = response.statusCode();
                    if (status == HttpStatus.SC_NO_CONTENT) {
                      finalResponse.put(QUEUE_NAME, queueName);
                    } else if (status == HttpStatus.SC_NOT_FOUND) {
                      ResponseBuilder responseBuilder =
                          new ResponseBuilder(FAILED)
                              .setTypeAndTitle(404, ResponseUrn.QUEUE_NOT_FOUND_URN.getUrn())
                              .setMessage(QUEUE_DOES_NOT_EXISTS);
                      finalResponse.mergeIn(responseBuilder.getResponse());
                    }
                  }
                  LOGGER.info("deleteQueueResponse:  " + finalResponse);
                  promise.complete(finalResponse);
                } else {
                  LOGGER.error("Fail : deletion of queue failed - ", ar.cause());
                  finalResponse.mergeIn(getResponseJson(500, FAILURE, QUEUE_DELETE_ERROR));
                  promise.fail(finalResponse.toString());
                }
              });
    }
    return promise.future();
  }

  /**
   * The bindQueue implements the bind queue to exchange by routing key.
   *
   * @param request which is a Json object
   * @param vhost virtual-host
   * @return response which is a Future object of promise of Json type
   */
  public Future<JsonObject> bindQueue(JsonObject request, String vhost) {
    LOGGER.trace("Info : RabbitClient#bindQueue() started");
    LOGGER.debug("request : {}; vHost : {}", request, vhost);
    JsonObject finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String exchangeName = request.getString("exchangeName");
      String queueName = request.getString(QUEUE_NAME);
      String routingKey = request.getString("resourceId");
      request.put("routing_key", routingKey);
      String url =
          "/api/bindings/"
              + vhost
              + "/e/"
              + encodeValue(exchangeName)
              + "/q/"
              + encodeValue(queueName);

      webClient
          .requestAsync(REQUEST_POST, url, request)
          .onComplete(
              ar -> {
                if (ar.succeeded()) {
                  HttpResponse<Buffer> response = ar.result();
                  if (response != null && !response.equals(" ")) {
                    int status = response.statusCode();
                    LOGGER.info("Info : Binding " + "rkey" + "Success. Status is " + status);
                    if (status == HttpStatus.SC_CREATED) {
                      finalResponse.put(EXCHANGE, exchangeName);
                      finalResponse.put(Constants.QUEUE_NAME, queueName);
                    } else if (status == HttpStatus.SC_NOT_FOUND) {
                      finalResponse.mergeIn(
                          getResponseJson(status, FAILURE, QUEUE_EXCHANGE_NOT_FOUND));
                    }
                  }
                  LOGGER.debug("Success : " + finalResponse);
                  promise.complete(finalResponse);
                } else {
                  LOGGER.error("Fail : Binding of Queue failed - ", ar.cause());
                  finalResponse.mergeIn(getResponseJson(500, FAILURE, QUEUE_BIND_ERROR));
                  promise.fail(finalResponse.toString());
                }
              });
    }
    return promise.future();
  }

  /**
   * The createUserIfNotExist implements the create user if does not exist.
   *
   * @param userid which is a String
   * @param vhost which is a String
   * @return response which is a Future object of promise of Json type
   */
  public Future<JsonObject> createUserIfNotExist(String userid, String vhost) {
    LOGGER.trace("Info : RabbitClient#createUserIfNotPresent() started");
    Promise<JsonObject> promise = Promise.promise();
    String password = randomPassword.get();
    String url = "/api/users/" + userid;
    /* Check if user exists */
    JsonObject response = new JsonObject();
    webClient
        .requestAsync(REQUEST_GET, url)
        .onComplete(
            reply -> {
              if (reply.succeeded()) {
                /* Check if user not found */
                if (reply.result().statusCode() == HttpStatus.SC_NOT_FOUND) {
                  LOGGER.debug("Success : User not found. creating user");
                  /* Create new user */
                  Future<JsonObject> userCreated = createUser(userid, password, vhost, url);
                  userCreated.onComplete(
                      handler -> {
                        if (handler.succeeded()) {
                          /* Handle the response */
                          JsonObject result = handler.result();
                          LOGGER.debug("userCreatedresult: " + result);
                          response.put(USER_ID, userid);
                          response.put(APIKEY, password);
                          response.put(TYPE, result.getInteger("type"));
                          response.put(TITLE, result.getString("title"));
                          response.put(DETAILS, result.getString("detail"));
                          response.put(VHOST_PERMISSIONS, vhost);
                          LOGGER.debug("userCreated: " + response);
                          promise.complete(response);
                        } else {
                          LOGGER.error(
                              "Error : Error in user creation. Cause : " + handler.cause());
                          response.mergeIn(
                              getResponseJson(INTERNAL_ERROR_CODE, ERROR, USER_CREATION_ERROR));
                          promise.fail(response.toString());
                        }
                      });

                } else if (reply.result().statusCode() == HttpStatus.SC_OK) {
                  LOGGER.debug(
                      " reply.result().bodyAsJsonObject(): " + reply.result().bodyAsJsonObject());
                  Future.future(
                      fu ->
                          updateUserPermissions(
                              vhost, userid, PermissionOpType.ADD_WRITE, "amq.default"));
                  response.put(USER_ID, userid);
                  response.put(APIKEY, API_KEY_MESSAGE);
                  response.mergeIn(
                      getResponseJson(SUCCESS_CODE, DATABASE_READ_SUCCESS, DATABASE_READ_SUCCESS));
                  response.put(VHOST_PERMISSIONS, vhost);
                  promise.complete(response);
                }

              } else {
                /* Handle API error */
                LOGGER.error(
                    "Error : Something went wrong while finding user using mgmt API: "
                        + reply.cause());
                promise.fail(reply.cause().toString());
              }
            });
    return promise.future();
  }

  /**
   * CreateUserIfNotPresent's helper method which creates user if not present.
   *
   * @param userid which is a String
   * @param vhost which is a String
   * @return response which is a Future object of promise of Json type
   */
  Future<JsonObject> createUser(String userid, String password, String vhost, String url) {
    LOGGER.trace("Info : RabbitClient#createUser() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject response = new JsonObject();
    JsonObject arg = new JsonObject();
    arg.put(PASSWORD, password);
    arg.put(TAGS, NONE);

    webClient
        .requestAsync(REQUEST_PUT, url, arg)
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                /* Check if user is created */
                if (ar.result().statusCode() == HttpStatus.SC_CREATED) {
                  response.put(USER_ID, userid);
                  response.put(PASSWORD, password);
                  LOGGER.debug("Info : user created successfully");
                  // set permissions to vhost for newly created user
                  Future<JsonObject> vhostPermission = setVhostPermissions(userid, vhost);
                  vhostPermission.onComplete(
                      handler -> {
                        if (handler.succeeded()) {
                          response.mergeIn(
                              getResponseJson(
                                  SUCCESS_CODE,
                                  VHOST_PERMISSIONS,
                                  handler.result().getString(DETAIL)));
                          promise.complete(response);
                        } else {
                          /* Handle error */
                          LOGGER.error(
                              "Error : error in setting vhostPermissions. Cause : ",
                              handler.cause());
                          promise.fail("Error : error in setting vhostPermissions");
                        }
                      });

                } else {
                  /* Handle error */
                  LOGGER.error("Error : createUser method - Some network error. cause", ar.cause());
                  response.put(FAILURE, NETWORK_ISSUE);
                  promise.fail(response.toString());
                }
              } else {
                /* Handle error */
                LOGGER.info(
                    "Error : Something went wrong while creating user using mgmt API :",
                    ar.cause());
                response.put(FAILURE, CHECK_CREDENTIALS);
                promise.fail(response.toString());
              }
            });
    return promise.future();
  }

  /* changed the access modifier to default as setTopicPermissions is not being called anywhere */
  /**
   * set vhost permissions for given userName.
   *
   * @param username which is a String
   * @param vhost which is a String
   * @return response which is a Future object of promise of Json type
   */
  Future<JsonObject> setVhostPermissions(String username, String vhost) {
    LOGGER.trace("Info : RabbitClient#setVhostPermissions() started");
    /* Construct URL to use */
    JsonObject vhostPermissions = new JsonObject();
    // all keys are mandatory. empty strings used for configure,read as not
    // permitted.
    vhostPermissions.put(CONFIGURE, DENY);
    vhostPermissions.put(WRITE, "amq.default");
    vhostPermissions.put(READ, NONE);
    Promise<JsonObject> promise = Promise.promise();
    /* Construct a response object */
    JsonObject vhostPermissionResponse = new JsonObject();
    String url = "/api/permissions/" + vhost + "/" + encodeValue(username);
    webClient
        .requestAsync(REQUEST_PUT, url, vhostPermissions)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                /* Check if permission was set */
                if (handler.result().statusCode() == HttpStatus.SC_CREATED) {
                  LOGGER.debug(
                      "Success :write permission set for user [ "
                          + username
                          + " ] in vHost [ "
                          + vhost
                          + "]");

                  vhostPermissionResponse.mergeIn(
                      getResponseJson(SUCCESS_CODE, VHOST_PERMISSIONS, VHOST_PERMISSIONS_WRITE));
                  LOGGER.debug("vhostPermissionResponse: " + vhostPermissionResponse);
                  promise.complete(vhostPermissionResponse);
                } else {
                  LOGGER.error(
                      "Error : error in write permission set for user [ "
                          + username
                          + " ] in vHost [ "
                          + vhost
                          + " ]");
                  vhostPermissionResponse.mergeIn(
                      getResponseJson(
                          INTERNAL_ERROR_CODE, VHOST_PERMISSIONS, VHOST_PERMISSION_SET_ERROR));
                  promise.fail(vhostPermissions.toString());
                }
              } else {
                /* Check if request has an error */
                LOGGER.error(
                    "Error : error in write permission set for user [ "
                        + username
                        + " ] in vHost [ "
                        + vhost
                        + " ]");
                vhostPermissionResponse.mergeIn(
                    getResponseJson(
                        INTERNAL_ERROR_CODE, VHOST_PERMISSIONS, VHOST_PERMISSION_SET_ERROR));
                promise.fail(vhostPermissions.toString());
              }
            });
    return promise.future();
  }

  Future<JsonObject> getUserPermissions(String userId, String vhost) {
    LOGGER.trace("Info : RabbitClient#getUserpermissions() started");
    Promise<JsonObject> promise = Promise.promise();
    String url = "/api/permissions/" + vhost + "/" + userId;

    webClient
        .requestAsync(REQUEST_GET, url)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                HttpResponse<Buffer> rmqResponse = handler.result();
                if (rmqResponse.statusCode() == HttpStatus.SC_NOT_FOUND) {
                  JsonObject vhostPermissions = new JsonObject();
                  // all keys are mandatory. empty strings used for configure,read as not
                  // permitted.
                  vhostPermissions.put(CONFIGURE, DENY);
                  vhostPermissions.put(WRITE, NONE);
                  vhostPermissions.put(READ, NONE);
                  promise.complete(vhostPermissions);
                }
                if (rmqResponse.statusCode() == HttpStatus.SC_OK) {
                  JsonObject permissionArray = new JsonObject(rmqResponse.body().toString());
                  promise.complete(permissionArray);
                } else {
                  LOGGER.error(handler.cause());
                  LOGGER.error(handler.result());
                  Response response =
                      new Response.Builder()
                          .withStatus(rmqResponse.statusCode())
                          .withTitle(ResponseUrn.BAD_REQUEST_URN.getUrn())
                          .withDetail("problem while getting user permissions")
                          .withUrn(ResponseUrn.BAD_REQUEST_URN.getUrn())
                          .build();
                  promise.fail(response.toString());
                }
              } else {
                Response response =
                    new Response.Builder()
                        .withStatus(HttpStatus.SC_BAD_REQUEST)
                        .withTitle(ResponseUrn.BAD_REQUEST_URN.getUrn())
                        .withDetail(handler.cause().getLocalizedMessage())
                        .withUrn(ResponseUrn.BAD_REQUEST_URN.getUrn())
                        .build();
                promise.fail(response.toString());
              }
            });
    return promise.future();
  }

  public Future<JsonObject> updateUserPermissions(
      String vhost, String userId, PermissionOpType type, String resourceId) {
    Promise<JsonObject> promise = Promise.promise();
    getUserPermissions(userId, vhost)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                String url = "/api/permissions/" + vhost + "/" + encodeValue(userId);
                JsonObject existingPermissions = handler.result();
                JsonObject updatedPermission =
                    getUpdatedPermission(existingPermissions, type, resourceId);
                webClient
                    .requestAsync(REQUEST_PUT, url, updatedPermission)
                    .onComplete(
                        updatePermissionHandler -> {
                          if (updatePermissionHandler.succeeded()) {
                            HttpResponse<Buffer> rmqResponse = updatePermissionHandler.result();
                            if (rmqResponse.statusCode() == HttpStatus.SC_NO_CONTENT) {
                              Response response =
                                  new Response.Builder()
                                      .withStatus(HttpStatus.SC_NO_CONTENT)
                                      .withTitle(ResponseUrn.SUCCESS_URN.getUrn())
                                      .withDetail("Permission updated successfully.")
                                      .withUrn(ResponseUrn.SUCCESS_URN.getUrn())
                                      .build();
                              promise.complete(response.toJson());
                            } else if (rmqResponse.statusCode() == HttpStatus.SC_CREATED) {
                              Response response =
                                  new Response.Builder()
                                      .withStatus(HttpStatus.SC_CREATED)
                                      .withTitle(ResponseUrn.SUCCESS_URN.getUrn())
                                      .withDetail("Permission updated successfully.")
                                      .withUrn(ResponseUrn.SUCCESS_URN.getUrn())
                                      .build();
                              promise.complete(response.toJson());
                            } else {
                              Response response =
                                  new Response.Builder()
                                      .withStatus(rmqResponse.statusCode())
                                      .withTitle(ResponseUrn.BAD_REQUEST_URN.getUrn())
                                      .withDetail(rmqResponse.statusMessage())
                                      .withUrn(ResponseUrn.BAD_REQUEST_URN.getUrn())
                                      .build();
                              promise.fail(response.toString());
                            }
                          } else {
                            Response response =
                                new Response.Builder()
                                    .withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                                    .withTitle(ResponseUrn.BAD_REQUEST_URN.getUrn())
                                    .withDetail(updatePermissionHandler.cause().getMessage())
                                    .withUrn(ResponseUrn.BAD_REQUEST_URN.getUrn())
                                    .build();
                            promise.fail(response.toString());
                          }
                        });
              } else {
                promise.fail(handler.cause().getMessage());
              }
            });
    return promise.future();
  }

  private JsonObject getUpdatedPermission(
      JsonObject permissionsJson, PermissionOpType type, String permisionId) {
    StringBuilder permission;
    switch (type) {
      case ADD_READ:
      case ADD_WRITE:
        permission = new StringBuilder(permissionsJson.getString(type.permission));
        // Check if permission already contains resourceId
        if (permission.length() != 0 && permission.indexOf(permisionId) == -1) {
          // Remove ".*" if present at the start
          if (permission.indexOf(".*") != -1) {
            permission.deleteCharAt(0).deleteCharAt(0);
          }
          // Append resourceId if it's not empty
          if (permission.length() != 0) {
            permission.append("|").append(permisionId);
          } else {
            permission.append(permisionId);
          }
          // Update permissionsJson with the new permission string
          permissionsJson.put(type.permission, permission.toString());
        }
        break;
      case DELETE_READ:
      case DELETE_WRITE:
        permission = new StringBuilder(permissionsJson.getString(type.permission));
        String[] permissionsArray = permission.toString().split("\\|");
        if (permissionsArray.length > 0) {
          Stream<String> stream = Arrays.stream(permissionsArray);
          String updatedPermission =
              stream.filter(item -> !item.equals(permisionId)).collect(Collectors.joining("|"));
          permissionsJson.put(type.permission, updatedPermission);
        }
        break;
      default:
        break;
    }
    return permissionsJson;
  }

  Future<JsonObject> resetPasswordInRmq(String userid, String password) {
    LOGGER.trace("Info : RabbitClient#resetPassword() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject response = new JsonObject();
    JsonObject arg = new JsonObject();
    arg.put(PASSWORD, password);
    arg.put(TAGS, NONE);
    String url = "/api/users/" + userid;
    webClient
        .requestAsync(REQUEST_PUT, url, arg)
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                if (ar.result().statusCode() == HttpStatus.SC_NO_CONTENT) {
                  response.put(userid, userid);
                  response.put(PASSWORD, password);
                  LOGGER.debug("user password changed");
                  promise.complete(response);
                } else {
                  LOGGER.error("Error :reset pwd method failed", ar.cause());
                  response.put(FAILURE, NETWORK_ISSUE);
                  promise.fail(response.toString());
                }
              } else {
                LOGGER.error("User creation failed using mgmt API :", ar.cause());
                response.put(FAILURE, CHECK_CREDENTIALS);
                promise.fail(response.toString());
              }
            });
    return promise.future();
  }
}
