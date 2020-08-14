package domain

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
