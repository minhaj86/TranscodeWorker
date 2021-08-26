package com.videostream.transcode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.WorkerExecutor;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

import java.io.IOException;

public class EventConsumerVerticle extends AbstractVerticle {

    private String mediaDirectory;

    @Override
    public void start() throws Exception {
        this.mediaDirectory = System.getenv("mediadirectory");
        vertx.eventBus().<String>consumer("transcodejob", msg -> {
            System.out.println("Event received: \n==================================================\n"+msg.body());
            WorkerExecutor executor = vertx.createSharedWorkerExecutor("my-worker-pool");
            executor.executeBlocking(promise -> {
                // Call some blocking API that takes a significant amount of time to return
                ObjectMapper obj = new ObjectMapper();
                TranscodeDTO transcodeJob = null;
                try {
                    transcodeJob = obj.readValue(msg.body(), TranscodeDTO.class);
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
                FFmpegExecutor ffmpegExecutor = new FFmpegExecutor(ffmpeg, ffprobe);
                ffmpegExecutor.createJob(builder).run();
                // Or run a two-pass encode (which is better quality at the cost of being slower)
                // executor.createTwoPassJob(builder).run();
                promise.complete("Done");
            }, res -> {
                System.out.println("The result is: " + res.result());
            });
            msg.reply("asdfasfasdf");
        });
    }
}

