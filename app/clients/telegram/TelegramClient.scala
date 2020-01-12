package clients.telegram

import cats.data.EitherT
import cats.implicits._
import domain.ApiClientError._
import domain.{ApiClientError, ResellPrice}
import javax.inject.Inject
import play.api.http.{HeaderNames, Status}
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext

class TelegramClient @Inject()(config: Configuration, client: WSClient)(implicit ex: ExecutionContext) {
  private val telegramConfig = config.get[TelegramConfig]("telegram")

  def sendMessage(channelId: String, message: String): FutureErrorOr[Unit] = {
    val response = client
      .url(s"${telegramConfig.baseUri}${telegramConfig.messagePath}")
      .withQueryStringParameters("chat_id" -> channelId, "text" -> message)
      .get()
      .map { res =>
        if (Status.isSuccessful(res.status)) Right(())
        else HttpError(res.status, s"error sending message to telegram channel $channelId: ${res.status}").asLeft
      }
      .recover(ApiClientError.recoverFromHttpCallFailure.andThen(_.asLeft))

    EitherT(response)
  }
}
