package ebay.auth

import exceptions.ApiClientError
import io.circe.generic.auto._
import io.circe.parser._
import play.api.libs.ws.BodyReadable

sealed trait EbayAuthResponse

private[auth] final case class EbayAuthSuccessResponse(access_token: String, expires_in: Long, token_type: String) extends EbayAuthResponse

private[auth] final case class EbayAuthErrorResponse(error: String, error_description: String) extends EbayAuthResponse

private[auth] object EbayAuthResponse {
  implicit val ebayAuthSuccessResponseBodyReadable = BodyReadable[Either[ApiClientError, EbayAuthSuccessResponse]] { response =>
    import play.shaded.ahc.org.asynchttpclient.{ Response => AHCResponse }
    val responseString = response.underlying[AHCResponse].getResponseBody
    decode[EbayAuthSuccessResponse](responseString).left.map(ApiClientError.jsonParsingError)
  }

  implicit val ebayAuthErrorResponseBodyReadable = BodyReadable[Either[ApiClientError, EbayAuthErrorResponse]] { response =>
    import play.shaded.ahc.org.asynchttpclient.{ Response => AHCResponse }
    val responseString = response.underlying[AHCResponse].getResponseBody
    decode[EbayAuthErrorResponse](responseString).left.map(ApiClientError.jsonParsingError)
  }
}
