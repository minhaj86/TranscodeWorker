package com.videostream.transcode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Address;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.QueueOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQConsumer;
import io.vertx.rabbitmq.RabbitMQMessage;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.rabbitmq.RabbitMQPublisher;
import io.vertx.rabbitmq.RabbitMQPublisherOptions;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class MQUtils {

  public static void createClientWithUri(Vertx vertx) {
    RabbitMQOptions config = new RabbitMQOptions();
    // full amqp uri
    config.setUri("amqp://xvjvsrrc:VbuL1atClKt7zVNQha0bnnScbNvGiqgb@moose.rmq.cloudamqp.com/xvjvsrrc");
    RabbitMQClient client = RabbitMQClient.create(vertx, config);

    // Connect
    client.start(asyncResult -> {
      if (asyncResult.succeeded()) {
        System.out.println("RabbitMQ successfully connected!");
      } else {
        System.out.println("Fail to connect to RabbitMQ " + asyncResult.cause().getMessage());
      }
    });
  }

  public static void createClientWithManualParams(Vertx vertx) {
    RabbitMQOptions config = new RabbitMQOptions();
    // Each parameter is optional
    // The default parameter with be used if the parameter is not set
//    config.setUser("user1");
//    config.setPassword("password1");
    config.setHost("localhost");
    config.setPort(5672);
//    config.setVirtualHost("vhost1");
    config.setConnectionTimeout(6000); // in milliseconds
    config.setRequestedHeartbeat(60); // in seconds
    config.setHandshakeTimeout(6000); // in milliseconds
    config.setRequestedChannelMax(5);
    config.setNetworkRecoveryInterval(500); // in milliseconds
    config.setAutomaticRecoveryEnabled(true);

    RabbitMQClient client = RabbitMQClient.create(vertx, config);

    // Connect
    client.start(asyncResult -> {
      if (asyncResult.succeeded()) {
        System.out.println("RabbitMQ successfully connected!");
      } else {
        System.out.println("Fail to connect to RabbitMQ " + asyncResult.cause().getMessage());
      }
    });
  }

  //pass multiple hosts to client, allow to use a clustered RabbitMQ
  public static void createClientWithMultipleHost(Vertx vertx) {
    RabbitMQOptions config = new RabbitMQOptions();
    config.setUser("user1");
    config.setPassword("password1");
    config.setVirtualHost("vhost1");

    config.setAddresses(Arrays.asList(Address.parseAddresses("firstHost,secondHost:5672")));

    RabbitMQClient client = RabbitMQClient.create(vertx, config);

    // Connect
    client.start(asyncResult -> {
      if (asyncResult.succeeded()) {
        System.out.println("RabbitMQ successfully connected!");
      } else {
        System.out.println("Fail to connect to RabbitMQ " + asyncResult.cause().getMessage());
      }
    });
  }

  public static void basicPublish(RabbitMQClient client) {
    Buffer message = Buffer.buffer("body", "UTF8");
    client.basicPublish("", "my.queue", message, pubResult -> {
      if (pubResult.succeeded()) {
        System.out.println("Message published !");
      } else {
        pubResult.cause().printStackTrace();
      }
    });
  }

  public static void basicPublishWithConfirm(RabbitMQClient client) {
    Buffer message = Buffer.buffer("body", "Hello RabbitMQ, from Vert.x !");

    // Put the channel in confirm mode. This can be done once at init.
    client.confirmSelect(confirmResult -> {
      if(confirmResult.succeeded()) {
        client.basicPublish("", "my.queue", message, pubResult -> {
          if (pubResult.succeeded()) {
            // Check the message got confirmed by the broker.
            client.waitForConfirms(waitResult -> {
              if(waitResult.succeeded())
                System.out.println("Message published !");
              else
                waitResult.cause().printStackTrace();
            });
          } else {
            pubResult.cause().printStackTrace();
          }
        });
      } else {
        confirmResult.cause().printStackTrace();
      }
    });

  }

  public static void basicConsumer(Vertx vertx, RabbitMQClient client, String transcodeJobQueue, String mediaDirectory) {
    client.basicConsumer(transcodeJobQueue, rabbitMQConsumerAsyncResult -> {
      if (rabbitMQConsumerAsyncResult.succeeded()) {
        System.out.println("RabbitMQ consumer created !");
        RabbitMQConsumer mqConsumer = rabbitMQConsumerAsyncResult.result();
        mqConsumer.handler(message -> {
          DeploymentOptions options = new DeploymentOptions();
          options.setMaxWorkerExecuteTime(600L *1000*1000*1000);
          options.setWorker(true);
          TranscodeVerticle transcodeVerticle = new TranscodeVerticle();
          transcodeVerticle.setRequestJson(message.body().toString());
          transcodeVerticle.setMediaDirectory(mediaDirectory);
          vertx.deployVerticle(transcodeVerticle, options);

          System.out.println("Got message: " + message.body().toString());
//          ObjectMapper obj = new ObjectMapper();
//          TranscodeDTO transcodeJob = null;
//          try {
//             transcodeJob = obj.readValue(message.body().toString(), TranscodeDTO.class);
//          } catch (JsonProcessingException e) {
//            e.printStackTrace();
//          }
//          System.out.println(transcodeJob.getId());
//          TranscodeDTO finalTranscodeJob = transcodeJob;
//          vertx.executeBlocking(promise -> {
//            FFmpeg ffmpeg = null;
//            FFprobe ffprobe = null;
//            try {
//              ffmpeg = new FFmpeg("/usr/bin/ffmpeg");
//              ffprobe = new FFprobe("/usr/bin/ffprobe");
//            } catch (IOException e) {
//              e.printStackTrace();
//            }
//            FFmpegBuilder builder = new FFmpegBuilder()
//              .setInput(mediaDirectory + finalTranscodeJob.getFileId() + "/" + finalTranscodeJob.getFileName())     // Filename, or a FFmpegProbeResult
//              .overrideOutputFiles(true) // Override the output if it exists
//              .addOutput(mediaDirectory + finalTranscodeJob.getFileId() + "/" + finalTranscodeJob.getOutputFileName())   // Filename for the destination
//              .setFormat(finalTranscodeJob.getTargetFormat())        // Format is inferred from filename, or can be set
////                .setTargetSize(250_000)  // Aim for a 250KB file
//              .disableSubtitle()       // No subtiles
//              .setAudioChannels(1)         // Mono audio
//              .setAudioCodec("aac")        // using the aac codec
//              .setAudioSampleRate(48_000)  // at 48KHz
//              .setAudioBitRate(32768)      // at 32 kbit/s
//              .setVideoCodec("libx264")     // Video using x264
//              .setVideoFrameRate(24, 1)     // at 24 frames per second
//              .setVideoResolution(640, 480) // at 640x480 resolution
//              .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL) // Allow FFmpeg to use experimental specs
//              .done();
//
//            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
//
//            // Run a one-pass encode
//            executor.createJob(builder).run();
//            promise.complete();
//
//          }).onComplete(res -> {
//            System.out.println("Transcode job done");
//          });


//          Or run a two-pass encode (which is better quality at the cost of being slower)
//          executor.createTwoPassJob(builder).run();
            System.out.println("Transcode job submitted for below request: " +
              "\n=============================================\n"+
              message.body().toString());

        });
      } else {
        rabbitMQConsumerAsyncResult.cause().printStackTrace();
      }
    });
  }

  public static void basicConsumerOptions(Vertx vertx, RabbitMQClient client) {
    QueueOptions options = new QueueOptions()
      .setMaxInternalQueueSize(1000)
      .setKeepMostRecent(true);

    client.basicConsumer("my.queue", options, rabbitMQConsumerAsyncResult -> {
      if (rabbitMQConsumerAsyncResult.succeeded()) {
        System.out.println("RabbitMQ consumer created !");
      } else {
        rabbitMQConsumerAsyncResult.cause().printStackTrace();
      }
    });
  }

  public static void pauseAndResumeConsumer(RabbitMQConsumer consumer){
    consumer.pause();
    consumer.resume();
  }

  public static void endHandlerConsumer(RabbitMQConsumer rabbitMQConsumer) {
    rabbitMQConsumer.endHandler(v -> {
      System.out.println("It is the end of the stream");
    });
  }

  public static void cancelConsumer(RabbitMQConsumer rabbitMQConsumer) {
    rabbitMQConsumer.cancel(cancelResult -> {
      if (cancelResult.succeeded()) {
        System.out.println("Consumption successfully stopped");
      } else {
        System.out.println("Tired in attempt to stop consumption");
        cancelResult.cause().printStackTrace();
      }
    });
  }

  public static void exceptionHandler(RabbitMQConsumer consumer) {
    consumer.exceptionHandler(e -> {
      System.out.println("An exception occurred in the process of message handling");
      e.printStackTrace();
    });
  }

  public static void consumerTag(RabbitMQConsumer consumer) {
    String consumerTag = consumer.consumerTag();
    System.out.println("Consumer tag is: " + consumerTag);
  }

  public static void getMessage(RabbitMQClient client) {
    client.basicGet("queue1", true, getResult -> {
      if (getResult.succeeded()) {
        RabbitMQMessage msg = getResult.result();
        System.out.println("Got message: " + msg.body());
      } else {
        System.out.println("Error getting message: ");
        getResult.cause().printStackTrace();
      }
    });

  }

  //pass the additional config for the exchange as JSON, check RabbitMQ documentation for specific config parameters
  public static void exchangeDeclareWithConfig(RabbitMQClient client) {
    JsonObject config = new JsonObject();
    config.put("x-dead-letter-exchange", "my.deadletter.exchange");
    config.put("alternate-exchange", "my.alternate.exchange");
    client.exchangeDeclare("my.exchange", "fanout", true, false, config, onResult -> {
      if (onResult.succeeded()) {
        System.out.println("Exchange successfully declared with config");
      } else {
        onResult.cause().printStackTrace();
      }
    });
  }

  public static void consumeWithManualAck(Vertx vertx, RabbitMQClient client) {
    client.basicConsumer("my.queue", new QueueOptions().setAutoAck(false), consumeResult -> {
      if (consumeResult.succeeded()) {
        System.out.println("RabbitMQ consumer created !");
        RabbitMQConsumer consumer = consumeResult.result();

        // Set the handler which messages will be sent to
        consumer.handler(msg -> {
          JsonObject json = (JsonObject) msg.body();
          System.out.println("Got message: " + json.getString("body"));
          // ack
          client.basicAck(json.getLong("deliveryTag"), false, asyncResult -> {
          });
        });
      } else {
        consumeResult.cause().printStackTrace();
      }
    });
  }

  //pass the additional config for the queue as JSON, check RabbitMQ documentation for specific config parameters
  public static void queueDeclareWithConfig(RabbitMQClient client) {
    JsonObject config = new JsonObject();
    config.put("x-message-ttl", 10_000L);

    client.queueDeclare("my-queue", true, false, true, config, queueResult -> {
      if (queueResult.succeeded()) {
        System.out.println("Queue declared!");
      } else {
        System.err.println("Queue failed to be declared!");
        queueResult.cause().printStackTrace();
      }
    });
  }

  // Use the connectionEstablishedCallback to declare an Exchange
  public static void connectionEstablishedCallback(Vertx vertx, RabbitMQOptions config) {
    RabbitMQClient client = RabbitMQClient.create(vertx, config);
    client.addConnectionEstablishedCallback(promise -> {
      client.exchangeDeclare("exchange", "fanout", true, false)
        .compose(v -> {
          return client.queueDeclare("queue", false, true, true);
        })
        .compose(declareOk -> {
          return client.queueBind(declareOk.getQueue(), "exchange", "");
        })
        .onComplete(promise);
    });

    // At this point the exchange, queue and binding will have been declared even if the client connects to a new server
    client.basicConsumer("queue", rabbitMQConsumerAsyncResult -> {
    });
  }

  public static void connectionEstablishedCallbackForServerNamedAutoDeleteQueue(Vertx vertx, RabbitMQOptions config) {
    RabbitMQClient client = RabbitMQClient.create(vertx, config);
    AtomicReference<RabbitMQConsumer> consumer = new AtomicReference<>();
    AtomicReference<String> queueName = new AtomicReference<>();
    client.addConnectionEstablishedCallback(promise -> {
      client.exchangeDeclare("exchange", "fanout", true, false)
        .compose(v -> client.queueDeclare("", false, true, true))
        .compose(dok -> {
          queueName.set(dok.getQueue());
          // The first time this runs there will be no existing consumer
          // on subsequent connections the consumer needs to be update with the new queue name
          RabbitMQConsumer currentConsumer = consumer.get();
          if (currentConsumer != null) {
            currentConsumer.setQueueName(queueName.get());
          }
          return client.queueBind(queueName.get(), "exchange", "");
        })
        .onComplete(promise);
    });

    client.start()
      .onSuccess(v -> {
        // At this point the exchange, queue and binding will have been declared even if the client connects to a new server
        client.basicConsumer(queueName.get(), rabbitMQConsumerAsyncResult -> {
          if (rabbitMQConsumerAsyncResult.succeeded()) {
            consumer.set(rabbitMQConsumerAsyncResult.result());
          }
        });
      })
      .onFailure(ex -> {
        System.out.println("It went wrong: " + ex.getMessage());
      });
  }

  public static void rabbitMqPublisher(Vertx vertx,
                                       RabbitMQClient client,
                                       RabbitMQPublisherOptions options,
                                       Map<String, JsonObject> messages) {
    RabbitMQPublisher publisher = RabbitMQPublisher.create(vertx, client, options);
    messages.forEach((k,v) -> {
      com.rabbitmq.client.BasicProperties properties = new AMQP.BasicProperties.Builder()
        .messageId(k)
        .build();
      publisher.publish("exchange", "routingKey", properties, v.toBuffer());
    });

    publisher.getConfirmationStream().handler(conf -> {
      if (conf.isSucceeded()) {
        messages.remove(conf.getMessageId());
      }
    });
  }


}


