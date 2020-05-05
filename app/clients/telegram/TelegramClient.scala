package clients.telegram

import cats.effect.IO
import cats.implicits._
import domain.ApiClientError._
import domain.ResellableItem
import javax.inject.Inject
import play.api.http.Status
import play.api.{Configuration, Logger}
import resources.CatsSttpBackend
import sttp.client._

class TelegramClient @Inject()(config: Configuration, catsSttpBackend: CatsSttpBackend) {
  import domain.ResellableItemOps._
  private val log: Logger = Logger(getClass)

  private val telegramConfig = config.get[TelegramConfig]("telegram")

  def sendMessageToMainChannel(item: ResellableItem): IO[Unit] =
    IO.pure(item.notificationMessage).flatMap {
      case Some(message) =>
        IO(log.info(s"""sending "$message"""")) *>
          sendMessageToMainChannel(message)
      case None =>
        IO(log.warn(s"not enough details for sending notification $item")) *>
          IO.pure(none[Unit])
    }

  def sendMessageToMainChannel(message: String): IO[Unit] =
    sendMessage(telegramConfig.mainChannelId, message)

  def sendMessage(channelId: String, message: String): IO[Unit] =
    catsSttpBackend.backendResource.use { implicit b =>
      basicRequest
        .get(uri"${telegramConfig.baseUri}/bot${telegramConfig.botKey}/sendMessage?chat_id=$channelId&message=$message")
        .send()
        .flatMap { r =>
          if (Status.isSuccessful(r.code.code)) IO.pure(())
          else IO.raiseError(HttpError(r.code.code, s"error sending message to telegram channel $channelId: ${r.statusText}"))
        }
    }
}
