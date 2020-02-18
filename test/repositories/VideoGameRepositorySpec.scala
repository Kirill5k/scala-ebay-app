package repositories

import java.time.Instant

import domain.ResellableItem.VideoGame
import domain.VideoGameBuilder
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
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
      val existsResult = videoGameRepository.existsByUrl("https://www.ebay.co.uk/itm/super-mario-3")

      whenReady(existsResult.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { exists =>
        exists must be (true)
      }
    }

    "check if video game doesnt exist by url" in {
      val videoGameRepository = inject[VideoGameRepository]
      val existsResult = videoGameRepository.existsByUrl("https://www.ebay.co.uk/itm/super-mario-4")

      whenReady(existsResult.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { exists =>
        exists must be (false)
      }
    }

    "find all video games" in {
      val videoGameRepository = inject[VideoGameRepository]
      val findAllResult = videoGameRepository.findAll()


      whenReady(findAllResult.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { items =>
        items must be (videoGames.reverse)
      }
    }

    "find all video games posted after provided date" in {
      val videoGameRepository = inject[VideoGameRepository]
      val findAllResult = videoGameRepository.findAll(from = Some(Instant.now))


      whenReady(findAllResult.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { items =>
        items must be (List(videoGames(2)))
      }
    }

    "save video game in db" in {
      val videoGameRepository = inject[VideoGameRepository]
      val saveResult = videoGameRepository.save(VideoGameBuilder.build("Witcher 3"))

      whenReady(saveResult.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { saved =>
        saved must be (())
      }
    }
  }
}
