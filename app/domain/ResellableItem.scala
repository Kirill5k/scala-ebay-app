package domain

import java.time.Instant

import domain.mappers.GameDetailsMapper

sealed trait ItemDetails

object ItemDetails {
  final case class PhoneDetails(
                                 make: Option[String],
                                 model: Option[String],
                                 colour: Option[String],
                                 storageCapacity: Option[String],
                                 network: Option[String],
                                 condition: Option[String],
                                 mpn: Option[String]
                               ) extends ItemDetails
  final case class GameDetails(
                                name: Option[String],
                                platform: Option[String],
                                releaseYear: Option[String],
                                genre: Option[String]
                              ) extends ItemDetails
}

case class ListingDetails(
                           url: String,
                           title: String,
                           shortDescription: Option[String],
                           description: Option[String],
                           image: String,
                           buyingOptions: Seq[String],
                           sellerName: String,
                           price: BigDecimal,
                           condition: String,
                           datePosted: Instant,
                           dateEnded: Option[Instant],
                           properties: Map[String, String]
                         ) {
  def as[T >: ItemDetails](implicit f: ListingDetails => T): T = f(this)
}

object ListingDetails {
  import ItemDetails._
  implicit def phoneDetailsMapper: ListingDetails => PhoneDetails = ???
  implicit def gameDetailsMapper: ListingDetails => GameDetails = GameDetailsMapper.from
}

case class ResellPrice(cash: BigDecimal, exchange: BigDecimal)

sealed trait ResellableItem {
  def itemDetails: ItemDetails
  def listingDetails: ListingDetails
  def resellPrice: Option[ResellPrice]
}

object ResellableItem {
  import ItemDetails._
  final case class VideoGame(itemDetails: GameDetails, listingDetails: ListingDetails, resellPrice: Option[ResellPrice]) extends ResellableItem
  final case class MobilePhone(itemDetails: PhoneDetails, listingDetails: ListingDetails, resellPrice: Option[ResellPrice]) extends ResellableItem
}


