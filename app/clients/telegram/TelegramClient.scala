package clients.telegram

import cats.effect.{ContextShift, IO}
import cats.implicits._
import domain.ApiClientError._
import domain.{ApiClientError, ResellableItem}
import javax.inject.Inject
import play.api.http.Status
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext

class TelegramClient @Inject()(config: Configuration, client: WSClient)(implicit ex: ExecutionContext) {
  private implicit val cs: ContextShift[IO] = IO.contextShift(ex)

  import domain.ResellableItemOps._
  private val log: Logger = Logger(getClass)

  private val telegramConfig = config.get[TelegramConfig]("telegram")

  def sendMessageToMainChannel(item: ResellableItem): IOErrorOr[Unit] =
    IO(item.notificationMessage).flatMap {
      case Some(message) => sendMessageToMainChannel(message)
      case None =>
        log.warn(s"not enough details for sending notification $item")
        IO(Right(none[Unit]))
    }

  def sendMessageToMainChannel(message: String): IOErrorOr[Unit] =
    sendMessage(telegramConfig.mainChannelId, message)

  def sendMessage(channelId: String, message: String): IOErrorOr[Unit] = {
    val response = client
      .url(s"${telegramConfig.baseUri}${telegramConfig.messagePath}")
      .withQueryStringParameters("chat_id" -> channelId, "text" -> message)
      .get()
      .map { res =>
        if (Status.isSuccessful(res.status)) Right(())
        else HttpError(res.status, s"error sending message to telegram channel $channelId: ${res.status}").asLeft
      }
      .recover(ApiClientError.recoverFromHttpCallFailure.andThen(_.asLeft))

    IO.fromFuture(IO(response))
  }
}
