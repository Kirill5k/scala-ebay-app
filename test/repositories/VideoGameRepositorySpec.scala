package repositories

import domain.ItemDetails.GameDetails
import domain.{ListingDetails, ResellPrice, VideoGameBuilder}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, FlatSpec}
import org.scalatestplus.play.PlaySpec
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection
import play.api.test.Helpers._
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps



class VideoGameRepositorySpec extends PlayWithMongoSpec with BeforeAndAfter with ScalaFutures {
  import scala.concurrent.ExecutionContext.Implicits.global
  import JsonFormats._

  var videoGames: Future[JSONCollection] = _

  before {
    await {
      videoGames = reactiveMongoApi.database.map(_.collection("videoGames"))

      videoGames.flatMap(_.insert(ordered = false).many(List(
        VideoGameEntity.from(VideoGameBuilder.build("GTA 5")),
        VideoGameEntity.from(VideoGameBuilder.build("Call of Duty WW2")),
        VideoGameEntity.from(VideoGameBuilder.build("Super Mario 3"))
      )))
    }
  }

  after {
    videoGames.flatMap(_.drop(failIfNotFound = false))
  }

  "VideoGameRepository" should {
    val videoGameRepository = new VideoGameRepository()

    "save video game in db" in {
      val futureResult = videoGameRepository.save(VideoGameBuilder.build("Witcher 3"))

      whenReady(futureResult.value, timeout(10 seconds), interval(500 millis)) { result =>
        result must be (Right(()))
      }
    }
  }
}
