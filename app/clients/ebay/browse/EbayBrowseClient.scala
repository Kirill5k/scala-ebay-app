package clients.ebay.browse

import cats.data.EitherT
import cats.implicits._
import clients.ebay.EbayConfig
import clients.ebay.browse.EbayBrowseResponse._
import domain.ApiClientError._
import domain.ApiClientError
import javax.inject._
import play.api.Configuration
import play.api.http.{HeaderNames, Status}
import play.api.libs.ws.{WSClient, WSRequest}

import scala.concurrent.ExecutionContext

@Singleton
private[ebay] class EbayBrowseClient @Inject()(config: Configuration, client: WSClient)(implicit ex: ExecutionContext) {
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
        res.status match {
          case status if Status.isSuccessful(status) => res.body[Either[ApiClientError, EbayBrowseResult]].map(_.itemSummaries.getOrElse(Seq()))
          case status => res.body[Either[ApiClientError, EbayErrorResponse]].flatMap(toApiClientError(status))
        }
      }
      .recover(ApiClientError.recoverFromHttpCallFailure.andThen(_.asLeft))
    EitherT(searchResponse)
  }

  def getItem(accessToken: String, itemId: String): FutureErrorOr[Option[EbayItem]] = {
    val getItemResponse = request(s"${ebayConfig.baseUri}${ebayConfig.itemPath}/$itemId", accessToken)
      .get()
      .map { res =>
        res.status match {
          case status if Status.isSuccessful(status) => res.body[Either[ApiClientError, EbayItem]].map(_.some)
          case Status.NOT_FOUND => none[EbayItem].asRight[ApiClientError]
          case status => res.body[Either[ApiClientError, EbayErrorResponse]].flatMap(toApiClientError(status))
        }
      }
      .recover(ApiClientError.recoverFromHttpCallFailure.andThen(_.asLeft))
    EitherT(getItemResponse)
  }

  private def request(url: String, accessToken: String): WSRequest =
    client.url(url).addHttpHeaders(defaultHeaders: _*).addHttpHeaders(HeaderNames.AUTHORIZATION -> s"Bearer $accessToken")

  private def toApiClientError[A](status: Int)(ebayErrorResponse: EbayErrorResponse): Either[ApiClientError, A] = status match {
    case Status.TOO_MANY_REQUESTS | Status.FORBIDDEN | Status.UNAUTHORIZED => AuthError(s"ebay account has expired: $status").asLeft[A]
    case _ => ebayErrorResponse.errors.headOption
      .fold(status.toString)(_.message)
      .asLeft[A]
      .leftMap(e => HttpError(status, s"error sending request to ebay search api: $e"))
  }
}
