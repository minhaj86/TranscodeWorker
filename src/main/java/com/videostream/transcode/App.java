package com.videostream.transcode;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.rabbitmq.RabbitMQClient;

public class App {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(40).setBlockedThreadCheckInterval(300L * 1000 * 1000 * 1000));
//        TranscodeVerticle transcodeVerticle = new TranscodeVerticle();
        vertx.deployVerticle(TranscodeVerticle.class.getName(), new DeploymentOptions().setWorker(true).setWorkerPoolSize(10).setInstances(4));
    }
}
