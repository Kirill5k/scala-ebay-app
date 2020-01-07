package clients.ebay

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}

sealed trait EbayAuthResponse

final case class EbayAuthSuccessResponse(accessToken: String, expiresIn: Long, tokenType: String) extends EbayAuthResponse

final case class EbayAuthErrorResponse(error: String, description: String) extends EbayAuthResponse

object EbayAuthResponse {
  implicit val ebayAuthSuccessResponseReads: Reads[EbayAuthSuccessResponse] = (
    (JsPath \ "access_token").read[String] and
      (JsPath \ "expires_in").read[Long] and
      (JsPath \ "token_type").read[String]
    )(EbayAuthSuccessResponse.apply _)

  implicit val ebayAuthErrorResponseReads: Reads[EbayAuthErrorResponse] = (
    (JsPath \ "error").read[String] and
      (JsPath \ "error_description").read[String]
    )(EbayAuthErrorResponse.apply _)
}
