package repositories

import domain.ItemDetails.GameDetails
import domain.{ItemDetails, ItemDetailsType, ListingDetails, ResellPrice}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json._

trait ResellableItemEntity {
  def _id: Option[BSONObjectID]
  def itemDetails: ItemDetails
  def listingDetails: ListingDetails
  def resellPrice: Option[ResellPrice]
}

object ResellableItemEntity {
  final case class VideoGameEntity(
    _id: Option[BSONObjectID],
    itemDetails: GameDetails,
    listingDetails: ListingDetails,
    resellPrice: Option[ResellPrice]) extends ResellableItemEntity


  import play.api.libs.json._

  implicit val itemDetailsFormat: Format[ItemDetailsType] = new Format[ItemDetailsType] {
    override def reads(json: JsValue): JsResult[ItemDetailsType] = json.toString() match {
      case "bundle" => JsSuccess(ItemDetailsType.Bundle)
      case _ => JsSuccess(ItemDetailsType.Single)
    }
    override def writes(detailsType: ItemDetailsType): JsString = detailsType match {
      case ItemDetailsType.Bundle => JsString("bundle")
      case _ => JsString("single")
    }
  }
  implicit val resellPriceFormat: OFormat[ResellPrice] = Json.format[ResellPrice]
  implicit val listingDetailsFormat: OFormat[ListingDetails] = Json.format[ListingDetails]
  implicit val videoGameDetailsFormat: OFormat[GameDetails] = Json.format[GameDetails]
  implicit val videoGameFormat: OFormat[VideoGameEntity] = Json.format[VideoGameEntity]
}
