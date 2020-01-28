package repositories

import java.net.URI
import java.time.Instant

import cats.data.EitherT
import cats.implicits._
import domain.ApiClientError.FutureErrorOr
import domain.ItemDetails.GameDetails
import domain.{ApiClientError, ListingDetails, ResellPrice}
import domain.ResellableItem.VideoGame
import javax.inject.Inject
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{Cursor, ReadConcern, ReadPreference}
import reactivemongo.bson.{BSONDateTime, BSONDocument, BSONLong, BSONObjectID, BSONReader, BSONString, BSONValue, BSONWriter}
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json._

import scala.concurrent.{ExecutionContext, Future}

private[repositories] case class VideoGameEntity(
                                                  _id: Option[BSONObjectID],
                                                  itemDetails: GameDetails,
                                                  listingDetails: ListingDetails,
                                                  resellPrice: Option[ResellPrice]
                                                )

private[repositories] object VideoGameEntity {
  def from(videoGame: VideoGame): VideoGameEntity =
    VideoGameEntity(None, videoGame.itemDetails, videoGame.listingDetails, videoGame.resellPrice)

  implicit class VideoGameEntitySyntax(val entity: VideoGameEntity) extends AnyVal {
    def toDomain: VideoGame = VideoGame(entity.itemDetails, entity.listingDetails, entity.resellPrice)
  }
}

private object JsonFormats {
  import play.api.libs.json._

  implicit val resellPriceFormat: OFormat[ResellPrice] = Json.format[ResellPrice]
  implicit val listingDetailsFormat: OFormat[ListingDetails] = Json.format[ListingDetails]
  implicit val videoGameDetailsFormat: OFormat[GameDetails] = Json.format[GameDetails]
  implicit val videoGameFormat: OFormat[VideoGameEntity] = Json.format[VideoGameEntity]
}

class VideoGameRepository @Inject()(override implicit val ex: ExecutionContext, override val mongo: ReactiveMongoApi)
  extends ResellableItemRepository[VideoGameEntity] {
  import JsonFormats._

  override protected val collectionName: String = "videoGames"

  def save(videoGame: VideoGame): FutureErrorOr[Unit] =
    EitherT(saveEntity(VideoGameEntity.from(videoGame)).map(_ => ().asRight[ApiClientError]))

  def findAll(limit: Int = 100): FutureErrorOr[Seq[VideoGame]] =
    EitherT(findAllEntities(limit).map(_.map(_.toDomain).asRight[ApiClientError]))

  def findAllPostedAfter(date: Instant, limit: Int = 1000): FutureErrorOr[Seq[VideoGame]] =
    EitherT(findAllEntitiesPostedAfter(date, limit).map(_.map(_.toDomain).asRight[ApiClientError]))
}
