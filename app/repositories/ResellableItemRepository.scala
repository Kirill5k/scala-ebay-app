package repositories

import java.net.URI
import java.time.Instant

import cats.data.EitherT
import cats.implicits._
import domain.ApiClientError
import domain.ApiClientError.FutureErrorOr
import play.api.libs.json.{JsObject, Json, OFormat}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.{Cursor, ReadConcern, ReadPreference}
import reactivemongo.bson.{BSONDocument, BSONString}
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json._

import scala.concurrent.{ExecutionContext, Future}

trait ResellableItemRepository[E] {
  implicit protected def ex: ExecutionContext
  implicit protected def mongo: ReactiveMongoApi

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
      .recover(ApiClientError.recoverFromHttpCallFailure.andThen(_.asLeft))
    EitherT(result)
  }

  protected def saveEntity(entity: E)(implicit f: OFormat[E]): FutureErrorOr[Unit] = {
    val result = itemCollection.flatMap(_.insert(ordered = false).one(entity).map(_ => ().asRight[ApiClientError]))
    EitherT(result)
  }

  protected def findAllEntities(limit: Int)(implicit f: OFormat[E]): FutureErrorOr[Seq[E]] = {
    val result = itemCollection.flatMap { collection =>
      collection
        .find(selector = Json.obj(), projection = Option.empty[JsObject])
        .sort(Json.obj("listingDetails.datePosted" -> -1))
        .cursor[E](ReadPreference.primary)
        .collect[Seq](limit, Cursor.FailOnError[Seq[E]]())
    }
      .map(_.asRight)
      .recover(ApiClientError.recoverFromHttpCallFailure.andThen(_.asLeft))
    EitherT(result)
  }

  protected def findAllEntitiesPostedAfter(date: Instant, limit: Int)(implicit f: OFormat[E]): FutureErrorOr[Seq[E]] = {
    val result = itemCollection.flatMap { collection =>
      collection
        .find(selector = BSONDocument("listingDetails.datePosted" -> BSONDocument("$gte" -> BSONString(date.toString))), projection = Option.empty[JsObject])
        .cursor[E](ReadPreference.primary)
        .collect[Seq](limit, Cursor.FailOnError[Seq[E]]())
    }
      .map(_.asRight)
      .recover(ApiClientError.recoverFromHttpCallFailure.andThen(_.asLeft))
    EitherT(result)
  }

}
