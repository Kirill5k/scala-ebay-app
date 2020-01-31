package services

import clients.cex.CexClient
import clients.ebay.EbaySearchClient
import clients.telegram.TelegramClient
import domain.{ItemDetails, ResellableItem}
import repositories.{ResellableItemEntity, ResellableItemRepository}

trait ResellableItemService[I <: ResellableItem, E <: ResellableItemEntity, D <: ItemDetails] {
  protected def itemRepository: ResellableItemRepository[I, E]
  protected def ebaySearchClient: EbaySearchClient[D]
  protected def telegramClient: TelegramClient
  protected def cexClient: CexClient
}
