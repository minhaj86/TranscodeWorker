package com.videostream.transcode;

import io.vertx.core.AbstractVerticle;
import org.jboss.resteasy.plugins.server.vertx.VertxRequestHandler;
import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;

public class RestResourcesVerticle extends AbstractVerticle {
    @Override
    public void start() throws Exception {
        VertxResteasyDeployment deployment = new VertxResteasyDeployment();
        deployment.start();
        deployment.getRegistry().addPerInstanceResource(TranscodeJobResource.class);
        vertx.createHttpServer()
            .requestHandler(new VertxRequestHandler(vertx, deployment))
            .listen(8080, ar -> {
                System.out.println("Server started on port "+ ar.result().actualPort());
            });
    }
}
