package controllers

import domain.{ItemDetails, ResellableItem}
import play.api.mvc.{Action, AnyContent, BaseController}
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import repositories.ResellableItemEntity
import services.ResellableItemService

import scala.concurrent.ExecutionContext

trait ResellableItemController[I <: ResellableItem, D <: ItemDetails, E <: ResellableItemEntity] extends BaseController {
  implicit protected def ex: ExecutionContext
  implicit protected def je: Encoder[I]

  protected def itemService: ResellableItemService[I, D, E]

  def getAll(limit: Int = 100): Action[AnyContent] = Action.async {
    itemService.getLatest(100).value.map{
      case Right(items) => Ok(items.asJson.noSpaces)
      case Left(error) => InternalServerError(error.asJson.noSpaces)
    }
  }
}
