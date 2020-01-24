package repositories

import domain.ItemDetails.GameDetails
import domain.{ListingDetails, ResellPrice, VideoGameBuilder}
import org.scalatest.{BeforeAndAfter, FlatSpec}
import org.scalatestplus.play.PlaySpec
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection
import play.api.test.Helpers._

import scala.concurrent.Future


class VideoGameRepositorySpec extends PlayWithMongoSpec with BeforeAndAfter {
  import scala.concurrent.ExecutionContext.Implicits.global
  import JsonFormats._

  var videoGames: Future[JSONCollection] = _

  before {
    await {
      videoGames = reactiveMongoApi.database.map(_.collection("videoGames"))

      videoGames.flatMap(_.insert(ordered = false).many(List(
        VideoGameEntity.from(VideoGameBuilder.get("GTA 5")),
        VideoGameEntity.from(VideoGameBuilder.get("Call of Duty WW2")),
        VideoGameEntity.from(VideoGameBuilder.get("Super Mario 3"))
      )))
    }
  }

  after {
    videoGames.flatMap(_.drop(failIfNotFound = false))
  }

}
