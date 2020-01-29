package repositories

import java.time.Instant

import cats.data.EitherT
import cats.implicits._
import domain.ApiClientError.FutureErrorOr
import domain.ItemDetails.GameDetails
import domain.{ListingDetails, ResellPrice}
import domain.ResellableItem.VideoGame
import javax.inject.Inject
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json._

import scala.concurrent.ExecutionContext

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

class VideoGameRepository @Inject()(implicit override val ex: ExecutionContext, override val mongo: ReactiveMongoApi)
  extends ResellableItemRepository[VideoGameEntity] {
  import JsonFormats._

  override protected val collectionName: String = "videoGames"

  def save(videoGame: VideoGame): FutureErrorOr[Unit] =
    (VideoGameEntity.from _ andThen saveEntity)(videoGame)

  def findAll(limit: Int = 100): FutureErrorOr[Seq[VideoGame]] =
    findAllEntities(limit).map(_.map(_.toDomain))

  def findAllPostedAfter(date: Instant, limit: Int = 1000): FutureErrorOr[Seq[VideoGame]] =
    findAllEntitiesPostedAfter(date, limit).map(_.map(_.toDomain))
}
