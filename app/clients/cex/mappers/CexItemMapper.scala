package clients.cex.mappers

import java.time.Instant
import java.time.temporal.TemporalAdjuster

import clients.cex.CexClient.SearchResult
import domain.StockUpdateType.PriceDrop
import domain.{ItemDetails, ListingDetails, Price, ResellPrice, ResellableItem}

trait CexItemMapper[D <: ItemDetails] {
  def toDomain(sr: SearchResult): ResellableItem[D]
}

object CexItemMapper {

  implicit val genericItemMapper = new CexItemMapper[ItemDetails.Generic] {
    override def toDomain(sr: SearchResult): ResellableItem[ItemDetails.Generic] =
      ResellableItem[ItemDetails.Generic](
        ItemDetails.Generic(sr.boxName),
        listingDetails(sr),
        price(sr),
        Some(resellPrice(sr))
      )
  }

  def price(sr: SearchResult): Price =
    Price(sr.ecomQuantityOnHand, sr.sellPrice)

  def resellPrice(sr: SearchResult): ResellPrice =
    ResellPrice(sr.cashPrice, sr.exchangePrice)

  def listingDetails(sr: SearchResult): ListingDetails =
    ListingDetails(
      s"https://uk.webuy.com/product-detail/?id=${sr.boxId}",
      sr.boxName,
      Some(sr.categoryName),
      None,
      None,
      s"USED / ${sr.boxName.last}",
      Instant.now,
      "CEX",
      Map()
    )
}
