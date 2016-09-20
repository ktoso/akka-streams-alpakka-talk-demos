package javaone.step2_kafka_to_frauddetection;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import akka.stream.scaladsl.xml.Xml;
import akka.util.ByteString;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import scala.concurrent.duration.FiniteDuration;
import scala.util.Failure;
import scala.util.Success;
import scala.util.Try;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Step2FraudDetection {

  private final static int MAX_CHUNK_SIZE = 1000;
  private final static FiniteDuration POLLING_INTERVAL = FiniteDuration.apply(1L, TimeUnit.MILLISECONDS);

  public static void main(String[] args) {
    if (args.length != 1) {
      throw new IllegalArgumentException("Usage: KafkaLogConsumer topic");
    }

    final String topic = args[0];

    final ActorSystem system = ActorSystem.create();
    final Materializer materializer = ActorMaterializer.create(system);

    final ConsumerSettings<byte[], String> consumerSettings = ConsumerSettings
      .create(system, new ByteArrayDeserializer(), new StringDeserializer())
      .withGroupId("group1")
      .withBootstrapServers("localhost:9092");

    Consumer.plainSource(consumerSettings, Subscriptions.topics(topic))
      .map(record -> parseLine(record.value())) // TODO mention Streaming event push-pull Xml parsing we provide
      .filter(Try::isSuccess) // filter out unparseable
      .map(fraudulent -> detectFraud(fraudulent.get()))
      .filter(result -> result.isFraudulent)
      .groupedWithin(100, FiniteDuration.create(1, TimeUnit.MINUTES))
      .runForeach(Step2FraudDetection::writeFraudReport, materializer);

  }


  public static class Entry {
    public final String timestamp;
    public final String host;
    public final String source;
    public final String message;
    public Entry(String timestamp, String host, String source, String message) {
      this.timestamp = timestamp;
      this.host = host;
      this.source = source;
      this.message = message;
    }
  }
  
  private static class FraudDetectionResult {
    public final boolean isFraudulent;
    public final Entry entry;
    public FraudDetectionResult(boolean isFraudulent, Entry entry) {
      this.isFraudulent = isFraudulent;
      this.entry = entry;
    }
  }

  private static void writeFraudReport(List<FraudDetectionResult> possibleFrauds) {
    // etc etc
    System.out.println("Detected possible frauds: ");
    for (FraudDetectionResult fraud: possibleFrauds) {
      System.out.println(fraud.entry);
    }
  }

  private static FraudDetectionResult detectFraud(Entry entry) {
    return new FraudDetectionResult(false, entry);
  }

  private static Try<Entry> parseLine(String line) {
    try {
      final Pattern pattern = Pattern.compile("([ .]{15}) (.+) (.+) ([ .]*)");

      final Matcher matcher = pattern.matcher(line);
      if (matcher.find()) {
        return new Success<>(new Entry(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4)));
       } else {
        return new Failure<>(new IllegalArgumentException("Entry format not recognized: " + line));
      }

    } catch (Exception ex) {
      return new Failure<>(ex);
    }
  }

}
