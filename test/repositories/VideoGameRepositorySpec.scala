package repositories

import java.net.URI
import java.time.Instant

import domain.ResellableItem.VideoGame
import domain.VideoGameBuilder
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.BeforeAndAfter
import reactivemongo.play.json.collection.JSONCollection
import play.api.test.Helpers._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps


class VideoGameRepositorySpec extends PlayWithMongoSpec with BeforeAndAfter with ScalaFutures {
  import scala.concurrent.ExecutionContext.Implicits.global
  import ResellableItemEntity._
  import ResellableItemEntityMapper._

  var videoGamesDb: Future[JSONCollection] = _
  val videoGames: Seq[VideoGame] = List(
      VideoGameBuilder.build("GTA 5", Instant.now().minusSeconds(1000)),
      VideoGameBuilder.build("Call of Duty WW2", Instant.now()),
      VideoGameBuilder.build("Super Mario 3", Instant.now().plusSeconds(1000))
  )

  before {
    await {
      videoGamesDb = reactiveMongoApi.database.map(_.collection("videoGames"))

      videoGamesDb.flatMap(_.insert(ordered = false).many(videoGames.map(videoGameEntityMapper.toEntity)))
    }
  }

  after {
    videoGamesDb.flatMap(_.drop(failIfNotFound = false))
  }

  "VideoGameRepository" should {

    "check if video game already exists by url" in {
      val videoGameRepository = inject[VideoGameRepository]
      val futureResult = videoGameRepository.existsByUrl("https://www.ebay.co.uk/itm/super-mario-3")

      whenReady(futureResult.value, timeout(10 seconds), interval(500 millis)) { result =>
        result must be (Right(true))
      }
    }

    "check if video game doesnt exist by url" in {
      val videoGameRepository = inject[VideoGameRepository]
      val futureResult = videoGameRepository.existsByUrl("https://www.ebay.co.uk/itm/super-mario-4")

      whenReady(futureResult.value, timeout(10 seconds), interval(500 millis)) { result =>
        result must be (Right(false))
      }
    }

    "find all video games" in {
      val videoGameRepository = inject[VideoGameRepository]
      val futureResult = videoGameRepository.findAll()

      whenReady(futureResult.value, timeout(10 seconds), interval(500 millis)) { result =>
        result must be (Right(videoGames.reverse))
      }
    }

    "find all video games posted after provided date" in {
      val videoGameRepository = inject[VideoGameRepository]
      val futureResult = videoGameRepository.findAllPostedAfter(Instant.now)

      whenReady(futureResult.value, timeout(10 seconds), interval(500 millis)) { result =>
        result must be (Right(List(videoGames(2))))
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
