package repositories

import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

trait ResellableItemRepository {

  implicit protected val ex: ExecutionContext
  implicit protected val mongo: ReactiveMongoApi

  protected val collectionName: String
  protected val itemCollection: Future[JSONCollection] = mongo.database.map(_.collection(collectionName))
}
