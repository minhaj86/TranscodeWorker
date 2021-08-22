package com.videostream.transcode;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.streams.Pump;
import io.vertx.ext.web.Router;

import java.util.UUID;

public class UploadServiceVerticle extends AbstractVerticle {
    private String MEDIA_DIRECTORY = null;
    @Override
    public void start() throws Exception {
        this.MEDIA_DIRECTORY = System.getenv("mediadirectory");
        Router router = Router.router(vertx);
        router.route(HttpMethod.POST, "/api/upload/:videoId/:fileName").handler(routingContext-> {
            HttpServerRequest httpRequest = routingContext.request();
            httpRequest.pause();
            @Nullable String videoId = httpRequest.getParam("videoId");
            @Nullable String fileName = httpRequest.getParam("fileName");
            String filePath = this.MEDIA_DIRECTORY + videoId + "/" + UUID.randomUUID() + "_" + fileName;
            vertx.fileSystem().open(filePath, new OpenOptions(), ares -> {
                AsyncFile file = ares.result();
                Pump pump = Pump.pump(httpRequest, file);
                httpRequest.endHandler(v1 -> file.close(v2 -> {
                    System.out.println("Uploaded to " + filePath);
                    httpRequest.response().end();
                }));
                pump.start();
                httpRequest.resume();
            });
        });
        vertx.createHttpServer().requestHandler(router).listen(8080);
    }
}
