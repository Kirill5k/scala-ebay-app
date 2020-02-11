package services

import cats.effect.{IO, Timer}
import clients.cex.CexClient
import clients.ebay.EbaySearchClient
import clients.telegram.TelegramClient
import domain.{ItemDetails, ListingDetails, ResellPrice, ResellableItem}
import fs2.Stream
import repositories.{ResellableItemEntity, ResellableItemRepository}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

trait ResellableItemService[I <: ResellableItem, D <: ItemDetails, E <: ResellableItemEntity] {
  implicit protected def ex: ExecutionContext
  implicit private val timer: Timer[IO] = IO.timer(ex)

  protected def itemRepository: ResellableItemRepository[I, E]
  protected def ebaySearchClient: EbaySearchClient[D]
  protected def telegramClient: TelegramClient
  protected def cexClient: CexClient

  protected def createItem(itemDetails: D, listingDetails: ListingDetails, resellPrice: Option[ResellPrice]): I

  def getLatestFromEbay(minutes: Int): Stream[IO, I] =
    ebaySearchClient.getItemsListedInLastMinutes(minutes)
      .delayBy(400 milliseconds)
      .evalMap { case (id, ld) => cexClient.findResellPrice(id).map(rp => createItem(id, ld, rp)) }

  def sendNotification(item: I): IO[Unit] =
    telegramClient.sendMessageToMainChannel(item)

  def save(item: I): IO[Unit] =
    itemRepository.save(item)

  def getLatest(limit: Int): IO[Seq[I]] =
    itemRepository.findAll(limit)

  def isNew(item: I): IO[Boolean] =
    itemRepository.existsByUrl(item.listingDetails.url).map(!_)
}
