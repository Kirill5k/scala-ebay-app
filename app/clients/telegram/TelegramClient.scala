package clients.telegram

import cats.data.EitherT
import cats.implicits._
import domain.ApiClientError._
import domain.{ApiClientError, ResellableItem}
import javax.inject.Inject
import play.api.http.Status
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}

import scala.concurrent.{ExecutionContext, Future}

class TelegramClient @Inject()(config: Configuration, client: WSClient)(implicit ex: ExecutionContext) {
  import domain.ResellableItemOps._
  private val logger: Logger = Logger(getClass)

  private val telegramConfig = config.get[TelegramConfig]("telegram")

  def sendMessageToMainChannel(item: ResellableItem): FutureErrorOr[Unit] =
    EitherT.rightT[Future, ApiClientError](item.notificationMessage).flatMap {
      case Some(message) => sendMessageToMainChannel(message)
      case None =>
        logger.warn(s"not enough details for sending notification $item")
        EitherT.rightT[Future, ApiClientError](none[Unit])
    }

  def sendMessageToMainChannel(message: String): FutureErrorOr[Unit] =
    sendMessage(telegramConfig.mainChannelId, message)

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
