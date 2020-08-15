package repositories

import java.time.Instant

import cats.effect.{ContextShift, IO}
import cats.implicits._
import common.Logging
import common.errors.ApiClientError.DbError
import domain.ItemDetails.Game
import domain.ResellableItem.VideoGame
import domain.{ItemDetails, ResellableItem}
import javax.inject.Inject
import play.api.libs.json.{JsObject, Json, OFormat}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{Cursor, ReadConcern, ReadPreference}
import reactivemongo.bson.{BSONDocument, BSONString}
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json._
import repositories.ResellableItemEntity.VideoGameEntity

import scala.concurrent.{ExecutionContext, Future}

trait ResellableItemRepository[D <: ItemDetails] extends Logging {
  implicit protected def ex: ExecutionContext
  implicit protected def mongo: ReactiveMongoApi
  implicit protected def entityMapper: ResellableItemEntityMapper[D]
  implicit protected def entityFormat: OFormat[ResellableItemEntity[D]]

  implicit private val cs: ContextShift[IO] = IO.contextShift(ex)

  protected def collectionName: String
  protected val itemCollection: Future[JSONCollection] = mongo.database.map(_.collection(collectionName))

  def existsByUrl(listingUrl: String): IO[Boolean] =
    toIO(itemCollection.flatMap { collection =>
      collection
        .withReadPreference(ReadPreference.primary)
        .count(Some(Json.obj("listingDetails.url" -> listingUrl)), None, 0, None, ReadConcern.Available)
        .map(_ > 0)
    })

  def save(item: ResellableItem[D]): IO[Unit] =
    (entityMapper.toEntity _ andThen saveEntity)(item)

  private def saveEntity(entity: ResellableItemEntity[D]): IO[Unit] =
    toIO(itemCollection.flatMap(_.insert(ordered = false).one(entity))) *> IO.unit

  def findAll(limit: Option[Int] = None, from: Option[Instant] = None, to: Option[Instant] = None): IO[List[ResellableItem[D]]] =
    findAllEntities(limit, from, to).map { entities =>
      entities.map(entityMapper.toDomain)
    }

  private def findAllEntities(limit: Option[Int], from: Option[Instant], to: Option[Instant]): IO[List[ResellableItemEntity[D]]] = {
    val selectors = postedDateRangeSelector(from, to).fold(BSONDocument())(BSONDocument(_))
    toIO(itemCollection.flatMap { collection =>
      collection
        .find(selector = selectors, projection = Option.empty[JsObject])
        .sort(Json.obj("listingDetails.datePosted" -> -1))
        .cursor[ResellableItemEntity[D]](ReadPreference.primary)
        .collect[List](limit.getOrElse(-1), Cursor.FailOnError[List[ResellableItemEntity[D]]]())
    })
  }

  private def toIO[A](result: Future[A]): IO[A] =
    IO.fromFuture(IO(result)).handleErrorWith { e =>
      IO(logger.error("error during db operation", e)) *>
        IO.raiseError(DbError(s"error during db operation: ${e.getMessage}"))
    }

  private def postedDateRangeSelector(from: Option[Instant], to: Option[Instant]): Option[(String, BSONDocument)] = {
    val dateSelector = List(from.map(f => "$gte" -> BSONString(f.toString)), to.map(t => "$lt" -> BSONString(t.toString))).flatten
    if (dateSelector.nonEmpty) Some("listingDetails.datePosted" -> BSONDocument(dateSelector))
    else None
  }
}

class VideoGameRepository @Inject()(
    implicit override val ex: ExecutionContext,
    override val mongo: ReactiveMongoApi
) extends ResellableItemRepository[Game] {
  import ResellableItemEntity._

  implicit override protected def entityMapper: ResellableItemEntityMapper[Game] =
    ResellableItemEntityMapper.videoGameEntityMapper

  implicit override protected def entityFormat: OFormat[VideoGameEntity] =
    ResellableItemEntity.videoGameFormat

  override protected val collectionName: String = "videoGames"
}
