package clients.ebay.mappers

import java.time.Instant

import clients.ebay.browse.EbayBrowseResponse.EbayItem
import domain.{ItemDetails, ListingDetails, Price, ResellableItem}

private[ebay] trait EbayItemMapper[D <: ItemDetails] {
  def toDomain(ebayItem: EbayItem): ResellableItem[D]
}


private[ebay] object EbayItemMapper {
  implicit val phoneDetailsMapper = new EbayItemMapper[ItemDetails.Phone] {
    override def toDomain(ebayItem: EbayItem): ResellableItem[ItemDetails.Phone] = {
      val listing = toListingDetails(ebayItem)
      ResellableItem(PhoneDetailsMapper.from(listing), listing, toPurchasePrice(ebayItem), None)
    }
  }

  implicit val gameDetailsMapper = new EbayItemMapper[ItemDetails.Game] {
    override def toDomain(ebayItem: EbayItem): ResellableItem[ItemDetails.Game] = {
      val listing = toListingDetails(ebayItem)
      ResellableItem(GameDetailsMapper.from(listing), listing, toPurchasePrice(ebayItem), None)
    }
  }

  private[mappers] def toListingDetails(item: EbayItem): ListingDetails =
    ListingDetails(
      url = item.itemWebUrl,
      title = item.title,
      shortDescription = item.shortDescription,
      description = item.description.map(_.replaceAll("(?i)<[^>]*>", "")).map(_.slice(0, 500)),
      image = item.image.map(_.imageUrl),
      condition = item.condition.toUpperCase,
      datePosted = Instant.now,
      seller = item.seller.username.fold("EBAY")(s => s"EBAY:$s"),
      properties = item.localizedAspects.getOrElse(List()).map(prop => prop.name -> prop.value).toMap
    )

  private[mappers] def toPurchasePrice(item: EbayItem): Price = {
    val postageCost = for {
      shippings <- item.shippingOptions
      minShippingCost <- shippings.map(_.shippingCost).map(_.value).minOption
    } yield minShippingCost
    Price(1, item.price.value + postageCost.getOrElse(BigDecimal.valueOf(0)))
  }
}
