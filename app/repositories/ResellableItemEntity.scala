package repositories

import domain.ItemDetails.GameDetails
import domain.{ItemDetails, Packaging, ListingDetails, ResellPrice}
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

  implicit val itemDetailsFormat: Format[Packaging] = new Format[Packaging] {
    override def reads(json: JsValue): JsResult[Packaging] = json.toString() match {
      case "bundle" => JsSuccess(Packaging.Bundle)
      case _ => JsSuccess(Packaging.Single)
    }
    override def writes(packaging: Packaging): JsString = packaging match {
      case Packaging.Bundle => JsString("bundle")
      case _ => JsString("single")
    }
  }
  implicit val resellPriceFormat: OFormat[ResellPrice] = Json.format[ResellPrice]
  implicit val listingDetailsFormat: OFormat[ListingDetails] = Json.format[ListingDetails]
  implicit val videoGameDetailsFormat: OFormat[GameDetails] = Json.format[GameDetails]
  implicit val videoGameFormat: OFormat[VideoGameEntity] = Json.format[VideoGameEntity]
}
