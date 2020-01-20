package domain

import java.net.URI
import java.time.Instant

final case class ListingDetails(
                                 url: URI,
                                 title: String,
                                 shortDescription: Option[String],
                                 description: Option[String],
                                 image: URI,
                                 buyingOptions: Seq[String],
                                 sellerName: String,
                                 price: BigDecimal,
                                 condition: String,
                                 datePosted: Instant,
                                 dateEnded: Option[Instant],
                                 properties: Map[String, String]
                               )
