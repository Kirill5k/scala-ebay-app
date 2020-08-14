package services

import java.time.Instant

import cats.effect.{IO, Sync}
import cats.implicits._
import clients.telegram.TelegramClient
import common.Logging
import domain.ResellableItem
import javax.inject.Inject

trait NotificationService[F[_]] extends Logging {
  def cheapItem(item: ResellableItem): F[Unit]
}

final class TelegramNotificationService @Inject()(
    telegramClient: TelegramClient
) extends NotificationService[IO] {
  import NotificationService._

  override def cheapItem(item: ResellableItem): IO[Unit] = {
    IO(item.notificationMessage).flatMap {
      case Some(message) =>
        IO(logger.info(s"""sending "$message"""")) *>
          telegramClient.sendMessageToMainChannel(message)
      case None =>
        IO(logger.warn(s"not enough details for sending notification $item")) *>
          IO.pure(None)
    }
  }
}

object NotificationService {
  implicit class ResellableItemOps(private val item: ResellableItem) extends AnyVal {
    def notificationMessage: Option[String] =
      for {
        itemSummary <- item.itemDetails.summary
        rp          <- item.resellPrice
        price            = item.listingDetails.price
        profitPercentage = rp.exchange * 100 / price - 100
        isEnding         = item.listingDetails.dateEnded.exists(_.minusSeconds(600).isBefore(Instant.now))
        url              = item.listingDetails.url
      } yield s"""${if (isEnding) "ENDING" else "NEW"} "$itemSummary" - ebay: £$price, cex: £${rp.exchange}(${profitPercentage.intValue}%)/£${rp.cash} $url"""
  }
}
