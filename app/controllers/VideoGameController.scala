package controllers

import java.time.Instant

import controllers.VideoGameController.{ResellableItemsSummaryResponse, VideoGameResponse}
import domain.ResellableItem.VideoGame
import domain.{ItemDetails, ListingDetails, Packaging, Price, ResellPrice, ResellableItem}
import common.json._
import domain.ItemDetails.Game
import io.circe._
import io.circe.generic.extras.auto._
import io.circe.generic.extras._
import io.circe.parser._
import io.circe.syntax._
import javax.inject.{Inject, Singleton}
import play.api.http.ContentTypes
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, Result}
import services.EbayVideoGameSearchService

import scala.concurrent.ExecutionContext

@Singleton
class VideoGameController @Inject()(
    itemService: EbayVideoGameSearchService,
    override val controllerComponents: ControllerComponents
)(
    implicit ex: ExecutionContext
) extends BaseController {

  def getAll(limit: Option[Int], from: Option[Instant], to: Option[Instant]): Action[AnyContent] = Action.async {
    itemService
      .get(limit, from, to)
      .map(_.map(VideoGameResponse.from))
      .map(toSuccessResult[List[VideoGameResponse]])
      .unsafeToFuture()
      .recover {
        case error => toErrorResult(error)
      }
  }

  def summary(from: Option[Instant], to: Option[Instant]): Action[AnyContent] = Action.async {
    itemService
      .get(None, from, to)
      .map(VideoGameController.resellableItemsSummaryResponse)
      .map(toSuccessResult[ResellableItemsSummaryResponse])
      .unsafeToFuture()
      .recover {
        case error => toErrorResult(error)
      }
  }

  private def toSuccessResult[A: Encoder](result: A): Result =
    Ok(result.asJson.noSpaces).as(ContentTypes.JSON)

  private def toErrorResult(error: Throwable): Result =
    InternalServerError(ErrorResponse(error.getMessage).asJson.noSpaces).as(ContentTypes.JSON)
}

object VideoGameController {
  final case class VideoGameResponse(
      itemDetails: Game,
      listingDetails: ListingDetails,
      price: Price,
      resellPrice: Option[ResellPrice]
  )

  object VideoGameResponse {
    def from(game: VideoGame): VideoGameResponse =
      VideoGameResponse(game.itemDetails, game.listingDetails, game.price, game.resellPrice)
  }

  final case class ItemSummary(
      name: Option[String],
      url: String,
      price: BigDecimal
  )
  final case class ItemsSummary(total: Int, items: Seq[ItemSummary])
  final case class ResellableItemsSummaryResponse(
      total: Int,
      unrecognized: ItemsSummary,
      profitable: ItemsSummary,
      rest: ItemsSummary
  )

  def resellableItemsSummaryResponse[D <: ItemDetails](items: Seq[ResellableItem[D]]): ResellableItemsSummaryResponse = {
    val withoutResellPrice  = items.filter(_.resellPrice.isEmpty)
    val profitableForResell = items.filter(i => i.resellPrice.exists(rp => rp.cash > i.price.value))
    val rest                = items.filter(i => !withoutResellPrice.contains(i) && !profitableForResell.contains(i))
    ResellableItemsSummaryResponse(
      items.size,
      toItemsSummary(withoutResellPrice),
      toItemsSummary(profitableForResell),
      toItemsSummary(rest)
    )
  }

  private def toItemsSummary[D <: ItemDetails](items: Seq[ResellableItem[D]]): ItemsSummary =
    ItemsSummary(
      items.size,
      items.map(i => ItemSummary(i.itemDetails.fullName, i.listingDetails.url, i.price.value))
    )
}
