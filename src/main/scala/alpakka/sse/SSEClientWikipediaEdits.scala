package alpakka.sse

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import play.api.libs.json._

import scala.sys.process._

/**
  * Just because we can :-)
  * Consume the WikipediaEdits stream which is implemented with SSE - see:
  * https://wikitech.wikimedia.org/wiki/EventStreams
  * https://www.matthowlett.com/2017-12-23-exploring-wikipedia-ksql.html
  *
  */
object SSEClientWikipediaEdits {
  implicit val system = ActorSystem("SSEClientWikipediaEdits")
  implicit val executionContext = system.dispatcher
  implicit val materializerServer = ActorMaterializer()

  def main(args: Array[String]) {
    browserClient()
    sseClient()
  }

  private def browserClient() = {
    //Open the default os browser with the html page
    //Chrome is able to consume the stream directly, that is the URL can be pasted
    val os = System.getProperty("os.name").toLowerCase
    if (os == "mac os x") "open ./src/main/scala/alpakka/sse/index.html".!
  }


  private def sseClient() = {

    import akka.http.scaladsl.unmarshalling.sse.EventStreamUnmarshalling._

    Http()
      .singleRequest(HttpRequest(
        uri = "https://stream.wikimedia.org/v2/stream/recentchange"
      ))
      .flatMap(Unmarshal(_).to[Source[ServerSentEvent, NotUsed]])
      .foreach {
        _.runForeach {
          event: ServerSentEvent => {
            val server_name = (Json.parse(event.data) \ "server_name").as[String]
            val user = (Json.parse(event.data) \ "user").as[String]
            println(s"Change on server: $server_name by: $user")
          }
        }
      }
  }
}