package clients.ebay.browse

import scala.math.BigDecimal
import java.time.Instant

sealed trait EbayBrowseResponse

private[ebay] object EbayBrowseResponse {
  final case class ItemProperty(name: String, value: String)
  final case class ItemSeller(username: Option[String], feedbackPercentage: Option[Double], feedbackScore: Option[Int])
  final case class ItemImage(imageUrl: String)
  final case class ItemPrice(value: BigDecimal, currency: String)
  final case class ShippingCost(value: BigDecimal, currency: String)
  final case class ItemShippingOption(shippingServiceCode: String, shippingCost: ShippingCost)

  final case class EbayItemSummary(
      itemId: String,
      title: String,
      price: Option[ItemPrice],
      seller: ItemSeller
  )

  final case class EbayItem(
      itemId: String,
      title: String,
      shortDescription: Option[String],
      description: Option[String],
      categoryPath: String,
      price: ItemPrice,
      condition: String,
      image: Option[ItemImage],
      seller: ItemSeller,
      localizedAspects: Option[Seq[ItemProperty]],
      buyingOptions: Seq[String],
      itemWebUrl: String,
      color: Option[String],
      brand: Option[String],
      mpn: Option[String],
      itemEndDate: Option[Instant],
      shippingOptions: Option[Seq[ItemShippingOption]]
  ) extends EbayBrowseResponse

  final case class EbayBrowseResult(total: Int, limit: Int, itemSummaries: Option[Seq[EbayItemSummary]]) extends EbayBrowseResponse

  final case class EbayError(errorId: Long, domain: String, category: String, message: String)
  final case class EbayErrorResponse(errors: Seq[EbayError]) extends EbayBrowseResponse

}
