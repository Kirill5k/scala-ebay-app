package clients.ebay.auth

import domain.ApiClientError
import domain.ApiClientError._
import io.circe.generic.auto._
import io.circe.parser._
import play.api.libs.ws.BodyReadable

sealed trait EbayAuthResponse

private[auth] object EbayAuthResponse {
  final case class EbayAuthSuccessResponse(access_token: String, expires_in: Long, token_type: String) extends EbayAuthResponse
  final case class EbayAuthErrorResponse(error: String, error_description: String) extends EbayAuthResponse

  implicit val ebayAuthSuccessResponseBodyReadable = BodyReadable[Either[ApiClientError, EbayAuthSuccessResponse]] { response =>
    import play.shaded.ahc.org.asynchttpclient.{ Response => AHCResponse }
    val responseString = response.underlying[AHCResponse].getResponseBody
    decode[EbayAuthSuccessResponse](responseString).left.map(e => JsonParsingError(e.getMessage))
  }

  implicit val ebayAuthErrorResponseBodyReadable = BodyReadable[Either[ApiClientError, EbayAuthErrorResponse]] { response =>
    import play.shaded.ahc.org.asynchttpclient.{ Response => AHCResponse }
    val responseString = response.underlying[AHCResponse].getResponseBody
    decode[EbayAuthErrorResponse](responseString).left.map(e => JsonParsingError(e.getMessage))
  }
}
