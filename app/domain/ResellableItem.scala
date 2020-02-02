package domain

import java.time.Instant

sealed trait ResellableItem {
  def itemDetails: ItemDetails
  def listingDetails: ListingDetails
  def resellPrice: Option[ResellPrice]
}

object ResellableItem {
  import ItemDetails._
  final case class VideoGame(itemDetails: GameDetails, listingDetails: ListingDetails, resellPrice: Option[ResellPrice]) extends ResellableItem
  final case class MobilePhone(itemDetails: PhoneDetails, listingDetails: ListingDetails, resellPrice: Option[ResellPrice]) extends ResellableItem
}

private[domain] trait NotificationMessageGenerator[A <: ResellableItem] {
  def generate(item: A): Option[String]
}

object ResellableItemOps {
  import domain.ItemDetailsOps._
  val messageGenerator = new NotificationMessageGenerator[ResellableItem] {
    override def generate(item: ResellableItem): Option[String] =
      for {
        itemSummary <- item.itemDetails.summary
        rp <- item.resellPrice
        price = item.listingDetails.price
        profitPercentage = rp.exchange * 100 / price - 100
        isEnding = item.listingDetails.dateEnded.exists(_.minusSeconds(600).isBefore(Instant.now))
      } yield s"""${if (isEnding) "ENDING" else "NEW"} "$itemSummary" - ebay: £$price, cex: £${rp.exchange}(${profitPercentage.intValue}%)/£${rp.cash}"""
  }

  implicit class ResellableItemSyntax(val item: ResellableItem) extends AnyVal {
    def notificationMessage: Option[String] = messageGenerator.generate(item)
  }
}


