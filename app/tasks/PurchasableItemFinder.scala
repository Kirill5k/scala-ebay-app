package tasks

import akka.actor.ActorSystem
import cats.effect.IO
import common.Logging
import domain.PurchasableItem.GenericPurchasableItem
import domain.{ItemDetails, PurchasableItem, SearchQuery, StockUpdate}
import fs2.Stream
import javax.inject.Inject
import services.{GenericPurchasableItemService, NotificationService, PurchasableItemService, TelegramNotificationService}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait PurchasableItemFinder[D <: ItemDetails] extends Logging {

  protected def searchQueries: List[SearchQuery]

  protected def itemService: PurchasableItemService[IO, D]
  protected def notificationService: NotificationService[IO]

  def checkCexStock(): Stream[IO, StockUpdate[D]] =
    fs2.Stream
      .emits(searchQueries)
      .evalMap(itemService.getStockUpdatesFromCex)
      .flatMap(updates => Stream.emits(updates))
      .evalTap(notificationService.stockUpdate[D])
      .handleErrorWith { error =>
        logger.error(s"error obtaining stock updates from cex: ${error.getMessage}", error)
        Stream.empty
      }
}

final class GenericPurchasableItemFinder @Inject()(
    override val itemService: GenericPurchasableItemService,
    override val notificationService: TelegramNotificationService,
    actorSystem: ActorSystem
)(
    implicit val ex: ExecutionContext
) extends PurchasableItemFinder[ItemDetails.Generic] {

  override protected val searchQueries: List[SearchQuery] = List(
    SearchQuery("macbook pro 16,1")
  )

  actorSystem.scheduler.scheduleWithFixedDelay(initialDelay = 1.minutes, delay = 20.minutes) { () =>
    checkCexStock().compile.drain.unsafeRunSync
  }
}
