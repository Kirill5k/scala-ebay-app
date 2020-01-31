package repositories

import java.net.URI
import java.time.Instant

import cats.data.EitherT
import cats.implicits._
import domain.{ApiClientError, ResellableItem}
import domain.ApiClientError.FutureErrorOr
import play.api.libs.json.{JsObject, Json, OFormat}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{Cursor, ReadConcern, ReadPreference}
import reactivemongo.bson.{BSONDocument, BSONString}
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json._

import scala.concurrent.{ExecutionContext, Future}

private[repositories] trait ResellableItemRepository[A <: ResellableItem, B <: ResellableItemEntity] {
  implicit protected def ex: ExecutionContext
  implicit protected def mongo: ReactiveMongoApi
  implicit protected def entityMapper: ResellableItemEntityMapper[A, B]
  implicit protected def entityFormat: OFormat[B]

  protected def collectionName: String
  protected val itemCollection: Future[JSONCollection] = mongo.database.map(_.collection(collectionName))

  def existsByUrl(listingUrl: URI): FutureErrorOr[Boolean] = {
    val result = itemCollection.flatMap { collection =>
      collection
        .withReadPreference(ReadPreference.primary)
        .count(Some(Json.obj("listingDetails.url" -> listingUrl.toString)), None, 0, None, ReadConcern.Available)
        .map(_ > 0)
        .map(_.asRight[ApiClientError])
    }
      .recover(ApiClientError.recoverFromDbError.andThen(_.asLeft))
    EitherT(result)
  }

  def save(item: A): FutureErrorOr[Unit] =
    (entityMapper.toEntity _ andThen saveEntity)(item)

  private def saveEntity(entity: B): FutureErrorOr[Unit] = {
    val result = itemCollection.flatMap{ collection =>
      collection
        .insert(ordered = false).
        one(entity)
    }
      .map(_ => ().asRight[ApiClientError])
      .recover(ApiClientError.recoverFromDbError.andThen(_.asLeft))
    EitherT(result)
  }

  def findAll(limit: Int = 100): FutureErrorOr[Seq[A]] =
    findAllEntities(limit).map(_.map(entityMapper.toDomain))

  private def findAllEntities(limit: Int): FutureErrorOr[Seq[B]] = {
    val result = itemCollection.flatMap { collection =>
      collection
        .find(selector = Json.obj(), projection = Option.empty[JsObject])
        .sort(Json.obj("listingDetails.datePosted" -> -1))
        .cursor[B](ReadPreference.primary)
        .collect[Seq](limit, Cursor.FailOnError[Seq[B]]())
    }
      .map(_.asRight)
      .recover(ApiClientError.recoverFromDbError.andThen(_.asLeft))
    EitherT(result)
  }

  def findAllPostedAfter(date: Instant, limit: Int = 1000): FutureErrorOr[Seq[A]] =
    findAllEntitiesPostedAfter(date, limit).map(_.map(entityMapper.toDomain))

  private def findAllEntitiesPostedAfter(date: Instant, limit: Int): FutureErrorOr[Seq[B]] = {
    val result = itemCollection.flatMap { collection =>
      collection
        .find(selector = BSONDocument("listingDetails.datePosted" -> BSONDocument("$gte" -> BSONString(date.toString))), projection = Option.empty[JsObject])
        .cursor[B](ReadPreference.primary)
        .collect[Seq](limit, Cursor.FailOnError[Seq[B]]())
    }
      .map(_.asRight)
      .recover(ApiClientError.recoverFromDbError.andThen(_.asLeft))
    EitherT(result)
  }

}
