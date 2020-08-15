package tasks

import akka.actor.ActorSystem
import cats.effect.IO
import common.Logging
import domain.{ItemDetails, ResellableItem, SearchQuery}
import fs2.Stream
import javax.inject.Inject
import services.{NotificationService, ResellableItemService, TelegramNotificationService, VideoGameService}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait ResellableItemFinder[D <: ItemDetails] extends Logging {

  protected def minMarginPercentage: Int
  protected def searchQueries: List[SearchQuery]

  protected def itemService: ResellableItemService[D]
  protected def notificationService: NotificationService[IO]

  def searchForCheapItems(): Stream[IO, ResellableItem[D]] = {
    fs2.Stream.emits(searchQueries)
      .flatMap(itemService.searchEbay(_, 15))
      .evalFilter(itemService.isNew)
      .evalTap(itemService.save)
      .filter(isProfitableToResell)
      .evalTap(notificationService.cheapItem)
      .handleErrorWith { error =>
        logger.error(s"error obtaining new items from ebay: ${error.getMessage}", error)
        Stream.empty
      }
  }

  private val isProfitableToResell: ResellableItem[D] => Boolean =
    item => item.resellPrice.exists(rp => (rp.exchange * 100 / item.listingDetails.price - 100) > minMarginPercentage)
}

final class VideoGamesFinder @Inject()(
    override val itemService: VideoGameService,
    override val notificationService: TelegramNotificationService,
    actorSystem: ActorSystem
)(
    implicit val ex: ExecutionContext
) extends ResellableItemFinder[ItemDetails.Game] {

  override protected val minMarginPercentage: Int = 15
  override protected val searchQueries: List[SearchQuery] = List(
    SearchQuery("PS3"),
    SearchQuery("PS4"),
    SearchQuery("XBOX ONE"),
    SearchQuery("SWITCH"),
    SearchQuery("XBOX 360"),
    SearchQuery("WII")
  )

  actorSystem.scheduler.scheduleWithFixedDelay(initialDelay = 5.seconds, delay = 60.seconds) { () =>
    searchForCheapItems().compile.drain.unsafeRunSync
  }
}
