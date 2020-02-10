package services

import cats.data.EitherT
import cats.implicits._
import clients.cex.CexClient
import clients.ebay.EbaySearchClient
import clients.telegram.TelegramClient
import domain.ApiClientError.IOErrorOr
import domain.{ItemDetails, ListingDetails, ResellPrice, ResellableItem}
import repositories.{ResellableItemEntity, ResellableItemRepository}

import scala.concurrent.ExecutionContext

trait ResellableItemService[I <: ResellableItem, D <: ItemDetails, E <: ResellableItemEntity] {
  implicit protected def ex: ExecutionContext

  protected def itemRepository: ResellableItemRepository[I, E]
  protected def ebaySearchClient: EbaySearchClient[D]
  protected def telegramClient: TelegramClient
  protected def cexClient: CexClient

  protected def createItem(itemDetails: D, listingDetails: ListingDetails, resellPrice: Option[ResellPrice]): I

  def getLatestFromEbay(minutes: Int): IOErrorOr[Seq[I]] =
    ebaySearchClient.getItemsListedInLastMinutes(minutes).flatMap { itemDetails =>
      itemDetails.map {
        case (id, ld) => cexClient.findResellPrice(id).map(createItem(id, ld, _))
      }.toList.sequence
    }.map(_.toSeq)

  def sendNotification(item: I): IOErrorOr[Unit] =
    telegramClient.sendMessageToMainChannel(item)

  def save(item: I): IOErrorOr[Unit] =
    itemRepository.save(item)

  def getLatest(limit: Int): IOErrorOr[Seq[I]] =
    itemRepository.findAll(limit)

  def isNew(item: I): IOErrorOr[Boolean] =
    itemRepository.existsByUrl(item.listingDetails.url).map(!_)
}
