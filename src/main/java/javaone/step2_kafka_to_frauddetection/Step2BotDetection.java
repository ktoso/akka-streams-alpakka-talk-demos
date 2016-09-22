package javaone.step2_kafka_to_frauddetection;

import akka.actor.ActorSystem;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Source;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import scala.concurrent.duration.FiniteDuration;
import scala.util.Success;
import scala.util.Try;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Step2BotDetection {

  public static void main(String[] args) {
    final String topic = "logs";

    final ActorSystem system = ActorSystem.create();
    final Materializer materializer = ActorMaterializer.create(system);

    final ConsumerSettings<byte[], String> consumerSettings = ConsumerSettings
      .create(system, new ByteArrayDeserializer(), new StringDeserializer())
      .withGroupId("group1")
      .withBootstrapServers("127.0.0.1:9092");

    /*
      bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic test --from-beginning
      
      while [ "1" == "1" ] 
      do
        fortune | head -n1 >> watch-me.log
      done
     */
    
    final Source<ConsumerRecord<byte[], String>, Consumer.Control> kafkaSource = 
      Consumer.plainSource(consumerSettings, Subscriptions.topics(topic))
//        .map(f -> {
//          System.out.println("f = " + f);
//          return f;
//        })
      ;
    
    
    kafkaSource
      .map(record -> parseLine(record.value())) // TODO mention Streaming event push-pull Xml parsing we provide
      .filter(Try::isSuccess) // filter out unparseable
      .map(tweet -> detectBot(tweet.get()))
      .filter(result -> result.isBot)
      .groupedWithin(100, FiniteDuration.create(1, TimeUnit.SECONDS))
      .runForeach(Step2BotDetection::writeFraudReport, materializer);

  }


  public static class TweetFromFile {
    public final String message;
    public TweetFromFile(String message) {
      this.message = message;
    }

    @Override
    public String toString() {
      return "TweetFromFile{" +
        "message='" + message + '\'' +
        '}';
    }
  }
  
  private static class BotDetectionResult {
    public final boolean isBot;
    public final TweetFromFile entry;
    public BotDetectionResult(boolean isBot, TweetFromFile entry) {
      this.isBot = isBot;
      this.entry = entry;
    }

    @Override
    public String toString() {
      return "BotDetectionResult{" +
        "isBot=" + isBot +
        ", entry=" + entry +
        '}';
    }
  }

  private static void writeFraudReport(List<BotDetectionResult> possibleBotTweets) {
    // etc etc
    System.out.println("Detected possible bots within last time-period: " + possibleBotTweets.size());
//    for (BotDetectionResult botTweet: possibleBotTweets) {
//      System.out.println(botTweet.entry);
//    }
  }

  private static BotDetectionResult detectBot(TweetFromFile entry) {
    return new BotDetectionResult(ThreadLocalRandom.current().nextBoolean(), entry);
  }

  private static Try<TweetFromFile> parseLine(String line) {
    return new Success<>(new TweetFromFile(line));
  }

}

/*
background log: info: Detected possible bots within last time-period: 100
background log: info: Detected possible bots within last time-period: 100
background log: info: Detected possible bots within last time-period: 100
background log: info: Detected possible bots within last time-period: 100
background log: info: Detected possible bots within last time-period: 100
background log: info: Detected possible bots within last time-period: 100
background log: info: Detected possible bots within last time-period: 21
background log: info: Detected possible bots within last time-period: 18
background log: info: Detected possible bots within last time-period: 42
background log: info: Detected possible bots within last time-period: 95
background log: info: Detected possible bots within last time-period: 57
background log: info: Detected possible bots within last time-period: 34
 */
