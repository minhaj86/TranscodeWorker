package com.videostream.transcode;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    String mqHost = System.getenv("mqhost");
    String transcodeJobQueue = System.getenv("transcodejobqueue");
    String mediaDirectory = System.getenv("mediadirectory");
//    vertx.deployVerticle(yourVerticleInstance, new DeploymentOptions().setWorker(true));

//    vertx.setPeriodic(1000, r -> {
//      System.out.println(Thread.currentThread().getName());
//      ConnectionFactory mqConnectionFactory = new ConnectionFactory();
//      mqConnectionFactory.setHost(mqHost);
//      mqConnectionFactory.setPort(5672);
//      Connection mqConnection = null;
//      try {
//        mqConnection = mqConnectionFactory.newConnection();
//        Channel channel = mqConnection.createChannel();
//        channel.exchangeDeclare("exc", "direct", true);
//        channel.queueBind(transcodeJobQueue, "exc", "black");
//        byte[] messageBodyBytes = ("Hello, world!@@@@@@@@@" + System.currentTimeMillis()).getBytes();
//
//        AMQP.BasicProperties amqproperties = new AMQP.BasicProperties();
//        channel.basicPublish("exc", "black", amqproperties, messageBodyBytes);
//        channel.close();
//        mqConnection.close();
//      } catch (IOException e) {
//        e.printStackTrace();
//      } catch (TimeoutException e) {
//        e.printStackTrace();
//      }
//    });

    RabbitMQClient client = getRabbitMQClient(vertx, mqHost);
    client.start(asyncResult -> {
      if (asyncResult.succeeded()) {
        System.out.println("RabbitMQ successfully connected!");
        MQUtils.basicConsumer(vertx, client, transcodeJobQueue, mediaDirectory);
      } else {
        System.out.println("Fail to connect to RabbitMQ " + asyncResult.cause().getMessage());
      }
    });
  }

  private RabbitMQClient getRabbitMQClient(Vertx vertx, String mqHost) {
    RabbitMQOptions config = new RabbitMQOptions()
      .setHost(mqHost)
      .setPort(5672)
      .setConnectionTimeout(6000) // in milliseconds
      .setRequestedHeartbeat(60) // in seconds
      .setHandshakeTimeout(6000) // in milliseconds
      .setRequestedChannelMax(5)
      .setNetworkRecoveryInterval(500) // in milliseconds
      .setAutomaticRecoveryEnabled(true);

    return RabbitMQClient.create(vertx, config);
  }
}
