package com.videostream.transcode;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.Pump;

import java.util.UUID;

public class UploadServiceVerticle extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        vertx.createHttpServer().requestHandler(req -> {
            String method = req.method().name();
            System.out.println("HTTP Method: " + method);
            req.pause();
            String filename = UUID.randomUUID() + ".uploaded";
            vertx.fileSystem().open(filename, new OpenOptions(), ares -> {
                AsyncFile file = ares.result();
                Pump pump = Pump.pump(req, file);
                req.endHandler(v1 -> file.close(v2 -> {
                    System.out.println("Uploaded to " + filename);
                    req.response().end();
                }));
                pump.start();
                req.resume();
            });
        }).listen(8080);
    }
}
