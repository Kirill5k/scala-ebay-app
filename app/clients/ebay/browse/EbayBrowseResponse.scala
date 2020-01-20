package clients.ebay.browse

import scala.math.BigDecimal
import java.time.Instant

import domain.ApiClientError
import domain.ApiClientError._
import io.circe.generic.auto._
import io.circe.parser._
import play.api.libs.ws.BodyReadable

sealed trait EbayBrowseResponse

private[ebay] object EbayBrowseResponse {
  final case class ItemProperty(name: String, value: String)
  final case class ItemSeller(username: String, feedbackPercentage: Option[Double], feedbackScore: Option[Int])
  final case class ItemImage(imageUrl: String)
  final case class ItemPrice(value: BigDecimal, currency: String)
  final case class EbayItemSummary(itemId: String, title: String, price: ItemPrice, seller: ItemSeller)

  final case class EbayItem(
                             itemId: String,
                             title: String,
                             shortDescription: Option[String],
                             description: Option[String],
                             categoryPath: String,
                             price: ItemPrice,
                             condition: String,
                             image: ItemImage,
                             seller: ItemSeller,
                             localizedAspects: Seq[ItemProperty],
                             buyingOptions: Seq[String],
                             itemWebUrl: String,
                             color: Option[String],
                             brand: Option[String],
                             mpn: Option[String],
                             itemEndDate: Option[Instant]
                           ) extends EbayBrowseResponse

  final case class EbayBrowseResult(total: Int, limit: Int, itemSummaries: Option[Seq[EbayItemSummary]]) extends EbayBrowseResponse

  final case class EbayError(errorId: Long, domain: String, category: String, message: String)
  final case class EbayErrorResponse(errors: Seq[EbayError]) extends EbayBrowseResponse

  implicit val ebayItemBodyReadable = BodyReadable[Either[ApiClientError, EbayItem]] { response =>
    import play.shaded.ahc.org.asynchttpclient.{Response => AHCResponse}
    val responseString = response.underlying[AHCResponse].getResponseBody
    decode[EbayItem](responseString).left.map(e => JsonParsingError(e.getMessage))
  }

  implicit val ebayErrorResponseBodyReadable = BodyReadable[Either[ApiClientError, EbayErrorResponse]] { response =>
    import play.shaded.ahc.org.asynchttpclient.{Response => AHCResponse}
    val responseString = response.underlying[AHCResponse].getResponseBody
    decode[EbayErrorResponse](responseString).left.map(e => JsonParsingError(e.getMessage))
  }

  implicit val ebaySearchResultBodyReadable = BodyReadable[Either[ApiClientError, EbayBrowseResult]] { response =>
    import play.shaded.ahc.org.asynchttpclient.{Response => AHCResponse}
    val responseString = response.underlying[AHCResponse].getResponseBody
    decode[EbayBrowseResult](responseString).left.map(e => JsonParsingError(e.getMessage))
  }
}