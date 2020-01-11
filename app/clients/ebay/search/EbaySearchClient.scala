package clients.ebay.search

import cats.data.EitherT
import cats.implicits._
import clients.ebay.EbayConfig
import exceptions.ApiClientError
import exceptions.ApiClientError._
import javax.inject.Inject
import play.api.http.{HeaderNames, Status}
import play.api.libs.ws.WSClient
import play.api.{Configuration}

import scala.concurrent.ExecutionContext

class EbaySearchClient @Inject()(config: Configuration, client: WSClient)(implicit ex: ExecutionContext) {
  private val ebayConfig = config.get[EbayConfig]("ebay")

  private val defaultHeaders = Map(
    HeaderNames.CONTENT_TYPE -> "application/json",
    HeaderNames.ACCEPT -> "application/json",
    "X-EBAY-C-MARKETPLACE-ID" -> "EBAY_GB"
  ).toList

  def getItem(accessToken: String, itemId: String): FutureErrorOr[EbayItem] = {
    val getItemResponse = client
      .url(s"${ebayConfig.baseUri}${ebayConfig.itemPath}/$itemId")
      .addHttpHeaders(defaultHeaders: _*)
      .addHttpHeaders(HeaderNames.AUTHORIZATION -> s"Bearer $accessToken")
      .get()
      .map { res =>
        if (Status.isSuccessful(res.status)) res.body[Either[ApiClientError, EbayItem]]
        else res.body[Either[ApiClientError, EbayErrorResponse]].flatMap(toApiClientError(res.status, _))
      }
      .recover(ApiClientError.recoverFromHttpCallFailure.andThen(_.asLeft))
    EitherT(getItemResponse)
  }

  private def toApiClientError[A](status: Int, ebayErrorResponse: EbayErrorResponse): Either[ApiClientError, A] = status match {
    case 429 | 403 | 401 => AuthError(s"ebay account has expired: $status").asLeft[A]
    case _ => ebayErrorResponse.errors.headOption
        .map(e => HttpError(status, s"error sending request to ebay search api: ${e.message}"))
        .getOrElse(HttpError(status, s"error sending request to ebay search api: $status"))
        .asLeft[A]
  }
}
