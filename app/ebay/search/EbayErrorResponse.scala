package ebay.search

import exceptions.ApiClientError
import io.circe.generic.auto._
import io.circe.parser._
import play.api.libs.ws.BodyReadable


final case class EbayError(errorId: Long, domain: String, category: String, message: String)

final case class EbayErrorResponse(errors: Seq[EbayError])

object EbayErrorResponse {
  implicit val ebayAuthSuccessResponseBodyReadable = BodyReadable[Either[ApiClientError, EbayErrorResponse]] { response =>
    import play.shaded.ahc.org.asynchttpclient.{ Response => AHCResponse }
    val responseString = response.underlying[AHCResponse].getResponseBody
    decode[EbayErrorResponse](responseString).left.map(ApiClientError.jsonParsingError)
  }
}
