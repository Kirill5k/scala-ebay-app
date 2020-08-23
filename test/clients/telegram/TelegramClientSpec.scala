package clients.telegram

import cats.effect.IO
import clients.SttpClientSpec
import common.errors.ApiClientError.HttpError
import play.api.Configuration
import sttp.client.Response
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client.testing.SttpBackendStub
import sttp.model.{Method, StatusCode}

import scala.concurrent.duration._
import scala.language.postfixOps

class TelegramClientSpec extends SttpClientSpec {

  val message = "lorem ipsum dolor sit amet"

  "TelegramClient" should {

    "send message to the main channel" in {
      val testingBackend: SttpBackendStub[IO, Nothing, WebSocketHandler] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r
              if isGoingTo(r, Method.GET, "telegram.com", List("botBOT-KEY", "sendMessage"), Map("chat_id" -> "m1", "text" -> message)) =>
            Response.ok("success")
          case _ => throw new RuntimeException()
        }

      val telegramClient = new TelegramClient(sttpCatsBackend(testingBackend))

      val result = telegramClient.sendMessageToMainChannel(message)

      result.unsafeToFuture().map(_ must be(()))
    }

    "send message to the secondary channel" in {
      val testingBackend: SttpBackendStub[IO, Nothing, WebSocketHandler] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r
            if isGoingTo(r, Method.GET, "telegram.com", List("botBOT-KEY", "sendMessage"), Map("chat_id" -> "m2", "text" -> message)) =>
            Response.ok("success")
          case _ => throw new RuntimeException()
        }

      val telegramClient = new TelegramClient(sttpCatsBackend(testingBackend))

      val result = telegramClient.sendMessageToSecondaryChannel(message)

      result.unsafeToFuture().map(_ must be(()))
    }

    "send message to the channel" in {
      val testingBackend: SttpBackendStub[IO, Nothing, WebSocketHandler] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r
              if isGoingTo(r, Method.GET, "telegram.com", List("botBOT-KEY", "sendMessage"), Map("chat_id" -> "m1", "text" -> message)) =>
            Response.ok("success")
          case _ => throw new RuntimeException()
        }

      val telegramClient = new TelegramClient(sttpCatsBackend(testingBackend))

      val result = telegramClient.sendMessageToMainChannel(message)

      result.unsafeToFuture().map(_ must be(()))
    }

    "return error when not success" in {
      val testingBackend: SttpBackendStub[IO, Nothing, WebSocketHandler] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r
              if isGoingTo(r, Method.GET, "telegram.com", List("botBOT-KEY", "sendMessage"), Map("chat_id" -> "m1", "text" -> message)) =>
            Response("fail", StatusCode.BadRequest)
          case _ => throw new RuntimeException()
        }

      val telegramClient = new TelegramClient(sttpCatsBackend(testingBackend))

      val result = telegramClient.sendMessageToMainChannel(message)

      result.attempt.unsafeToFuture().map(_ must be(Left(HttpError(400, "error sending message to telegram channel m1: 400"))))
    }
  }
}
