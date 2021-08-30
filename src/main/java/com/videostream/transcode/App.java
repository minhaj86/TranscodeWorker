package com.videostream.transcode;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;

public class App {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(40).setBlockedThreadCheckInterval(300L * 1000 * 1000 * 1000));
        EventBus eventBus = vertx.eventBus();
        eventBus.registerDefaultCodec(TranscodeJobDTO.class, new TranscodeDtoCodec());
//        vertx.deployVerticle(TranscodeVerticle.class.getName(), new DeploymentOptions().setWorker(true).setWorkerPoolSize(10).setInstances(4));
        vertx.deployVerticle(UploadServiceVerticle.class.getName(), new DeploymentOptions().setWorker(true).setWorkerPoolSize(10));
        vertx.deployVerticle(RestResourcesVerticle.class.getName(), new DeploymentOptions().setWorker(true).setWorkerPoolSize(10));
        vertx.deployVerticle(TranscodeJobEventConsumer.class.getName(), new DeploymentOptions().setWorker(true).setWorkerPoolSize(10));
    }
}
