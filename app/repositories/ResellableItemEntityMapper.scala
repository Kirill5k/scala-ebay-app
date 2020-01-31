package repositories

import domain.ResellableItem
import domain.ResellableItem.VideoGame
import repositories.ResellableItemEntity.VideoGameEntity

trait ResellableItemEntityMapper[A <: ResellableItem, B <: ResellableItemEntity] {
  def toEntity(item: A): B
  def toDomain(entity: B): A
}

object ResellableItemEntityMapper {
  val videoGameEntityMapper = new ResellableItemEntityMapper[VideoGame, VideoGameEntity] {
    override def toEntity(videoGame: VideoGame): VideoGameEntity =
      VideoGameEntity(None, videoGame.itemDetails, videoGame.listingDetails, videoGame.resellPrice)

    override def toDomain(entity: VideoGameEntity): VideoGame =
      VideoGame(entity.itemDetails, entity.listingDetails, entity.resellPrice)
  }
}
