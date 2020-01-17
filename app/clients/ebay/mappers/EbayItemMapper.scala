package clients.ebay.mappers

import java.time.Instant

import clients.ebay.browse.EbayBrowseResponse.EbayItem
import domain.{ItemDetails, ListingDetails}

private[ebay] object EbayItemMapper {
  import domain.ItemDetails._
  implicit def phoneDetailsMapper: ListingDetails => PhoneDetails = PhoneDetailsMapper.from
  implicit def gameDetailsMapper: ListingDetails => GameDetails = GameDetailsMapper.from

  implicit class EbayItemSyntax(val ebayItem: EbayItem) extends AnyVal {
    def as[T <: ItemDetails](implicit f: ListingDetails => T): (T, ListingDetails) = {
      val listing = toListingDetails(ebayItem)
      (f(listing), listing)
    }

    private def toListingDetails(item: EbayItem): ListingDetails =
      ListingDetails(
        url = item.itemWebUrl,
        title = item.title,
        shortDescription = item.shortDescription,
        description = item.description.map(_.replaceAll("(?i)<[^>]*>", "")).map(_.slice(0, 500)),
        image = item.image.imageUrl,
        buyingOptions = item.buyingOptions,
        sellerName = item.seller.username,
        price = item.price.value,
        condition = item.condition,
        datePosted = Instant.now,
        dateEnded = item.itemEndDate,
        properties = item.localizedAspects.map(prop => prop.name -> prop.value).toMap
      )
  }
}
