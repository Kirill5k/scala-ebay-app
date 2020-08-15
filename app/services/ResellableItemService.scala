package services

import java.time.Instant

import cats.effect.IO
import cats.implicits._
import clients.cex.CexClient
import clients.ebay.{EbaySearchClient, VideoGameEbayClient}
import common.Logging
import domain.{ItemDetails, ResellableItem, SearchQuery}
import fs2.Stream
import javax.inject.Inject
import repositories.{ResellableItemRepository, VideoGameRepository}

import scala.concurrent.ExecutionContext

trait ResellableItemService[D <: ItemDetails] extends Logging {

  protected def itemRepository: ResellableItemRepository[D]
  protected def ebaySearchClient: EbaySearchClient[D]
  protected def cexClient: CexClient

  def searchEbay(query: SearchQuery, minutes: Int): Stream[IO, ResellableItem[D]] =
    ebaySearchClient
      .findItemsListedInLastMinutes(query, minutes)
      .evalMap {
        case (id, ld) =>
          id.fullName match {
            case Some(summary) =>
              cexClient.findResellPrice(SearchQuery(summary)).map(rp => ResellableItem[D](id, ld, rp))
            case None =>
              IO(logger.warn(s"not enough details to query for resell price $id")) *>
                IO(ResellableItem[D](id, ld, None))
          }
      }

  def save(item: ResellableItem[D]): IO[Unit] =
    itemRepository.save(item)

  def get(limit: Option[Int], from: Option[Instant], to: Option[Instant]): IO[List[ResellableItem[D]]] =
    itemRepository.findAll(limit, from, to)

  def isNew(item: ResellableItem[D]): IO[Boolean] =
    itemRepository.existsByUrl(item.listingDetails.url).map(!_)
}

class VideoGameService @Inject()(
    override val itemRepository: VideoGameRepository,
    override val ebaySearchClient: VideoGameEbayClient,
    override val cexClient: CexClient
)(
    implicit ex: ExecutionContext
) extends ResellableItemService[ItemDetails.Game] {}
