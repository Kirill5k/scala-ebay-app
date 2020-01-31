package domain

import cats.implicits._

sealed trait ResellableItem {
  def itemDetails: ItemDetails
  def listingDetails: ListingDetails
  def resellPrice: Option[ResellPrice]

  def searchQuery: Option[String]
}

object ResellableItem {
  import ItemDetails._
  final case class VideoGame(itemDetails: GameDetails, listingDetails: ListingDetails, resellPrice: Option[ResellPrice]) extends ResellableItem {
    override def searchQuery: Option[String] =
      itemDetails.name.flatMap(n => itemDetails.platform.map(p => s"$n $p"))
  }
  final case class MobilePhone(itemDetails: PhoneDetails, listingDetails: ListingDetails, resellPrice: Option[ResellPrice]) extends ResellableItem {
    override def searchQuery: Option[String] =
      List(itemDetails.make, itemDetails.model, itemDetails.storageCapacity, itemDetails.colour, itemDetails.network).sequence.map(_.mkString(" "))
  }
}


