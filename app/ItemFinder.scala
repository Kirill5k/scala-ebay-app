import cats.data.EitherT
import cats.implicits._
import domain.ApiClientError.FutureErrorOr
import domain.{ItemDetails, ResellableItem}
import play.api.Logger
import repositories.ResellableItemEntity
import services.ResellableItemService

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait ItemFinder[I <: ResellableItem, D <: ItemDetails, E <: ResellableItemEntity] {
  private val logger: Logger = Logger(getClass)

  implicit protected def ex: ExecutionContext

  protected def minMarginPercentage: Int

  protected def itemService: ResellableItemService[I, D, E]


  def searchForCheapItems(): Unit = {
    itemService.getLatestFromEbay(15)
      .map(_.toList)
      .flatMap(_.map(isNew).sequence)
      .map(_.flatten)
      .flatMap(_.map(save).sequence)
      .map(_.filter(isProfitableToResell))
      .flatMap(_.map(informAboutGoodDeal).sequence)
      .value
      .onComplete {
        case Success(Right(items)) => logger.info(s"found ${items.size} new good deals")
        case Success(Left(error)) => logger.error(s"error obtaining new items from ebay: ${error.message}")
        case Failure(exception) => logger.error(s"exception trying to obtain new items from ebay: ${exception.getMessage}")
      }
  }

  private val isNew: I => FutureErrorOr[Option[I]] =
    item => itemService.isNew(item).map(if (_) Some(item) else None)

  private val save: I => FutureErrorOr[I] =
    item => itemService.save(item).map(_ => item)

  private val isProfitableToResell: I => Boolean =
    item => item.resellPrice.exists(rp => (rp.exchange * 100 / item.listingDetails.price - 100) > minMarginPercentage)

  private val informAboutGoodDeal: I => FutureErrorOr[I] =
    item => itemService.sendNotification(item).map(_ => item)
}
