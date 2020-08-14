package domain

import java.time.Instant

final case class ListingDetails(
    url: String,
    title: String,
    shortDescription: Option[String],
    description: Option[String],
    image: Option[String],
    buyingOptions: Seq[String],
    sellerName: Option[String],
    price: BigDecimal,
    condition: String,
    datePosted: Instant,
    dateEnded: Option[Instant],
    properties: Map[String, String]
)

final case class ResellPrice(
    cash: BigDecimal,
    exchange: BigDecimal
)

sealed trait ResellableItem {
  def itemDetails: ItemDetails
  def listingDetails: ListingDetails
  def resellPrice: Option[ResellPrice]
}

object ResellableItem {
  import ItemDetails._

  final case class VideoGame(
      itemDetails: GameDetails,
      listingDetails: ListingDetails,
      resellPrice: Option[ResellPrice]
  ) extends ResellableItem

  final case class MobilePhone(
      itemDetails: PhoneDetails,
      listingDetails: ListingDetails,
      resellPrice: Option[ResellPrice]
  ) extends ResellableItem
}
