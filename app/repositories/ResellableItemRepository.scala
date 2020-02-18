package repositories

import java.time.Instant

import cats.effect.{ContextShift, IO}
import cats.implicits._
import domain.{ApiClientError, ResellableItem}
import play.api.libs.json.{JsObject, Json, OFormat}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{Cursor, ReadConcern, ReadPreference}
import reactivemongo.bson.{BSONDocument, BSONString}
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json._

import scala.concurrent.{ExecutionContext, Future}

trait ResellableItemRepository[D <: ResellableItem, E <: ResellableItemEntity] {
  implicit protected def ex: ExecutionContext
  implicit protected def mongo: ReactiveMongoApi
  implicit protected def entityMapper: ResellableItemEntityMapper[D, E]
  implicit protected def entityFormat: OFormat[E]

  implicit private val cs: ContextShift[IO] = IO.contextShift(ex)

  protected def collectionName: String
  protected val itemCollection: Future[JSONCollection] = mongo.database.map(_.collection(collectionName))

  def existsByUrl(listingUrl: String): IO[Boolean] = {
    val result = itemCollection.flatMap { collection =>
      collection
        .withReadPreference(ReadPreference.primary)
        .count(Some(Json.obj("listingDetails.url" -> listingUrl.toString)), None, 0, None, ReadConcern.Available)
        .map(_ > 0)
        .map(_.asRight[ApiClientError])
    }
      .recover(ApiClientError.recoverFromDbError.andThen(_.asLeft))
    ApiClientError.fromFutureErrorToIO(result)
  }

  def save(item: D): IO[Unit] =
    (entityMapper.toEntity _ andThen saveEntity)(item)

  private def saveEntity(entity: E): IO[Unit] = {
    val result = itemCollection.flatMap{ collection =>
      collection
        .insert(ordered = false).
        one(entity)
    }
      .map(_ => ().asRight[ApiClientError])
      .recover(ApiClientError.recoverFromDbError.andThen(_.asLeft))
    ApiClientError.fromFutureErrorToIO(result)
  }

  def findAll(limit: Option[Int] = None, from: Option[Instant] = None): IO[Seq[D]] =
    findAllEntities(limit, from).map(_.map(entityMapper.toDomain))

  private def findAllEntities(limit: Option[Int], from: Option[Instant]): IO[Seq[E]] = {
    val result = itemCollection.flatMap { collection =>
      collection
        .find(selector = from.fold(BSONDocument())(postedAfterSelector), projection = Option.empty[JsObject])
        .sort(Json.obj("listingDetails.datePosted" -> -1))
        .cursor[E](ReadPreference.primary)
        .collect[Seq](limit.getOrElse(1000), Cursor.FailOnError[Seq[E]]())
    }
      .map(_.asRight)
      .recover(ApiClientError.recoverFromDbError.andThen(_.asLeft))
    ApiClientError.fromFutureErrorToIO(result)
  }

  private def postedAfterSelector(from: Instant): BSONDocument =
    BSONDocument("listingDetails.datePosted" -> BSONDocument("$gte" -> BSONString(from.toString)))
}
