import java.time.Instant

package object domain {

  final case class SearchQuery(value: String) extends AnyVal

  final case class ListingDetails(
      url: String,
      title: String,
      shortDescription: Option[String],
      description: Option[String],
      image: Option[String],
      buyingOptions: Seq[String],
      sellerName: Option[String],
      price: BigDecimal,
      condition: String,
      datePosted: Instant,
      dateEnded: Option[Instant],
      properties: Map[String, String]
  )

  final case class ResellPrice(
      cash: BigDecimal,
      exchange: BigDecimal
  )

  final case class PurchasePrice(
      quantityAvailable: Int,
      pricePerUnit: BigDecimal
  )
}
