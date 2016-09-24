package javaone.step3_http_streaming;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.model.ws.TextMessage;
import akka.http.javadsl.server.Route;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.*;

import java.util.concurrent.ThreadLocalRandom;

import static akka.http.javadsl.server.Directives.*;

public class Step3Monitoring {

  public static void main(String[] args) {
    ActorSystem system = ActorSystem.create();
    Materializer materializer = ActorMaterializer.create(system);
    Http http = Http.get(system);

    // dynamic *shared* fan-in fan-out's
    final Source<String, Sink<String, NotUsed>> mergeHub = MergeHub.of(String.class);
    final Sink<String, Source<String, NotUsed>> broadcastHub = BroadcastHub.of(String.class);
    
    final Pair<Sink<String, NotUsed>, Source<String, NotUsed>> hubPair =
      mergeHub.toMat(broadcastHub, Keep.both()).run(materializer);
    final Sink<String, NotUsed> progressIn = hubPair.first();
    final Source<String, NotUsed> progressSource = hubPair.second();

    // --- report progress of multiple jobs ---
    for (int i = 0; i < 10; i++) {
      final String jobId = "job-00" + i;
      System.out.println("Starting job: " + jobId);
      
      
      
      ProgressReporter.reportTo(jobId, 
        ThreadLocalRandom.current().nextInt(500, 2000), progressIn, materializer);
    }

    final Flow<Message, Message, NotUsed> progressFlow =
      Flow.of(Message.class)
        .map(message -> message.asTextMessage().getStrictText())
        .via(Flow.fromSinkAndSource(Sink.ignore(), progressSource))
        .map(t -> (Message) TextMessage.create(t));

    final Route site =
      path("index", () -> getFromResource("site/index-progress.html"));

    final Route ws =
      path("progress", () -> handleWebSocketMessages(progressFlow));

    final Flow<HttpRequest, HttpResponse, NotUsed> handler =
      site.orElse(ws)
        .flow(system, materializer);

    http.bindAndHandle(handler,
      ConnectHttp.toHost("localhost", 8080),
      materializer
    );
    System.out.println("Running at http://localhost:8080/index");

  }

}
