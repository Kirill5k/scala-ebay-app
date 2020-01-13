package domain

import java.time.Instant


case class ResellPrice(cash: BigDecimal, exchange: BigDecimal)

case class ListingDetails(
                           url: String,
                           title: String,
                           description: String,
                           image: String,
                           listingType: String,
                           sellerName: String,
                           price: BigDecimal,
                           condition: String,
                           datePosted: Instant,
                           dateEnded: Option[Instant],
                         )
