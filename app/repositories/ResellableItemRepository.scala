package repositories

import java.time.Instant

import cats.data.EitherT
import cats.effect.{ContextShift, IO}
import cats.implicits._
import domain.{ApiClientError, ResellableItem}
import domain.ApiClientError.IOErrorOr
import play.api.libs.json.{JsObject, Json, OFormat}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{Cursor, ReadConcern, ReadPreference}
import reactivemongo.bson.{BSONDocument, BSONString}
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json._

import scala.concurrent.{ExecutionContext, Future}

trait ResellableItemRepository[A <: ResellableItem, B <: ResellableItemEntity] {
  implicit protected def ex: ExecutionContext
  implicit protected def mongo: ReactiveMongoApi
  implicit protected def entityMapper: ResellableItemEntityMapper[A, B]
  implicit protected def entityFormat: OFormat[B]
  implicit private val cs: ContextShift[IO] = IO.contextShift(ex)

  protected def collectionName: String
  protected val itemCollection: Future[JSONCollection] = mongo.database.map(_.collection(collectionName))

  def existsByUrl(listingUrl: String): IOErrorOr[Boolean] = {
    val result = itemCollection.flatMap { collection =>
      collection
        .withReadPreference(ReadPreference.primary)
        .count(Some(Json.obj("listingDetails.url" -> listingUrl.toString)), None, 0, None, ReadConcern.Available)
        .map(_ > 0)
        .map(_.asRight[ApiClientError])
    }
      .recover(ApiClientError.recoverFromDbError.andThen(_.asLeft))
    IO.fromFuture(IO(result))
  }

  def save(item: A): IOErrorOr[Unit] =
    (entityMapper.toEntity _ andThen saveEntity)(item)

  private def saveEntity(entity: B): IOErrorOr[Unit] = {
    val result = itemCollection.flatMap{ collection =>
      collection
        .insert(ordered = false).
        one(entity)
    }
      .map(_ => ().asRight[ApiClientError])
      .recover(ApiClientError.recoverFromDbError.andThen(_.asLeft))
    IO.fromFuture(IO(result))
  }

  def findAll(limit: Int = 100): IOErrorOr[Seq[A]] =
    EitherT(findAllEntities(limit)).map(_.map(entityMapper.toDomain)).value

  private def findAllEntities(limit: Int): IOErrorOr[Seq[B]] = {
    val result = itemCollection.flatMap { collection =>
      collection
        .find(selector = Json.obj(), projection = Option.empty[JsObject])
        .sort(Json.obj("listingDetails.datePosted" -> -1))
        .cursor[B](ReadPreference.primary)
        .collect[Seq](limit, Cursor.FailOnError[Seq[B]]())
    }
      .map(_.asRight)
      .recover(ApiClientError.recoverFromDbError.andThen(_.asLeft))
    IO.fromFuture(IO(result))
  }

  def findAllPostedAfter(date: Instant, limit: Int = 1000): IOErrorOr[Seq[A]] =
    EitherT(findAllEntitiesPostedAfter(date, limit)).map(_.map(entityMapper.toDomain)).value

  private def findAllEntitiesPostedAfter(date: Instant, limit: Int): IOErrorOr[Seq[B]] = {
    val result = itemCollection.flatMap { collection =>
      collection
        .find(selector = BSONDocument("listingDetails.datePosted" -> BSONDocument("$gte" -> BSONString(date.toString))), projection = Option.empty[JsObject])
        .cursor[B](ReadPreference.primary)
        .collect[Seq](limit, Cursor.FailOnError[Seq[B]]())
    }
      .map(_.asRight)
      .recover(ApiClientError.recoverFromDbError.andThen(_.asLeft))
    IO.fromFuture(IO(result))
  }

}
