package controllers

import controllers.ControllerResponse.ErrorResponse
import io.circe.generic.auto._
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import javax.inject.{Inject, Singleton}
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
      .recover(error => InternalServerError(ErrorResponse(error.getMessage).asJson.noSpaces).as(ContentTypes.JSON))
  }

}
