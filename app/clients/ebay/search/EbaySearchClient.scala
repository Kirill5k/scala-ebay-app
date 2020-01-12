package clients.ebay.search

import cats.data.EitherT
import cats.implicits._
import clients.ebay.EbayConfig
import javax.inject.Inject
import play.api.http.{HeaderNames, Status}
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.Configuration

import scala.concurrent.ExecutionContext
import EbaySearchResponse._
import domain.ApiClientError
import domain.ApiClientError._

class EbaySearchClient @Inject()(config: Configuration, client: WSClient)(implicit ex: ExecutionContext) {
  private val ebayConfig = config.get[EbayConfig]("ebay")

  private val defaultHeaders = Map(
    HeaderNames.CONTENT_TYPE -> "application/json",
    HeaderNames.ACCEPT -> "application/json",
    "X-EBAY-C-MARKETPLACE-ID" -> "EBAY_GB"
  ).toList

  def search(accessToken: String, queryParams: Map[String, String]): FutureErrorOr[Seq[EbayItemSummary]] = {
    val searchResponse = request(s"${ebayConfig.baseUri}${ebayConfig.searchPath}", accessToken)
      .withQueryStringParameters(queryParams.toList: _*)
      .get()
      .map { res =>
        if (Status.isSuccessful(res.status)) res.body[Either[ApiClientError, EbaySearchResult]]
        else res.body[Either[ApiClientError, EbayErrorResponse]].flatMap(toApiClientError(res.status))
      }
      .recover(ApiClientError.recoverFromHttpCallFailure.andThen(_.asLeft))
    EitherT(searchResponse).map(_.itemSummaries.getOrElse(Seq()))
  }

  def getItem(accessToken: String, itemId: String): FutureErrorOr[EbayItem] = {
    val getItemResponse = request(s"${ebayConfig.baseUri}${ebayConfig.itemPath}/$itemId", accessToken)
      .get()
      .map { res =>
        if (Status.isSuccessful(res.status)) res.body[Either[ApiClientError, EbayItem]]
        else res.body[Either[ApiClientError, EbayErrorResponse]].flatMap(toApiClientError(res.status))
      }
      .recover(ApiClientError.recoverFromHttpCallFailure.andThen(_.asLeft))
    EitherT(getItemResponse)
  }

  private def request(url: String, accessToken: String): WSRequest =
    client.url(url).addHttpHeaders(defaultHeaders: _*).addHttpHeaders(HeaderNames.AUTHORIZATION -> s"Bearer $accessToken")

  private def toApiClientError[A](status: Int)(ebayErrorResponse: EbayErrorResponse): Either[ApiClientError, A] = status match {
    case 429 | 403 | 401 => AuthError(s"ebay account has expired: $status").asLeft[A]
    case _ => ebayErrorResponse.errors.headOption
        .map(e => HttpError(status, s"error sending request to ebay search api: ${e.message}"))
        .getOrElse(HttpError(status, s"error sending request to ebay search api: $status"))
        .asLeft[A]
  }
}
