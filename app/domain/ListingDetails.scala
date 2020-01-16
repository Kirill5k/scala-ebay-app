package domain

import java.time.Instant

import domain.mappers.{GameDetailsMapper, PhoneDetailsMapper}

final case class ListingDetails(
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
  def as[T <: ItemDetails](implicit f: ListingDetails => T): T = f(this)
}

object ListingDetails {
  import ItemDetails._
  implicit def phoneDetailsMapper: ListingDetails => PhoneDetails = PhoneDetailsMapper.from
  implicit def gameDetailsMapper: ListingDetails => GameDetails = GameDetailsMapper.from
}
