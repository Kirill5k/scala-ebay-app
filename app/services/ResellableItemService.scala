package services

import java.time.Instant

import cats.effect.{IO, Timer}
import clients.cex.CexClient
import clients.ebay.{EbaySearchClient, VideoGameEbayClient}
import clients.telegram.TelegramClient
import domain.ItemDetails.GameDetails
import domain.ResellableItem.VideoGame
import domain.{ItemDetails, ListingDetails, ResellPrice, ResellableItem}
import fs2.Stream
import javax.inject.Inject
import repositories.ResellableItemEntity.VideoGameEntity
import repositories.{ResellableItemEntity, ResellableItemRepository, VideoGameRepository}

import scala.concurrent.ExecutionContext

trait ResellableItemService[I <: ResellableItem, D <: ItemDetails, E <: ResellableItemEntity] {

  protected def itemRepository: ResellableItemRepository[I, E]
  protected def ebaySearchClient: EbaySearchClient[D]
  protected def cexClient: CexClient

  protected def createItem(itemDetails: D, listingDetails: ListingDetails, resellPrice: Option[ResellPrice]): I

  def getLatestFromEbay(minutes: Int): Stream[IO, I] =
    ebaySearchClient
      .getItemsListedInLastMinutes(minutes)
      .evalMap { case (id, ld) => cexClient.findResellPrice(id).map(rp => createItem(id, ld, rp)) }

  def save(item: I): IO[Unit] =
    itemRepository.save(item)

  def getLatest(limit: Option[Int], from: Option[Instant], to: Option[Instant]): IO[Seq[I]] =
    itemRepository.findAll(limit, from, to)

  def isNew(item: I): IO[Boolean] =
    itemRepository.existsByUrl(item.listingDetails.url).map(!_)
}

class VideoGameService @Inject()(
    override val itemRepository: VideoGameRepository,
    override val ebaySearchClient: VideoGameEbayClient,
    override val cexClient: CexClient
)(implicit ex: ExecutionContext)
    extends ResellableItemService[VideoGame, GameDetails, VideoGameEntity] {

  override protected def createItem(
      itemDetails: GameDetails,
      listingDetails: ListingDetails,
      resellPrice: Option[ResellPrice]
  ): VideoGame =
    VideoGame.apply(itemDetails, listingDetails, resellPrice)
}
