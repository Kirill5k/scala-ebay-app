package repositories

import domain.ItemDetails.GameDetails
import domain.{ItemDetails, ResellableItem}
import domain.ResellableItem.VideoGame
import repositories.ResellableItemEntity.VideoGameEntity

private[repositories] trait ResellableItemEntityMapper[D <: ItemDetails] {
  def toEntity(item: ResellableItem[D]): ResellableItemEntity[D]
  def toDomain(entity: ResellableItemEntity[D]): ResellableItem[D]
}

private[repositories] object ResellableItemEntityMapper {
  val videoGameEntityMapper = new ResellableItemEntityMapper[GameDetails] {
    override def toEntity(videoGame: VideoGame): VideoGameEntity =
      ResellableItemEntity[GameDetails](None, videoGame.itemDetails, videoGame.listingDetails, videoGame.resellPrice)

    override def toDomain(entity: VideoGameEntity): VideoGame =
      ResellableItem[GameDetails](entity.itemDetails, entity.listingDetails, entity.resellPrice)
  }
}
