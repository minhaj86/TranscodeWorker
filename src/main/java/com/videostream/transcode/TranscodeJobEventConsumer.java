package com.videostream.transcode;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFmpegUtils;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.progress.Progress;
import net.bramp.ffmpeg.progress.ProgressListener;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TranscodeJobEventConsumer extends AbstractVerticle {
    final static Logger logger = LoggerFactory.getLogger(TranscodeJobEventConsumer.class);

    private String mediaDirectory;

    @Override
    public void start() throws Exception {
        this.mediaDirectory = System.getenv("mediadirectory");
        vertx.eventBus().<TranscodeJobDTO>consumer("transcodejob", transcodeJobEvent -> {
            logger.info("Event received: ================================================== "+transcodeJobEvent.body());
            WorkerExecutor executor = vertx.createSharedWorkerExecutor("transcode-job-worker-pool");
            executor.executeBlocking(promise -> {
                TranscodeJobDTO transcodeJob = transcodeJobEvent.body();
                FFmpeg ffmpeg = null;
                FFprobe ffprobe = null;
                try {
                    ffmpeg = new FFmpeg("/usr/bin/ffmpeg");
                    ffprobe = new FFprobe("/usr/bin/ffprobe");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String fullFilePath = mediaDirectory + transcodeJob.getFileId() + "/" + transcodeJob.getFileName();
                logger.info("Media file path: "+fullFilePath);
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
                FFmpegProbeResult in = null;
                try {
                    in = ffprobe.probe(fullFilePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                final FFmpegProbeResult fin = in;

                FFmpegJob ffmpegJob = ffmpegExecutor.createJob(builder, new ProgressListener() {
                    final double duration_ns = fin.getFormat().duration * TimeUnit.SECONDS.toNanos(1);

                    @Override
                    public void progress(Progress progress) {
                        double percentage = progress.out_time_ns / duration_ns;

                        // Print out interesting information about the progress
                        logger.info(String.format(
                            "[%.0f%%] status:%s frame:%d time:%s ms fps:%.0f speed:%.2fx",
                            percentage * 100,
                            progress.status,
                            progress.frame,
                            FFmpegUtils.toTimecode(progress.out_time_ns, TimeUnit.NANOSECONDS),
                            progress.fps.doubleValue(),
                            progress.speed
                        ));
                    }
                });
                ffmpegJob.run();
                // Or run a two-pass encode (which is better quality at the cost of being slower)
                // executor.createTwoPassJob(builder).run();
                promise.complete("Done");
            }, asyncResult -> {
                logger.info("The result is: " + asyncResult.result());
            });
            transcodeJobEvent.reply(transcodeJobEvent.body());
        });
    }
}

