package com.videostream.transcode;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import org.jboss.resteasy.plugins.server.vertx.VertxRequestHandler;
import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;

public class RestResourcesVerticle extends AbstractVerticle {
    final static Logger logger = LoggerFactory.getLogger(RestResourcesVerticle.class);

    @Override
    public void start() throws Exception {
        VertxResteasyDeployment deployment = new VertxResteasyDeployment();
        deployment.start();
        deployment.getRegistry().addPerInstanceResource(TranscodeJobResource.class);
        vertx.createHttpServer()
            .requestHandler(new VertxRequestHandler(vertx, deployment))
            .listen(8080, serverAsyncResult -> {
                logger.info("Server started on port "+ serverAsyncResult.result().actualPort());
            });
    }
}
