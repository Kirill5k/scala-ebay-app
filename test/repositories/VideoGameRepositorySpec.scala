package repositories

import java.time.Instant

import domain.ItemDetails.GameDetails
import domain.ResellableItem.VideoGame
import domain.{ListingDetails, ResellPrice, VideoGameBuilder}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.BeforeAndAfter
import reactivemongo.play.json.collection.JSONCollection
import play.api.test.Helpers._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps



class VideoGameRepositorySpec extends PlayWithMongoSpec with BeforeAndAfter with ScalaFutures {
  import scala.concurrent.ExecutionContext.Implicits.global
  import JsonFormats._

  var videoGamesDb: Future[JSONCollection] = _
  val videoGames: Seq[VideoGame] = List(
      VideoGameBuilder.build("GTA 5"),
      VideoGameBuilder.build("Call of Duty WW2"),
      VideoGameBuilder.build("Super Mario 3")
  )

  before {
    await {
      videoGamesDb = reactiveMongoApi.database.map(_.collection("videoGames"))

      videoGamesDb.flatMap(_.insert(ordered = false).many(videoGames.map(VideoGameEntity.from)))
    }
  }

  after {
    videoGamesDb.flatMap(_.drop(failIfNotFound = false))
  }

  "VideoGameRepository" should {

    "find all video games posted after provided date" in {
      val videoGameRepository = inject[VideoGameRepository]
      val futureResult = videoGameRepository.findAllPostedAfter(Instant.now.minusSeconds(10000))

      whenReady(futureResult.value, timeout(10 seconds), interval(500 millis)) { result =>
        result must be (Right(videoGames))
      }
    }

    "save video game in db" in {
      val videoGameRepository = inject[VideoGameRepository]
      val futureResult = videoGameRepository.save(VideoGameBuilder.build("Witcher 3"))

      whenReady(futureResult.value, timeout(10 seconds), interval(500 millis)) { result =>
        result must be (Right(()))
      }
    }
  }
}
