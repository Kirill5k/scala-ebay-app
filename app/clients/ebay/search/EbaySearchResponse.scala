package clients.ebay.search

import java.time.Instant

import exceptions.ApiClientError
import exceptions.ApiClientError.JsonParsingError
import io.circe.generic.auto._
import io.circe.parser._
import play.api.libs.ws.BodyReadable

sealed trait EbaySearchResponse

private[ebay] object EbaySearchResponse {
  final case class ItemProperty(name: String, value: String)
  final case class ItemSeller(username: String, feedbackPercentage: String, feedbackScore: Int)
  final case class ItemImage(imageUrl: String)
  final case class ItemPrice(value: String, currency: String)
  final case class ItemSummary(itemId: String, title: String, price: ItemPrice, seller: ItemSeller)

  final case class EbayItem(
                             itemId: String,
                             title: String,
                             shortDescription: String,
                             description: String,
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
                           ) extends EbaySearchResponse

  final case class EbaySearchResult(total: Int, limit: Int, itemSummaries: Seq[ItemSummary]) extends EbaySearchResponse
  final case class EbayError(errorId: Long, domain: String, category: String, message: String)
  final case class EbayErrorResponse(errors: Seq[EbayError]) extends EbaySearchResponse

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

  implicit val ebaySearchResponseBodyReadable = BodyReadable[Either[ApiClientError, EbaySearchResponse]] { response =>
    import play.shaded.ahc.org.asynchttpclient.{Response => AHCResponse}
    val responseString = response.underlying[AHCResponse].getResponseBody
    decode[EbaySearchResponse](responseString).left.map(e => JsonParsingError(e.getMessage))
  }
}
