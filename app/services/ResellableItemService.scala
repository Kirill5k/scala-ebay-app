package services

import java.time.Instant

import cats.effect.{IO, Timer}
import clients.cex.CexClient
import clients.ebay.EbaySearchClient
import clients.telegram.TelegramClient
import domain.{ItemDetails, ListingDetails, ResellPrice, ResellableItem}
import fs2.Stream
import repositories.{ResellableItemEntity, ResellableItemRepository}

trait ResellableItemService[I <: ResellableItem, D <: ItemDetails, E <: ResellableItemEntity] {
  implicit protected def timer: Timer[IO]

  protected def itemRepository: ResellableItemRepository[I, E]
  protected def ebaySearchClient: EbaySearchClient[D]
  protected def telegramClient: TelegramClient
  protected def cexClient: CexClient

  protected def createItem(itemDetails: D, listingDetails: ListingDetails, resellPrice: Option[ResellPrice]): I

  def getLatestFromEbay(minutes: Int): Stream[IO, I] =
    ebaySearchClient.getItemsListedInLastMinutes(minutes)
      .evalMap { case (id, ld) => cexClient.findResellPrice(id).map(rp => createItem(id, ld, rp)) }

  def sendNotification(item: I): IO[Unit] =
    telegramClient.sendMessageToMainChannel(item)

  def save(item: I): IO[Unit] =
    itemRepository.save(item)

  def getLatest(limit: Option[Int], from: Option[Instant], to: Option[Instant]): IO[Seq[I]] =
    itemRepository.findAll(limit, from, to)

  def isNew(item: I): IO[Boolean] =
    itemRepository.existsByUrl(item.listingDetails.url).map(!_)
}
