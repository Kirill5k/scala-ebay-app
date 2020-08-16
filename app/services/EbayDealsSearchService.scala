package services

import java.time.Instant

import cats.effect.IO
import cats.implicits._
import clients.cex.CexClient
import clients.ebay.mappers._
import clients.ebay.params._
import clients.ebay.EbaySearchClient
import common.Logging
import domain.{ItemDetails, ResellableItem, SearchQuery}
import fs2.Stream
import javax.inject.Inject

import scala.concurrent.ExecutionContext

trait EbayDealsSearchService[D <: ItemDetails] extends Logging {

  implicit protected def ebayItemMapper: EbayItemMapper[D]
  implicit protected def ebaySearchParams: EbaySearchParams[D]

  protected def ebaySearchClient: EbaySearchClient
  protected def cexClient: CexClient

  def searchEbay(query: SearchQuery, minutes: Int): Stream[IO, ResellableItem[D]] =
    ebaySearchClient
      .findItemsListedInLastMinutes[D](query, minutes)
      .evalMap {
        case i =>
          i.itemDetails.fullName match {
            case Some(name) =>
              cexClient.findResellPrice(SearchQuery(name)).map(rp => i.copy(resellPrice = rp))
            case None =>
              IO(logger.warn(s"not enough details to query for resell price ${i.itemDetails}")) *>
                IO.pure(i)
          }
      }
}

class EbayVideoGameSearchService @Inject()(
    override val ebaySearchClient: EbaySearchClient,
    override val cexClient: CexClient
)(
    implicit ex: ExecutionContext
) extends EbayDealsSearchService[ItemDetails.Game] {
  override implicit protected def ebaySearchParams: EbaySearchParams[ItemDetails.Game] = videoGameSearchParams
  override implicit protected def ebayItemMapper: EbayItemMapper[ItemDetails.Game] = EbayItemMapper.gameDetailsMapper
}
