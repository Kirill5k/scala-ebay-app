package clients.ebay.mappers

import domain.{ItemDetails, ListingDetails}

private[ebay] object ListingDetailsMapper {
  import domain.ItemDetails._
  implicit def phoneDetailsMapper: ListingDetails => PhoneDetails = PhoneDetailsMapper.from
  implicit def gameDetailsMapper: ListingDetails => GameDetails = GameDetailsMapper.from

  implicit class ListingDetailsOps(listingDetails: ListingDetails) {
    def as[T <: ItemDetails](implicit f: ListingDetails => T): T = f(listingDetails)
  }
}
