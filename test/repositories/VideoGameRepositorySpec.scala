package repositories

import java.time.Instant

import cats.effect.testing.scalatest.AsyncIOSpec
import domain.ResellableItem.VideoGame
import domain.VideoGameBuilder
import org.scalatest.BeforeAndAfter
import reactivemongo.play.json.collection.JSONCollection
import play.api.test.Helpers._

import scala.concurrent.Future


class VideoGameRepositorySpec extends PlayWithMongoSpec with BeforeAndAfter with AsyncIOSpec {
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

      existsResult.asserting(_ must be (true))
    }

    "check if video game doesnt exist by url" in {
      val videoGameRepository = inject[VideoGameRepository]
      val existsResult = videoGameRepository.existsByUrl("https://www.ebay.co.uk/itm/super-mario-4")

      existsResult.asserting(_ must be (false))
    }

    "find all video games" in {
      val videoGameRepository = inject[VideoGameRepository]
      val findAllResult = videoGameRepository.findAll()

      findAllResult.asserting(_ must be (videoGames.reverse))
    }

    "find all video games posted after provided date" in {
      val videoGameRepository = inject[VideoGameRepository]
      val findAllResult = videoGameRepository.findAllPostedAfter(Instant.now)

      findAllResult.asserting(_ must be (List(videoGames(2))))
    }

    "save video game in db" in {
      val videoGameRepository = inject[VideoGameRepository]
      val saveResult = videoGameRepository.save(VideoGameBuilder.build("Witcher 3"))

      saveResult.asserting(_ must be (()))
    }
  }
}
