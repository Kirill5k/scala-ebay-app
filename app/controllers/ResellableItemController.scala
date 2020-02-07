package controllers

import domain.{ItemDetails, ResellableItem}
import play.api.mvc.{Action, AnyContent, BaseController}
import play.api.libs.json._
import repositories.ResellableItemEntity
import services.ResellableItemService

import scala.concurrent.ExecutionContext

trait ResellableItemController[I <: ResellableItem, D <: ItemDetails, E <: ResellableItemEntity] extends BaseController {
  implicit protected def ex: ExecutionContext

  protected def minMarginPercentage: Int

  protected def itemService: ResellableItemService[I, D, E]

  def getAll(limit: Int = 100): Action[AnyContent] = Action.async {
    itemService.getLatest(100).value.map{
      case Right(items) => Ok(Json.toJson(items))
      case Left(error) => InternalServerError(error)
    }
  }
}
