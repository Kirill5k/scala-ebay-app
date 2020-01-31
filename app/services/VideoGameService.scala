package services

import clients.cex.CexClient
import clients.ebay.VideoGameEbayClient
import clients.telegram.TelegramClient
import domain.ItemDetails.GameDetails
import domain.{ListingDetails, ResellPrice}
import domain.ResellableItem.VideoGame
import javax.inject.Inject
import repositories.ResellableItemEntity.VideoGameEntity
import repositories.VideoGameRepository

import scala.concurrent.ExecutionContext

class VideoGameService @Inject()(
                                  override val itemRepository: VideoGameRepository,
                                  override val ebaySearchClient: VideoGameEbayClient,
                                  override val telegramClient: TelegramClient,
                                  override val cexClient: CexClient
                                )(implicit override val ex: ExecutionContext)
  extends ResellableItemService[VideoGame, GameDetails, VideoGameEntity] {

  override protected def createItem(itemDetails: GameDetails, listingDetails: ListingDetails, resellPrice: Option[ResellPrice]): VideoGame =
    VideoGame.apply(itemDetails, listingDetails, resellPrice)
}
