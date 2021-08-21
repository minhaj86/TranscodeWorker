package com.videostream.transcode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQConsumer;
import io.vertx.rabbitmq.RabbitMQOptions;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

import java.io.IOException;

public class TranscodeVerticle extends AbstractVerticle {
    String requestJson = null;
    private String mediaDirectory=null;
    private String mqHost=null;
    private String transcodeJobQueue=null;

    public String getRequestJson() {
        return requestJson;
    }

    public void setRequestJson(String requestJson) {
        this.requestJson = requestJson;
    }

    public String getMediaDirectory() {
        return mediaDirectory;
    }

    public void setMediaDirectory(String mediaDirectory) {
        this.mediaDirectory = mediaDirectory;
    }

    public String getMqHost() {
        return mqHost;
    }

    public void setMqHost(String mqHost) {
        this.mqHost = mqHost;
    }

    public String getTranscodeJobQueue() {
        return transcodeJobQueue;
    }

    public void setTranscodeJobQueue(String transcodeJobQueue) {
        this.transcodeJobQueue = transcodeJobQueue;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
//        transcodeVerticle.setMediaDirectory(mediaDirectory);
//        transcodeVerticle.setMqHost(mqHost);
//        transcodeVerticle.setTranscodeJobQueue(transcodeJobQueue);
        String mqHost = System.getenv("mqhost");
        String transcodeJobQueue = System.getenv("transcodejobqueue");
        String mediaDirectory = System.getenv("mediadirectory");

        RabbitMQClient client = getRabbitMQClient(vertx, mqHost);
        client.start(asyncResult -> {
            if (asyncResult.succeeded()) {
                System.out.println("RabbitMQ successfully connected!");
                client.basicConsumer(transcodeJobQueue, rabbitMQConsumerAsyncResult -> {
                    if (rabbitMQConsumerAsyncResult.succeeded()) {
                        System.out.println("RabbitMQ consumer created !");
                        RabbitMQConsumer mqConsumer = rabbitMQConsumerAsyncResult.result();
                        mqConsumer.handler(message -> {
                            System.out.println("Got message: " + message.body().toString());
                            System.out.println("Transcode job submitted for below request: " +
                                "\n=============================================\n" +
                                message.body().toString());
                            vertx.executeBlocking(promise -> {
                                ObjectMapper obj = new ObjectMapper();
                                TranscodeDTO transcodeJob = null;
                                try {
                                    transcodeJob = obj.readValue(message.body().toString(), TranscodeDTO.class);
                                } catch (JsonProcessingException e) {
                                    e.printStackTrace();
                                }
                                System.out.println(transcodeJob.getId());
                                FFmpeg ffmpeg = null;
                                FFprobe ffprobe = null;
                                try {
                                    ffmpeg = new FFmpeg("/usr/bin/ffmpeg");
                                    ffprobe = new FFprobe("/usr/bin/ffprobe");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                String fullFilePath = mediaDirectory + transcodeJob.getFileId() + "/" + transcodeJob.getFileName();
                                System.out.println("Media file path: "+fullFilePath);
                                FFmpegBuilder builder = new FFmpegBuilder()
                                    .setInput(fullFilePath)     // Filename, or a FFmpegProbeResult
                                    .overrideOutputFiles(true) // Override the output if it exists
                                    .addOutput(mediaDirectory + transcodeJob.getFileId() + "/" + transcodeJob.getOutputFileName())   // Filename for the destination
                                    .setFormat(transcodeJob.getTargetFormat())        // Format is inferred from filename, or can be set
                                    // .setTargetSize(250_000)  // Aim for a 250KB file
                                    .disableSubtitle()       // No subtiles
                                    .setAudioChannels(1)         // Mono audio
                                    .setAudioCodec("aac")        // using the aac codec
                                    .setAudioSampleRate(48_000)  // at 48KHz
                                    .setAudioBitRate(32768)      // at 32 kbit/s
                                    .setVideoCodec("libx264")     // Video using x264
                                    .setVideoFrameRate(24, 1)     // at 24 frames per second
                                    .setVideoResolution(640, 480) // at 640x480 resolution
                                    .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL) // Allow FFmpeg to use experimental specs
                                    .done();
                                FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
                                executor.createJob(builder).run();
                                // Or run a two-pass encode (which is better quality at the cost of being slower)
                                // executor.createTwoPassJob(builder).run();
                                promise.complete();
                            }).onComplete(result -> {
                                System.out.println("Transcode job completed");
                            });

                        });
                    } else {
                        rabbitMQConsumerAsyncResult.cause().printStackTrace();
                    }
                });

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
