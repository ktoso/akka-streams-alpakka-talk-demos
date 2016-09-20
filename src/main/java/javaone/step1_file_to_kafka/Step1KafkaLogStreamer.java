package javaone.step1_file_to_kafka;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.kafka.ProducerSettings;
import akka.kafka.javadsl.Producer;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.contrib.FileTailSource;
import akka.stream.javadsl.Framing;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import scala.concurrent.duration.FiniteDuration;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class Step1KafkaLogStreamer {

  private final static int MAX_CHUNK_SIZE = 1000;
  private final static FiniteDuration POLLING_INTERVAL = FiniteDuration.apply(1L, TimeUnit.MILLISECONDS);


  public static void main(String[] args) {
    if (args.length != 2) {
      throw new IllegalArgumentException("Usage: KafkaLogStreamer logfile topic");
    }

    final FileSystem fs = FileSystems.getDefault();
    final Path logfile = fs.getPath(args[0]);
    final String topic = args[1];

    final ActorSystem system = ActorSystem.create();
    final Materializer materializer = ActorMaterializer.create(system);

    final ProducerSettings<byte[], String> producerSettings = ProducerSettings
      .create(system, new ByteArraySerializer(), new StringSerializer())
      .withBootstrapServers("127.0.0.1:9092");
      //      .withBootstrapServers("192.168.0.12:9092");

    final Source<String, NotUsed> logLines =
      FileTailSource.create(
        logfile,
        MAX_CHUNK_SIZE,
        0, // starting offset
        POLLING_INTERVAL)
      .via(Framing.delimiter(ByteString.fromString("\n"), MAX_CHUNK_SIZE))
      .map(ByteString::utf8String);

    final Sink<ProducerRecord<byte[], String>, CompletionStage<Done>> kafkaSink =
      Producer.plainSink(producerSettings);

    logLines
      .map(line -> new ProducerRecord<byte[], String>(topic, line))
      .to(kafkaSink)
      .run(materializer);

  }

}
