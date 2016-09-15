package samples.scalaexchange.step5

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonStreamingSupport
import akka.http.scaladsl.server.directives.EntityStreamingDirectives
import akka.http.scaladsl.server.{Directives, JsonSourceRenderingMode}
import akka.stream.{Attributes, ActorMaterializer}
import akka.stream.scaladsl.{Keep, RunnableGraph, Sink, Source}
import samples.scalaexchange.utils.MakingUpData
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.Future

trait TweetsStreamService extends Directives
  with SprayJsonStreamingSupport
  with EntityStreamingDirectives // we'll be able to stream any content (csv, your custom format, anything)
  with MyModelJsonProtocol
  with MakingUpData {

  implicit def system: ActorSystem
  implicit def materializer: ActorMaterializer

  implicit val jsonRenderingMode = JsonSourceRenderingMode.LineByLine

  val randomTweets = Source.fromIterator(() => Iterator.continually(randomTweet()))

  def tweetsStreamRoutes =
    path("tweets") {
      get {
        complete {
          randomTweets
            .log("tweets").withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel)) // TODO explain attributes
            .conflate {  (t1, t2) => t1 }
            // TODO show netstat
            // TODO content type negotiation => csv response is requested
        }
      }
    }

  private def randomTweet(): Tweet =
    Tweet(shortName(), lipsum())
}

final case class Tweet(nickname: String, tweet: String)

trait MyModelJsonProtocol extends DefaultJsonProtocol {
  implicit val tweetFormat: RootJsonFormat[Tweet] = jsonFormat2(Tweet)
}
