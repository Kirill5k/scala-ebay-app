package repositories

import domain.ItemDetails.GameDetails
import domain.{ItemDetails, ListingDetails, ResellPrice}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json._

private[repositories] trait ResellableItemEntity {
  def _id: Option[BSONObjectID]
  def itemDetails: ItemDetails
  def listingDetails: ListingDetails
  def resellPrice: Option[ResellPrice]
}

private[repositories] object ResellableItemEntity {
  case class VideoGameEntity(
                              _id: Option[BSONObjectID],
                              itemDetails: GameDetails,
                              listingDetails: ListingDetails,
                              resellPrice: Option[ResellPrice]
                            ) extends ResellableItemEntity


  import play.api.libs.json._

  implicit val resellPriceFormat: OFormat[ResellPrice] = Json.format[ResellPrice]
  implicit val listingDetailsFormat: OFormat[ListingDetails] = Json.format[ListingDetails]
  implicit val videoGameDetailsFormat: OFormat[GameDetails] = Json.format[GameDetails]
  implicit val videoGameFormat: OFormat[VideoGameEntity] = Json.format[VideoGameEntity]
}
