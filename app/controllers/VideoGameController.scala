package controllers

import java.time.Instant

import controllers.ErrorResponse.GeneralErrorResponse
import io.circe._
import io.circe.generic.extras.auto._
import io.circe.generic.extras._
import io.circe.parser._
import io.circe.syntax._
import javax.inject.{Inject, Singleton}
import play.api.http.ContentTypes
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, Result}
import services.VideoGameService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VideoGameController @Inject()(itemService: VideoGameService, override val controllerComponents: ControllerComponents)(implicit ex: ExecutionContext)
  extends BaseController {

  implicit val genDevConfig: Configuration = Configuration.default.withDiscriminator("_type")

  def getAll(limit: Option[Int], from: Option[Instant]): Action[AnyContent] = Action.async {
    toResponse(itemService.getLatest(limit, from).unsafeToFuture())
  }

  def summary(from: Option[Instant]): Action[AnyContent] = Action.async {
    toResponse(itemService.getLatest(None, from)
      .map(ResellableItemResponse.itemsSummary)
      .unsafeToFuture())
  }

  private def toResponse[A: Encoder](result: Future[A]): Future[Result] =
    result
      .map(r => Ok(r.asJson.noSpaces).as(ContentTypes.JSON))
      .recover(error => InternalServerError(GeneralErrorResponse(error.getMessage).asJson.noSpaces).as(ContentTypes.JSON))
}
