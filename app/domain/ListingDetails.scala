package domain

import java.time.Instant

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
                               )
