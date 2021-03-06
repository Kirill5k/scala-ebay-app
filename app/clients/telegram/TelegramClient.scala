package clients.telegram

import cats.effect.IO
import cats.implicits._
import common.Logging
import common.config.AppConfig
import common.errors.ApiClientError
import common.resources.SttpBackendResource
import javax.inject.Inject
import sttp.client._

class TelegramClient @Inject()(catsSttpBackendResource: SttpBackendResource[IO]) extends Logging {
  private val telegramConfig = AppConfig.load().telegram

  def sendMessageToMainChannel(message: String): IO[Unit] =
    sendMessage(telegramConfig.mainChannelId, message)

  def sendMessageToSecondaryChannel(message: String): IO[Unit] =
    sendMessage(telegramConfig.secondaryChannelId, message)

  def sendMessage(channelId: String, message: String): IO[Unit] =
    catsSttpBackendResource.get.use { implicit b =>
      basicRequest
        .get(uri"${telegramConfig.baseUri}/bot${telegramConfig.botKey}/sendMessage?chat_id=$channelId&text=$message")
        .send()
        .flatMap { r =>
          r.body match {
            case Right(_) => IO.pure(())
            case Left(error) =>
              IO(logger.error(s"error sending message to telegram: ${r.code}\n$error")) *>
                IO.raiseError(ApiClientError.HttpError(r.code.code, s"error sending message to telegram channel $channelId: ${r.code}"))
          }
        }
    }
}
