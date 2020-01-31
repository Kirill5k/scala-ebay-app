package repositories

import java.time.Instant

import cats.data.EitherT
import cats.implicits._
import domain.ApiClientError.FutureErrorOr
import domain.ItemDetails.GameDetails
import domain.{ListingDetails, ResellPrice}
import domain.ResellableItem.VideoGame
import javax.inject.Inject
import play.api.libs.json.OFormat
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json._
import repositories.ResellableItemEntity.VideoGameEntity

import scala.concurrent.ExecutionContext

class VideoGameRepository @Inject()(implicit override val ex: ExecutionContext, override val mongo: ReactiveMongoApi)
  extends ResellableItemRepository[VideoGame, VideoGameEntity] {
  import ResellableItemEntity._

  override implicit protected def entityMapper: ResellableItemEntityMapper[VideoGame, VideoGameEntity] =
    ResellableItemEntityMapper.videoGameEntityMapper

  override implicit protected def entityFormat: OFormat[VideoGameEntity] =
    ResellableItemEntity.videoGameFormat

  override protected val collectionName: String = "videoGames"
}
