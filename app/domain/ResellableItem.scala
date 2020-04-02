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

object ResellableItemOps {

  implicit class ResellableItemSyntax(private val item: ResellableItem) extends AnyVal {

    private def bundleMessage(itemSummary: String, price: BigDecimal): String =
      s"""BUNDLE "$itemSummary" £$price"""

    private def singleItemMessage(itemSummary: String, price: BigDecimal, resellPrice: ResellPrice): String = {
      val profitPercentage = resellPrice.exchange * 100 / price - 100
      s""""$itemSummary" - ebay: £$price, cex: £${resellPrice.exchange}(${profitPercentage.intValue}%)/£${resellPrice.cash}"""
    }

    def notificationMessage: Option[String] =
      for {
        itemSummary <- item.itemDetails.summary
        price = item.listingDetails.price
        url = item.listingDetails.url
        messageBody <- if (item.itemDetails.isBundle) Some(bundleMessage(itemSummary, price))
                       else item.resellPrice.map(rp => singleItemMessage(itemSummary, price, rp))
        isEnding = item.listingDetails.dateEnded.exists(_.minusSeconds(600).isBefore(Instant.now))
      } yield s"""${if (isEnding) "ENDING" else "NEW"} $messageBody $url"""
  }
}


