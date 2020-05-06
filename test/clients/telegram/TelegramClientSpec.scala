package clients.telegram

import cats.effect.{ContextShift, IO, Resource}
import domain.ApiClientError._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import resources.SttpBackendResource
import sttp.client
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client.testing.SttpBackendStub
import sttp.client.{NothingT, Response, SttpBackend}
import sttp.model.{Method, StatusCode}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class TelegramClientSpec extends PlaySpec with ScalaFutures {
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.Implicits.global)

  val telegramConfig        = Map("baseUri"            -> "http://telegram.com", "botKey" -> "BOT-KEY", "mainChannelId" -> "m1", "secondaryChannelId" -> "s1")
  val config: Configuration = Configuration("telegram" -> telegramConfig)

  val message = "lorem ipsum dolor sit amet"

  "TelegramClient" should {

    "send message to the main channel" in {
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isGoingTo(r, Method.GET, "telegram.com", List("botBOT-KEY", "sendMessage"), Map("chat_id" -> "m1", "text" -> message)) =>
            Response.ok("success")
          case _ => throw new RuntimeException()
        }

      val telegramClient = new TelegramClient(config, sttpCatsBackend(testingBackend))

      val result = telegramClient.sendMessageToMainChannel(message)

      whenReady(result.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { sent =>
        sent must be(())
      }
    }

    "send message to the channel" in {
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isGoingTo(r, Method.GET, "telegram.com", List("botBOT-KEY", "sendMessage"), Map("chat_id" -> "m1", "text" -> message)) =>
            Response.ok("success")
          case _ => throw new RuntimeException()
        }

      val telegramClient = new TelegramClient(config, sttpCatsBackend(testingBackend))

      val result = telegramClient.sendMessageToMainChannel(message)

      whenReady(result.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { sent =>
        sent must be(())
      }
    }

    "return error when not success" in {
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isGoingTo(r, Method.GET, "telegram.com", List("botBOT-KEY", "sendMessage"), Map("chat_id" -> "m1", "text" -> message)) =>
            Response.apply("fail", StatusCode.BadRequest)
          case _ => throw new RuntimeException()
        }

      val telegramClient = new TelegramClient(config, sttpCatsBackend(testingBackend))

      val result = telegramClient.sendMessageToMainChannel(message)

      whenReady(result.attempt.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { sent =>
        sent must be(Left(HttpError(400, "error sending message to telegram channel m1: 400")))
      }
    }
  }

  def isGoingTo(
      req: client.Request[_, _],
      method: Method,
      host: String,
      paths: Seq[String] = Nil,
      params: Map[String, String] = Map.empty
  ): Boolean =
    req.uri.host == host && (paths.isEmpty || req.uri.path == paths) && req.method == method && req.uri.params.toMap
      .toSet[(String, String)]
      .subsetOf(params.toSet)

  def sttpCatsBackend(testingBackend: SttpBackendStub[IO, Nothing]): SttpBackendResource[IO] = new SttpBackendResource[IO] {
    override val get: Resource[IO, SttpBackend[IO, Nothing, NothingT]] =
      Resource.liftF(IO.pure(testingBackend))
  }
}
