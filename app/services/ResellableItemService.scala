package services

import java.time.Instant

import cats.effect.IO
import domain.{ItemDetails, ResellableItem}
import javax.inject.Inject
import repositories.{ResellableItemRepository, VideoGameRepository}

trait ResellableItemService[D <: ItemDetails] {
  protected def itemRepository: ResellableItemRepository[D]

  def save(item: ResellableItem[D]): IO[Unit] =
    itemRepository.save(item)

  def get(limit: Option[Int], from: Option[Instant], to: Option[Instant]): IO[List[ResellableItem[D]]] =
    itemRepository.findAll(limit, from, to)

  def isNew(item: ResellableItem[D]): IO[Boolean] =
    itemRepository.existsByUrl(item.listingDetails.url).map(!_)
}

class VideoGameService @Inject()(
    override val itemRepository: VideoGameRepository
) extends ResellableItemService[ItemDetails.Game] {}
