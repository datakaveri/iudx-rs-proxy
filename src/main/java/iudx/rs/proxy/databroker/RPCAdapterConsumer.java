package iudx.rs.proxy.databroker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BasicProperties;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.QueueOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQConsumer;

/**
 * Only for testing purpose. should be in separate instance.
 *
 */
public class RPCAdapterConsumer {
  private static final Logger LOGGER = LogManager.getLogger(RPCAdapterConsumer.class);

  RabbitMQClient rabbitMQClient;

  private final QueueOptions options =
      new QueueOptions()
          .setMaxInternalQueueSize(1000)
          .setKeepMostRecent(true);

  public RPCAdapterConsumer(RabbitMQClient rabbitMQClient) {
    this.rabbitMQClient = rabbitMQClient;
  }


  public void start() {
    LOGGER.debug("starting rmq listener " + rabbitMQClient.isConnected());
    rabbitMQClient.basicConsumer("rpc_requests", options, rabbitMQConsumerResult -> {
      if (rabbitMQConsumerResult.succeeded()) {
        // LOGGER.info("message consumed : {}", rabbitMQConsumerResult.result());
        RabbitMQConsumer rmqConsumer = rabbitMQConsumerResult.result();
        rmqConsumer.handler(msg -> {
          BasicProperties properties = msg.properties();
          String correlationId=properties.getCorrelationId();
          String replyQueue = properties.getReplyTo();
          LOGGER.info("message consumed corerelationId: {}, replyQ : {} ", correlationId,
              replyQueue);
          AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
              .correlationId(correlationId)
              .build();

          try {
            Thread.sleep(10000L);
          } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }

          Buffer body = msg.body();
          if (body != null) {
            JsonObject json = new JsonObject(msg.body());
            LOGGER.debug("message received by adapter, {}", json);
            JsonObject jsonResponse = new JsonObject();
            jsonResponse.put("corelationId", correlationId);
            Buffer buffer = Buffer.buffer(jsonResponse.toString());

            rabbitMQClient.basicPublish("", replyQueue, props, buffer);
          } else {
            LOGGER.info("Empty message received by adapter");
          }


        });
      } else {
        rabbitMQConsumerResult.cause().printStackTrace();
      }

    });
  }
}
