package repositories

import java.time.Instant

import cats.effect.{ContextShift, IO}
import cats.implicits._
import common.Logging
import domain.ApiClientError.DbError
import domain.ResellableItem
import play.api.Logger
import play.api.libs.json.{JsObject, Json, OFormat}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{Cursor, ReadConcern, ReadPreference}
import reactivemongo.bson.{BSONDocument, BSONString}
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json._

import scala.concurrent.{ExecutionContext, Future}

trait ResellableItemRepository[D <: ResellableItem, E <: ResellableItemEntity] extends Logging {
  implicit protected def ex: ExecutionContext
  implicit protected def mongo: ReactiveMongoApi
  implicit protected def entityMapper: ResellableItemEntityMapper[D, E]
  implicit protected def entityFormat: OFormat[E]

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

  def save(item: D): IO[Unit] =
    (entityMapper.toEntity _ andThen saveEntity)(item)

  private def saveEntity(entity: E): IO[Unit] =
    toIO(itemCollection.flatMap{ collection =>
      collection
        .insert(ordered = false)
        .one(entity)
    }) *> IO.pure(())

  def findAll(limit: Option[Int] = None, from: Option[Instant] = None, to: Option[Instant] = None): IO[Seq[D]] =
    findAllEntities(limit, from, to).map(_.map(entityMapper.toDomain))

  private def findAllEntities(limit: Option[Int], from: Option[Instant], to: Option[Instant]): IO[Seq[E]] = {
    val selectors = postedDateRangeSelector(from, to).fold(BSONDocument())(BSONDocument(_))
    toIO(itemCollection.flatMap { collection =>
      collection
        .find(selector = selectors, projection = Option.empty[JsObject])
        .sort(Json.obj("listingDetails.datePosted" -> -1))
        .cursor[E](ReadPreference.primary)
        .collect[List](limit.getOrElse(-1), Cursor.FailOnError[List[E]]())
    })
  }

  private def toIO[A](result: Future[A]): IO[A] =
    IO.fromFuture(IO(result)).handleErrorWith { e =>
      logger.error("error during db operation", e)
      IO.raiseError(DbError(s"error during db operation: ${e.getMessage}"))
    }

  private def postedDateRangeSelector(from: Option[Instant], to: Option[Instant]): Option[(String, BSONDocument)] = {
    val dateSelector = List(from.map(f => "$gte" -> BSONString(f.toString)), to.map(t => "$lt" -> BSONString(t.toString))).flatten
    if (dateSelector.nonEmpty) Some("listingDetails.datePosted" -> BSONDocument(dateSelector)) else None
  }
}
