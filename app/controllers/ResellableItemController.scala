package controllers

import domain.{ItemDetails, ResellableItem}
import play.api.mvc.BaseController
import repositories.ResellableItemEntity
import services.ResellableItemService

import scala.concurrent.ExecutionContext

trait ResellableItemController[I <: ResellableItem, D <: ItemDetails, E <: ResellableItemEntity] extends BaseController {
  implicit protected def ex: ExecutionContext

  protected def minMarginPercentage: Int

  protected def itemService: ResellableItemService[I, D, E]

}
