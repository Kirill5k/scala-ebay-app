package services

import java.time.Instant

import cats.effect.{IO, Sync}
import cats.implicits._
import clients.telegram.TelegramClient
import common.Logging
import domain.{PurchasableItem, ResellableItem, StockUpdate}
import javax.inject.Inject

trait NotificationService[F[_]] extends Logging {
  def cheapItem(item: ResellableItem): F[Unit]
  def stockUpdate[I <: PurchasableItem](update: StockUpdate[I]): F[Unit]
}

final class TelegramNotificationService @Inject()(
    telegramClient: TelegramClient
) extends NotificationService[IO] {
  import NotificationService._

  override def cheapItem(item: ResellableItem): IO[Unit] =
    IO(item.cheapItemNotification).flatMap {
      case Some(message) =>
        IO(logger.info(s"""sending "$message"""")) *>
          telegramClient.sendMessageToMainChannel(message)
      case None =>
        IO(logger.warn(s"not enough details for sending cheap item notification $item"))
    }

  override def stockUpdate[I <: PurchasableItem](update: StockUpdate[I]): IO[Unit] =
    update.purchasableItem.itemDetails.fullName match {
      case Some(name) =>
        val message = s"STOCK UPDATE for $name: ${update.updateType}"
        IO(logger.info(s"""sending "$message"""")) *>
          telegramClient.sendMessageToSecondaryChannel(message)
      case None =>
        IO(logger.warn(s"not enough details for stock update notification $update"))
    }
}

object NotificationService {
  implicit class ResellableItemOps(private val item: ResellableItem) extends AnyVal {
    def cheapItemNotification: Option[String] =
      for {
        itemSummary <- item.itemDetails.fullName
        rp          <- item.resellPrice
        price            = item.listingDetails.price
        profitPercentage = rp.exchange * 100 / price - 100
        isEnding         = item.listingDetails.dateEnded.exists(_.minusSeconds(600).isBefore(Instant.now))
        url              = item.listingDetails.url
        header           = if (isEnding) "ENDING" else "NEW"
      } yield s"""$header "$itemSummary" - ebay: £$price, cex: £${rp.exchange}(${profitPercentage.intValue}%)/£${rp.cash} $url"""
  }
}
