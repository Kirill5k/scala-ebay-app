package tasks

import cats.effect.IO
import common.Logging
import domain.{PurchasableItem, SearchQuery, StockUpdate}
import fs2.Stream
import services.{NotificationService, PurchasableItemService}

trait PurchasableItemFinder[I <: PurchasableItem] extends Logging {

  protected def searchQueries: List[SearchQuery]

  protected def itemService: PurchasableItemService[IO, I]
  protected def notificationService: NotificationService[IO]

  def checkCexStock(): Stream[IO, StockUpdate[I]] = {
    fs2.Stream.emits(searchQueries)
      .evalMap(itemService.getStockUpdatesFromCex)
      .flatMap(updates => Stream.emits(updates))
      .evalTap(notificationService.stockUpdate)
      .handleErrorWith { error =>
        logger.error(s"error obtaining stock updates from cex: ${error.getMessage}", error)
        Stream.empty
      }
  }
}
