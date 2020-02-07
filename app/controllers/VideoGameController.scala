package controllers

import domain.ItemDetails.GameDetails
import domain.ResellableItem.VideoGame
import javax.inject.{Inject, Singleton}
import play.api.mvc.ControllerComponents
import repositories.ResellableItemEntity.VideoGameEntity
import services.VideoGameService

import scala.concurrent.ExecutionContext

@Singleton
class VideoGameController @Inject()(
                                     override val itemService: VideoGameService,
                                     override val controllerComponents: ControllerComponents)
                                   (implicit override val ex: ExecutionContext)
  extends ResellableItemController[VideoGame, GameDetails, VideoGameEntity] {

}
