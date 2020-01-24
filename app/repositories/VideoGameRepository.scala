package repositories

import cats.data.EitherT
import cats.implicits._
import domain.ApiClientError.FutureErrorOr
import domain.ItemDetails.GameDetails
import domain.{ApiClientError, ListingDetails, ResellPrice}
import domain.ResellableItem.VideoGame
import javax.inject.Inject
import play.api.libs.json.Format
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.bson.BSONObjectID
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json._


import scala.concurrent.{ExecutionContext, Future}

private case class VideoGameEntity(_id: Option[BSONObjectID], itemDetails: GameDetails, listingDetails: ListingDetails, resellPrice: Option[ResellPrice])

private object VideoGameEntity {
  def from(videoGame: VideoGame): VideoGameEntity =
    VideoGameEntity(None, videoGame.itemDetails, videoGame.listingDetails, videoGame.resellPrice)
}

private object JsonFormats {
  import play.api.libs.json._

  implicit val resellPriceFormat: OFormat[ResellPrice] = Json.format[ResellPrice]
  implicit val listingDetailsFormat: OFormat[ListingDetails] = Json.format[ListingDetails]
  implicit val videoGameDetailsFormat: OFormat[GameDetails] = Json.format[GameDetails]
  implicit val videoGameFormat: OFormat[VideoGameEntity] = Json.format[VideoGameEntity]
}

class VideoGameRepository @Inject() (implicit ex: ExecutionContext, mongo: ReactiveMongoApi) {
  import JsonFormats._

  def videoGamesCollection: Future[JSONCollection] = mongo.database.map(_.collection("videoGames"))

  def save(videoGame: VideoGame): FutureErrorOr[Unit] = {
    val result = videoGamesCollection
      .flatMap(_.insert(ordered = false).one(VideoGameEntity.from(videoGame)))
      .map(_ => ().asRight[ApiClientError])
    EitherT(result)
  }
}
