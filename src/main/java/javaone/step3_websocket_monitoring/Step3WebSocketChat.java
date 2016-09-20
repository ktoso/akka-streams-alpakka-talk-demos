package javaone.step3_websocket_monitoring;

import akka.actor.ActorSystem;

import akka.NotUsed;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.model.ws.TextMessage;
import akka.http.javadsl.server.Route;
import akka.japi.Pair;
import akka.stream.*;
import akka.stream.javadsl.*;
import akka.stream.stage.AbstractInHandler;
import akka.stream.stage.AbstractOutHandler;
import akka.stream.stage.GraphStage;
import akka.stream.stage.GraphStageLogic;

import static akka.http.javadsl.server.Directives.*;

public class Step3WebSocketChat {

  public static void main(String[] args) {
    ActorSystem system = ActorSystem.create();
    Materializer materializer = ActorMaterializer.create(system);
    Http http = Http.get(system);

    final Pair<Sink<String, NotUsed>, Source<String, NotUsed>> hubPair =
      MergeHub.of(String.class)
        .toMat(BroadcastHub.of(String.class), Keep.both())
        .run(materializer);

    final Source<String, NotUsed> chatSource = hubPair.second();
    final Sink<String, NotUsed> chatSink = hubPair.first();

    final Flow<Message, Message, NotUsed> chatFlow =
      Flow.of(Message.class)
        .map(message -> message.asTextMessage().getStrictText())
        .via(Flow.fromSinkAndSource(chatSink, chatSource))
        .map(TextMessage::create);

    final Route site =
      path("index", () ->
        get(() ->
          getFromResource("site/index-chat.html")
        ));

    final Route ws =
      path("chat", () ->
        handleWebSocketMessages(chatFlow)
      );

    final Flow<HttpRequest, HttpResponse, NotUsed> handler =
      site.orElse(ws)
        .flow(system, materializer);

    http.bindAndHandle(handler,
      ConnectHttp.toHost("localhost", 8080),
      materializer
    );
    System.out.println("Running at http://localhost:8080");

  }

}
