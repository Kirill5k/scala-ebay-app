package domain

final case class ResellableItem[D <: ItemDetails](
  itemDetails: D,
  listingDetails: ListingDetails,
  resellPrice: Option[ResellPrice]
)

object ResellableItem {
  import ItemDetails._

  type VideoGame = ResellableItem[GameDetails]
  type MobilePhone = ResellableItem[PhoneDetails]

  def generic(id: GenericItemDetails, ld: ListingDetails, rp: Option[ResellPrice]): ResellableItem[GenericItemDetails] =
    ResellableItem(id, ld, rp)
}
