package clients.telegram

import domain.ApiClientError._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Results
import play.api.routing.sird._
import play.api.test.WsTestClient
import play.api.Configuration
import play.core.server.Server


class TelegramClientSpec extends PlaySpec with ScalaFutures {
  import scala.concurrent.ExecutionContext.Implicits.global

  val telegramConfig = Map("baseUri" -> "/telegram", "messagePath" -> "/message", "mainChannelId" -> "m1", "secondaryChannelId" -> "s1")
  val config: Configuration = Configuration("telegram" -> telegramConfig)

  val message = "lorem ipsum dolor sit amet"

  "TelegramClient" should {

    "send message to the main channel" in {
      withTelegramClient(200, "m1") { telegramClient =>
        val result = telegramClient.sendMessageToMainChannel(message)

        whenReady(result.unsafeToFuture()) { sent =>
          sent must be (())
        }
      }
    }

    "send message to the channel" in {
      withTelegramClient(200, "m1") { telegramClient =>
        val result = telegramClient.sendMessage("m1", message)

        whenReady(result.unsafeToFuture()) { sent =>
          sent must be (())
        }
      }
    }

    "return error when not success" in {
      withTelegramClient(400, "m1") { telegramClient =>
        val result = telegramClient.sendMessage("m1", message)

        whenReady(result.unsafeToFuture()) { sent =>
          sent must be (HttpError(400, "error sending message to telegram channel m1: 400"))
        }
      }
    }
  }

  def withTelegramClient[T](status: Int, channel: String)(block: TelegramClient => T): T = {
    Server.withRouterFromComponents() { components =>
      import components.{ defaultActionBuilder => Action }
      {
        case GET(p"/telegram/message" ? q"chat_id=$chatId" & q"text=$text") =>
          chatId must be (channel)
          text must be ("lorem ipsum dolor sit amet")
          Action {
            Results.Status(status)
          }
      }
    } { implicit port =>
      WsTestClient.withClient { client =>
        block(new TelegramClient(config, client))
      }
    }
  }
}
