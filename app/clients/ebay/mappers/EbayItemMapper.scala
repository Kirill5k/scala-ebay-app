package clients.ebay.mappers

import java.net.URI
import java.time.Instant

import clients.ebay.browse.EbayBrowseResponse.EbayItem
import domain.{ItemDetails, ListingDetails}


private[ebay] trait EbayItemMapper[T <: ItemDetails] {
  def toDomain(ebayItem: EbayItem): (T, ListingDetails)
}


private[ebay] object EbayItemMapper {
  import domain.ItemDetails._
  implicit val phoneDetailsMapper = new EbayItemMapper[PhoneDetails] {
    override def toDomain(ebayItem: EbayItem): (PhoneDetails, ListingDetails) = {
      val listing = toListingDetails(ebayItem)
      (PhoneDetailsMapper.from(listing), listing)
    }
  }

  implicit val gameDetailsMapper = new EbayItemMapper[GameDetails] {
    override def toDomain(ebayItem: EbayItem): (GameDetails, ListingDetails) = {
      val listing = toListingDetails(ebayItem)
      (GameDetailsMapper.from(listing), listing)
    }
  }

  implicit class EbayItemMapperOps[T](val ebayItem: EbayItem) extends AnyVal {
    def as[T <: ItemDetails](implicit m: EbayItemMapper[T]): (T, ListingDetails) = m.toDomain(ebayItem)
  }

  private[mappers] def toListingDetails(item: EbayItem): ListingDetails =
    ListingDetails(
      url = new URI(item.itemWebUrl),
      title = item.title,
      shortDescription = item.shortDescription,
      description = item.description.map(_.replaceAll("(?i)<[^>]*>", "")).map(_.slice(0, 500)),
      image = new URI(item.image.imageUrl),
      buyingOptions = item.buyingOptions,
      sellerName = item.seller.username,
      price = item.price.value,
      condition = item.condition,
      datePosted = Instant.now,
      dateEnded = item.itemEndDate,
      properties = item.localizedAspects.map(prop => prop.name -> prop.value).toMap
    )
}
