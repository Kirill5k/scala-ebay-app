package services

import clients.cex.CexClient
import clients.ebay.EbaySearchClient
import clients.telegram.TelegramClient
import domain.ApiClientError.FutureErrorOr
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

  def getLatestFromEbay(minutes: Int): FutureErrorOr[Seq[I]]

  def sendNotification(item: I): FutureErrorOr[Unit]

  def save(item: I): FutureErrorOr[Unit]

  def getLatest(limit: Int): FutureErrorOr[Seq[I]]

  def isNew(item: I): FutureErrorOr[Boolean]
}
