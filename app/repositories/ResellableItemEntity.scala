package repositories

import domain.ItemDetails.GameDetails
import domain.{ItemDetails, ListingDetails, Packaging, ResellPrice}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json._

final case class ResellableItemEntity[D <: ItemDetails] (
  _id: Option[BSONObjectID],
  itemDetails: D,
  listingDetails: ListingDetails,
  resellPrice: Option[ResellPrice]
)

object ResellableItemEntity {
  type VideoGameEntity = ResellableItemEntity[GameDetails]

  import play.api.libs.json._

  implicit val itemDetailsFormat: Format[Packaging] = new Format[Packaging] {
    override def reads(json: JsValue): JsResult[Packaging] = json.toString() match {
      case "bundle" => JsSuccess(Packaging.Bundle)
      case _        => JsSuccess(Packaging.Single)
    }
    override def writes(packaging: Packaging): JsString = packaging match {
      case Packaging.Bundle => JsString("bundle")
      case _                => JsString("single")
    }
  }
  implicit val resellPriceFormat: OFormat[ResellPrice]       = Json.format[ResellPrice]
  implicit val listingDetailsFormat: OFormat[ListingDetails] = Json.format[ListingDetails]
  implicit val videoGameDetailsFormat: OFormat[GameDetails]  = Json.using[Json.WithDefaultValues].format[GameDetails]
  implicit val videoGameFormat: OFormat[VideoGameEntity]     = Json.format[VideoGameEntity]
}
