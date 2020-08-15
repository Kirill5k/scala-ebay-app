package domain

final case class ResellableItem[D <: ItemDetails](
  itemDetails: D,
  listingDetails: ListingDetails,
  resellPrice: Option[ResellPrice]
)

object ResellableItem {

  type VideoGame = ResellableItem[ItemDetails.Game]
  type MobilePhone = ResellableItem[ItemDetails.Phone]

  def generic(id: ItemDetails.Generic, ld: ListingDetails, rp: Option[ResellPrice]): ResellableItem[ItemDetails.Generic] =
    ResellableItem(id, ld, rp)
}
