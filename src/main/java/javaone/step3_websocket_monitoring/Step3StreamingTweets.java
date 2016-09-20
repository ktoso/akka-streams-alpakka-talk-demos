package javaone.step3_websocket_monitoring;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.common.EntityStreamingSupport;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.model.ws.TextMessage;
import akka.http.javadsl.server.Route;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.*;

import static akka.http.javadsl.server.Directives.*;

public class Step3StreamingTweets {

  public static void main(String[] args) {
    ActorSystem system = ActorSystem.create();
    Materializer materializer = ActorMaterializer.create(system);
    Http http = Http.get(system);

    
    final Source<Tweet, NotUsed> tweets = Source.repeat(new Tweet("Hello world"));

    final Route tweetsRoute =
      path("tweets", () ->
        completeWithSource(tweets, Jackson.marshaller(), EntityStreamingSupport.json())
      );

    
    final Flow<HttpRequest, HttpResponse, NotUsed> handler =
      tweetsRoute.flow(system, materializer);

    http.bindAndHandle(handler,
      ConnectHttp.toHost("localhost", 8080),
      materializer
    );
    System.out.println("Running at http://localhost:8080");

  }

}

class Tweet {
  final String text;

  public Tweet() {
    text = "";
  }
  public Tweet(String text) {
    this.text = text;
  }

  public String getText() {
    return text;
  }
}
