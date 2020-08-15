import java.time.Instant

package object domain {

  final case class SearchQuery(value: String) extends AnyVal

  final case class ListingDetails(
      url: String,
      title: String,
      shortDescription: Option[String],
      description: Option[String],
      image: Option[String],
      condition: String,
      datePosted: Instant,
      seller: String,
      properties: Map[String, String]
  )

  final case class ResellPrice(
      cash: BigDecimal,
      exchange: BigDecimal
  )

  final case class Price(
      quantityAvailable: Int,
      value: BigDecimal
  )
}
