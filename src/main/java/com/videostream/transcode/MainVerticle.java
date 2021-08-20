package com.videostream.transcode;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;

public class MainVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        String mqHost = System.getenv("mqhost");
        String transcodeJobQueue = System.getenv("transcodejobqueue");
        String mediaDirectory = System.getenv("mediadirectory");
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
