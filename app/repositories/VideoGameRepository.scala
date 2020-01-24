package repositories

import domain.ItemDetails.GameDetails
import domain.{ListingDetails, ResellPrice}
import domain.ResellableItem.VideoGame
import javax.inject.Inject
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.bson.BSONObjectID
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

private case class VideoGameEntity(_id: Option[BSONObjectID], itemDetails: GameDetails, listingDetails: ListingDetails, resellPrice: Option[ResellPrice])

private object VideoGameEntity {
  def from(videoGame: VideoGame): VideoGameEntity =
    VideoGameEntity(None, videoGame.itemDetails, videoGame.listingDetails, videoGame.resellPrice)
}

private object JsonFormats {
  import play.api.libs.json._

  implicit val videoGameFormat: OFormat[VideoGameEntity] = Json.format[VideoGameEntity]
}

class VideoGameRepository @Inject() (implicit ex: ExecutionContext, mongo: ReactiveMongoApi) {
  import JsonFormats._

  def videoGamesCollection: Future[JSONCollection] = mongo.database.map(_.collection("videoGames"))

  def save(videoGame: VideoGame): Future[WriteResult] =
    videoGamesCollection.flatMap(_.insert(ordered = false).one(VideoGameEntity.from(videoGame)))
}
