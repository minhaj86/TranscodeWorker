package com.videostream.transcode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

import java.io.IOException;

public class TranscodeVerticle  extends AbstractVerticle {
  String requestJson = null;
  private String mediaDirectory;

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

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    VertxOptions options = new VertxOptions();
    options.setBlockedThreadCheckInterval(300L*1000*1000*1000);
    Vertx.vertx(options).executeBlocking(promise -> {

//    });
//
//    vertx.executeBlocking(promise -> {
      ObjectMapper obj = new ObjectMapper();
      TranscodeDTO transcodeJob = null;
      try {
        transcodeJob = obj.readValue(this.requestJson, TranscodeDTO.class);
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
      FFmpegBuilder builder = new FFmpegBuilder()
        .setInput(mediaDirectory + transcodeJob.getFileId() + "/" + transcodeJob.getFileName())     // Filename, or a FFmpegProbeResult
        .overrideOutputFiles(true) // Override the output if it exists
        .addOutput(mediaDirectory + transcodeJob.getFileId() + "/" + transcodeJob.getOutputFileName())   // Filename for the destination
        .setFormat(transcodeJob.getTargetFormat())        // Format is inferred from filename, or can be set
//                .setTargetSize(250_000)  // Aim for a 250KB file
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
      promise.complete();
    }).onComplete(result -> {
      System.out.println("Transcode job completed");
    });

  }
}
