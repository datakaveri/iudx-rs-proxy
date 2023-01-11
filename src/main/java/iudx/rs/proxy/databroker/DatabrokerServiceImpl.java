package iudx.rs.proxy.databroker;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import iudx.rs.proxy.common.Response;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.Queue.DeclareOk;
import com.rabbitmq.client.BasicProperties;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.QueueOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQConsumer;
import iudx.rs.proxy.common.ResponseUrn;

import static iudx.rs.proxy.apiserver.util.ApiServerConstants.FAILED;
import static iudx.rs.proxy.apiserver.util.ApiServerConstants.HEADER_PUBLIC_KEY;
import static iudx.rs.proxy.metering.util.Constants.SUCCESS;


public class DatabrokerServiceImpl implements DatabrokerService {

  private static final Logger LOGGER = LogManager.getLogger(DatabrokerServiceImpl.class);

  private final String replyQueue;
  private final String publishEx;
  private final Vertx vertx;
  RabbitMQClient client;

  private final QueueOptions queueOption =
      new QueueOptions()
          .setMaxInternalQueueSize(2)
          .setAutoAck(false)
          .setKeepMostRecent(true);


  public DatabrokerServiceImpl(Vertx vertx, RabbitMQClient rabbitMQClient, final String publishEx,
      final String replyQueue) {
    this.vertx = vertx;
    this.client = rabbitMQClient;
    this.replyQueue = "rpc_responses";
    this.publishEx = publishEx;

    client.basicQos(1);
  }

  /**
   * create Exchanges, queues and proper bindings for routing, In this case producer and consumer
   * will be before hand know about the exchanges and queus for consuming and pushing messages
   *
   * Issue : negatively acknowledge messages (Nack : in case of correlation id mismatch) there is no
   * way to flag a certain consumer to not consume same message again, thus this creates a repeated
   * consumption of same message (currently tested for single consumer only).
   *
   */
  @Override
  public DatabrokerService executeAdapterQuery(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {


    final String corelationId = UUID.randomUUID().toString();
    LOGGER.debug("corelationid : {}", corelationId);
    AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
        .correlationId(corelationId)
        .replyTo(replyQueue)
        .build();

    String routingKey = request.getJsonArray("id").getString(0);

    LOGGER.debug("queue declared : {}", replyQueue);
    Buffer buffer = Buffer.buffer(request.toString());
    client.basicPublish("rpc-adapter-requests", routingKey, props, buffer, publishHandler -> {
      if (publishHandler.succeeded()) {
        LOGGER.debug("published with cid: {}", corelationId);
        client.basicConsumer("rpc_responses", queueOption, rabbitMQConsumerResult -> {
          if (rabbitMQConsumerResult.succeeded()) {
            // LOGGER.info("message consumed : {}", rabbitMQConsumerResult.result());
            RabbitMQConsumer rmqConsumer = rabbitMQConsumerResult.result();

            long timerId = vertx.setTimer(5000, timeout -> {
              LOGGER.info("max wait time elapsed for consumer, cancelling consumer");
              rmqConsumer.cancel();

              JsonObject json = new JsonObject();
              json.put("type", ResponseUrn.YET_NOT_IMPLEMENTED_URN.getUrn());
              json.put("title", "request timed out");
              json.put("details", "request taking more than allocated time, please contact admin");

              handler.handle(Future.failedFuture(json.toString()));
              return;
            });

            rmqConsumer.handler(msg -> {
              LOGGER.debug("Got message: " + msg);
              BasicProperties properties = msg.properties();
              String reply_correlationId = properties.getCorrelationId();
              String replyQueue = properties.getReplyTo();
              Long deliveryTag = msg.envelope().getDeliveryTag();
              LOGGER.info("message consumed corerelationId: {}, replyQ : {}, deliveryTag : {} ",
                  reply_correlationId,
                  replyQueue,
                  deliveryTag);

              Buffer body = msg.body();
              if (body != null) {
                JsonObject json = new JsonObject(msg.body());
                LOGGER.debug("Got message: " + json);
                if (reply_correlationId.equals(corelationId)) {
                  LOGGER.info("ack mode");
                  client.basicAck(deliveryTag, false, asyncResult -> {
                    LOGGER.info("response received for consumer, cancelling consumer");
                    vertx.cancelTimer(timerId);
                    rmqConsumer.cancel();
                    handler.handle(Future.succeededFuture(json));
                  });
                } else {
                  client.basicNack(deliveryTag, true, true, resultHandler -> {
                    LOGGER.info("nack for corelationId : {}", reply_correlationId);
                  });
                }
              } else {
                LOGGER.info("Empty message received by adapter");
              }
            });
          } else {
            rabbitMQConsumerResult.cause().printStackTrace();
          }
        });
      } else {
        handler.handle(Future.failedFuture("failed"));
      }
    });
    return this;
  }


  /**
   * Create an exclusive queue and pass it as message properties to RMQ. message consumer(adaptor)
   * will look for 'reply_to' property in received message and send response in the same queue.
   *
   * Requeued problem as stated above is not a problem in this since, dedicated queue is used for
   * every request/response
   *
   * Since no dedicated exchange is there for responses, so there might be issue for message
   * tracking (not to sure about it)
   *
   */
  @Override
  public DatabrokerService executeAdapterQueryRPC(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    final String corelationId = UUID.randomUUID().toString();
    final String replyQueueName = UUID.randomUUID().toString();
    Map<String, Object> map = new HashMap<>();
    map.put(HEADER_PUBLIC_KEY,request.getValue(HEADER_PUBLIC_KEY));
    Future<DeclareOk> replyQueueDeclareFuture =
        client.queueDeclare(replyQueueName, false, true, true);

    LOGGER.debug("corelationid : {}", corelationId);
    AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
        .correlationId(corelationId)
        .replyTo(replyQueueName)
        .headers(map)
        .build();

    LOGGER.debug("queue declared : {}", replyQueueName);
    String routingKey = request.getJsonArray("id").getString(0);
    LOGGER.debug("routing key : {}", routingKey);
    Buffer buffer = Buffer.buffer(request.toString());
    Future<Void> publishFut =
        client.basicPublish("rpc-adapter-requests", routingKey, props, buffer);

    client.basicConsumer(replyQueueName, queueOption, rabbitMQConsumerResult -> {
      if (rabbitMQConsumerResult.succeeded()) {
        // LOGGER.info("message consumed : {}", rabbitMQConsumerResult.result());
        RabbitMQConsumer rmqConsumer = rabbitMQConsumerResult.result();

        long timerId = vertx.setTimer(10000, timeout -> {
          LOGGER.info("max wait time elapsed for consumer, cancelling consumer");
          rmqConsumer.cancel();
          JsonObject json = new JsonObject();
          json.put("type", ResponseUrn.YET_NOT_IMPLEMENTED_URN.getUrn());
          json.put("title", "request timed out");
          json.put("details", "request taking more than allocated time, please contact admin");

          handler.handle(Future.failedFuture(json.toString()));
          return;
        });

        rmqConsumer.handler(msg -> {
          LOGGER.debug("Got message: " + msg);
          BasicProperties properties = msg.properties();
          String reply_correlationId = properties.getCorrelationId();
          String replyQueue = properties.getReplyTo();
          Long deliveryTag = msg.envelope().getDeliveryTag();
          LOGGER.info("message consumed corerelationId: {}, replyQ : {}, deliveryTag : {} ",
              reply_correlationId,
              replyQueue,
              deliveryTag);

          Buffer body = msg.body();
          if (body != null) {
            JsonObject json = new JsonObject(msg.body());
            LOGGER.debug("Got message: " + json);
            if (reply_correlationId.equals(corelationId)) {
              client.basicAck(deliveryTag, false, asyncResult -> {
                LOGGER.info("[ACK] Response received for correlationId : {}, cancelling consumer",
                    corelationId);
                vertx.cancelTimer(timerId);
                rmqConsumer.cancel();
                handler.handle(Future.succeededFuture(json));
              });
            } else {
              client.basicNack(deliveryTag, true, true, resultHandler -> {
                LOGGER.info("[Nack] corelationId : {}", reply_correlationId);
              });
              handler.handle(Future.failedFuture("Failed to get the response"));
            }
          } else {
            LOGGER.info("Empty message received by adapter");
            handler.handle(Future.failedFuture("Empty message received by adapter"));
          }
        });
      } else {
        rabbitMQConsumerResult.cause().printStackTrace();
      }
    });
    return this;
  }

  @Override
  public DatabrokerService publishMessage(
      JsonObject request,
      String toExchange,
      String routingKey,
      Handler<AsyncResult<JsonObject>> handler) {

    Buffer buffer = Buffer.buffer(request.toString());

    if (!client.isConnected()) client.start();

    client.basicPublish(
        toExchange,
        routingKey,
        buffer,
        publishHandler -> {
          if (publishHandler.succeeded()) {
            JsonObject result = new JsonObject().put("type", SUCCESS);
            handler.handle(Future.succeededFuture(result));
          } else {
            Response respBuilder =
                new Response.Builder()
                    .withTitle(FAILED)
                    .withDetail(publishHandler.cause().getLocalizedMessage())
                    .build();
            handler.handle(Future.failedFuture(respBuilder.toString()));
          }
        });
    return this;
  }
}
