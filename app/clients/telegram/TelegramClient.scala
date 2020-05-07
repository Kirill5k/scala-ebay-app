package clients.telegram

import cats.effect.IO
import cats.implicits._
import common.config.AppConfig
import common.resources.SttpBackendResource
import domain.{ApiClientError, ResellableItem}
import javax.inject.Inject
import play.api.Logger
import sttp.client._

class TelegramClient @Inject()(catsSttpBackendResource: SttpBackendResource[IO]) {
  import domain.ResellableItemOps._
  private val log: Logger = Logger(getClass)

  private val telegramConfig = AppConfig.load().telegram

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
    catsSttpBackendResource.get.use { implicit b =>
      basicRequest
        .get(uri"${telegramConfig.baseUri}/bot${telegramConfig.botKey}/sendMessage?chat_id=$channelId&text=$message")
        .send()
        .flatMap { r =>
          r.body match {
            case Right(_) => IO.pure(())
            case Left(error) =>
              IO(log.error(s"error sending message to telegram: ${r.code}\n$error")) *>
                IO.raiseError(ApiClientError.HttpError(r.code.code, s"error sending message to telegram channel $channelId: ${r.code}"))
          }
        }
    }
}
