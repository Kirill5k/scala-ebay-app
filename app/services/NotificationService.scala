package services

import cats.effect.IO
import cats.implicits._
import clients.telegram.TelegramClient
import common.Logging
import domain.{ItemDetails, ResellableItem, StockUpdate}
import javax.inject.Inject

trait NotificationService[F[_]] extends Logging {
  def cheapItem[D <: ItemDetails](item: ResellableItem[D]): F[Unit]
  def stockUpdate[D <: ItemDetails](update: StockUpdate[D]): F[Unit]
}

final class TelegramNotificationService @Inject()(
    telegramClient: TelegramClient
) extends NotificationService[IO] {
  import NotificationService._

  override def cheapItem[D <: ItemDetails](item: ResellableItem[D]): IO[Unit] =
    IO(item.cheapItemNotification).flatMap {
      case Some(message) =>
        IO(logger.info(s"""sending "$message"""")) *>
          telegramClient.sendMessageToMainChannel(message)
      case None =>
        IO(logger.warn(s"not enough details for sending cheap item notification $item"))
    }

  override def stockUpdate[D <: ItemDetails](update: StockUpdate[D]): IO[Unit] =
    update.item.itemDetails.fullName match {
      case Some(name) =>
        val message = s"STOCK UPDATE for $name: ${update.updateType}"
        IO(logger.info(s"""sending "$message"""")) *>
          telegramClient.sendMessageToSecondaryChannel(message)
      case None =>
        IO(logger.warn(s"not enough details for stock update notification $update"))
    }
}

object NotificationService {
  implicit class ResellableItemOps[D <: ItemDetails](private val item: ResellableItem[D]) extends AnyVal {
    def cheapItemNotification: Option[String] =
      for {
        itemSummary <- item.itemDetails.fullName
        rp          <- item.resellPrice
        price            = item.price.value
        profitPercentage = rp.exchange * 100 / price - 100
        url              = item.listingDetails.url
      } yield s"""NEW "$itemSummary" - ebay: £$price, cex: £${rp.exchange}(${profitPercentage.intValue}%)/£${rp.cash} $url"""
  }
}
