import cats.data.EitherT
import cats.implicits._

import domain.{ItemDetails, ResellableItem}
import repositories.ResellableItemEntity
import services.ResellableItemService

import scala.concurrent.ExecutionContext

trait ItemFinder[I <: ResellableItem, D <: ItemDetails, E <: ResellableItemEntity] {
  implicit protected def ex: ExecutionContext

  protected def itemService: ResellableItemService[I, D, E]

  def searchForCheapItems: Unit
}
