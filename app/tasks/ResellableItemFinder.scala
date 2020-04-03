package tasks

import cats.effect.IO
import cats.implicits._
import domain.{ItemDetails, ResellableItem}
import fs2.Stream
import play.api.Logger
import repositories.ResellableItemEntity
import services.ResellableItemService

import scala.concurrent.ExecutionContext

trait ResellableItemFinder[I <: ResellableItem, D <: ItemDetails, E <: ResellableItemEntity] {
  private val logger: Logger = Logger(getClass)

  implicit protected def ex: ExecutionContext

  protected def minMarginPercentage: Int

  protected def itemService: ResellableItemService[I, D, E]

  def searchForCheapItems(): Stream[IO, I] =
    itemService.getLatestFromEbay(15)
      .evalFilter(itemService.isNew)
      .evalMap(item => itemService.save(item) *> IO.pure(item))
      .filter(item => item.itemDetails.isBundle || isProfitableToResell(item))
      .evalMap(item => itemService.sendNotification(item) *> IO.pure(item))
      .handleErrorWith { error =>
        logger.error(s"error obtaining new items from ebay: ${error.getMessage}", error)
        Stream.empty
      }

  private val isProfitableToResell: I => Boolean =
    item => item.resellPrice.exists(rp => (rp.exchange * 100 / item.listingDetails.price - 100) > minMarginPercentage)
}
