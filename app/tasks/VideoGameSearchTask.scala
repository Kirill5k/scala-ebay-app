package tasks

import akka.actor.ActorSystem
import domain.ItemDetails.GameDetails
import domain.ResellableItem.VideoGame
import javax.inject.Inject
import repositories.ResellableItemEntity.VideoGameEntity
import services.VideoGameService

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class VideoGameSearchTask @Inject()(override val itemService: VideoGameService, actorSystem: ActorSystem)(implicit override val ex: ExecutionContext)
  extends ResellableItemFinder[VideoGame, GameDetails, VideoGameEntity] {

  override protected def minMarginPercentage: Int = 0

  actorSystem.scheduler.scheduleWithFixedDelay(initialDelay = 5 seconds, delay = 60 seconds) { () => searchForCheapItems() }
}
