package controllers

import javax.inject.{Inject, Singleton}
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import play.api.http.ContentTypes
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import services.VideoGameService

import scala.concurrent.ExecutionContext

@Singleton
class VideoGameController @Inject()(itemService: VideoGameService, override val controllerComponents: ControllerComponents)(implicit ex: ExecutionContext)
  extends BaseController {

  def getAll(limit: Int = 100): Action[AnyContent] = Action.async {
    itemService.getLatest(100)
      .unsafeToFuture()
      .map(items => Ok(items.asJson.noSpaces).as(ContentTypes.JSON))
      .recover(error => InternalServerError(error.asJson.noSpaces).as(ContentTypes.JSON))
    }

}
