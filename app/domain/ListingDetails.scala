package domain


import java.time.Instant

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
