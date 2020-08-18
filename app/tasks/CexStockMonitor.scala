package tasks

import akka.actor.ActorSystem
import cats.effect.IO
import common.Logging
import domain.{ItemDetails, SearchQuery, StockMonitorRequest, StockUpdate}
import fs2.Stream
import javax.inject.Inject
import services.{CexStockSearchService, GenericPurchasableItemService, NotificationService, TelegramNotificationService}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait CexStockMonitor[D <: ItemDetails] extends Logging {

  protected def searchQueries: List[StockMonitorRequest]

  protected def itemService: CexStockSearchService[IO, D]
  protected def notificationService: NotificationService[IO]

  def checkCexStock(): Stream[IO, StockUpdate[D]] =
    fs2.Stream
      .emits(searchQueries)
      .evalMap(itemService.getStockUpdates)
      .flatMap(updates => Stream.emits(updates))
      .evalTap(notificationService.stockUpdate[D])
      .handleErrorWith { error =>
        logger.error(s"error obtaining stock updates from cex: ${error.getMessage}", error)
        Stream.empty
      }
}

final class CexGenericStockMonitor @Inject()(
    override val itemService: GenericPurchasableItemService,
    override val notificationService: TelegramNotificationService,
    actorSystem: ActorSystem
)(
    implicit val ex: ExecutionContext
) extends CexStockMonitor[ItemDetails.Generic] {

  override protected val searchQueries: List[StockMonitorRequest] = List(
    StockMonitorRequest(SearchQuery("macbook pro 16,1")),
    StockMonitorRequest(SearchQuery("xbox 360 grand theft auto 5"), false, true),
  )

  actorSystem.scheduler.scheduleWithFixedDelay(initialDelay = 1.minutes, delay = 10.minutes) { () =>
    checkCexStock().compile.drain.unsafeRunSync
  }
}
