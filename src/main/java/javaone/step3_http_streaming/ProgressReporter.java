package javaone.step3_http_streaming;

import akka.NotUsed;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class ProgressReporter {

  private int progress = 0 /*percent*/;
  
  private final String id;
  private final Sink<String, NotUsed> notificationsSink;
  private final Materializer materializer;

  public ProgressReporter(String id, Sink<String, NotUsed> notificationsSink, Materializer materializer) {
    this.id = id;
    this.notificationsSink = notificationsSink;
    this.materializer = materializer;
  }
  
  public static void reportTo(String id, int intervalMillis, Sink<String, NotUsed> notificationsSink, Materializer materializer) {
    new ProgressReporter(id, notificationsSink, materializer).start(intervalMillis);
  }

  private void start(int intervalMillis) {
    System.out.println("intervalMillis = " + intervalMillis);
    final FiniteDuration delay = FiniteDuration.create(intervalMillis, TimeUnit.MILLISECONDS);
    final FiniteDuration interval = FiniteDuration.create(intervalMillis, TimeUnit.MILLISECONDS);
    
    Source.tick(delay, interval, "tick")
      .map(t -> makeProgress())
      .take(100) // FIXME, a takeWhile that emits the last element as well
      .alsoTo(Sink.onComplete(done -> System.out.println("done = " + done)))
      .runWith(notificationsSink, materializer);
  }

  private String progress() {
    if (progress > 100) return "100%";
    else return progress + "%";
  }
  
  private String makeProgress() {
    progress += ThreadLocalRandom.current().nextInt(5);
    
    if (progress >= 100) 
      return String.format("{\"id\":\"%s\", \"progress\":\"%s\", \"complete\":true}\n", id, progress(), progress > 100);
    else 
      return String.format("{\"id\":\"%s\", \"progress\":\"%s\"}\n", id, progress(), progress > 100);
  }


}
