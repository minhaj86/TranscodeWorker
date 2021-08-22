package com.videostream.transcode;

import io.vertx.codegen.annotations.Nullable;
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
            @Nullable String resourcePath = req.path();
            System.out.println("HTTP Method: " + method);
            System.out.println("HTTP Path: " + resourcePath);
            req.pause();
            if (method.equals("POST") && resourcePath.equals("/upload")) {
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
            } else if (!resourcePath.equals("/upload")) {
                System.out.println("Wrong path");
                req.endHandler(v1 -> {
                    req.response().setStatusCode(404);
                    req.response().end();
                });
                req.resume();
            } else if (!method.equals("POST")) {
                System.out.println("Wrong method");
                req.endHandler(v1 -> {
                    req.response().setStatusCode(405);
                    req.response().end();
                });
                req.resume();
            }
        }).listen(8080);
    }
}
