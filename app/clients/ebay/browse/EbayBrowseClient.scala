package clients.ebay.browse

import java.time.Instant

import cats.data.EitherT
import cats.implicits._
import clients.ebay.EbayConfig
import clients.ebay.browse.EbayBrowseResponse._
import domain.ApiClientError._
import domain.{ApiClientError, ListingDetails}
import javax.inject.Inject
import play.api.Configuration
import play.api.http.{HeaderNames, Status}
import play.api.libs.ws.{WSClient, WSRequest}

import scala.concurrent.ExecutionContext

class EbayBrowseClient @Inject()(config: Configuration, client: WSClient)(implicit ex: ExecutionContext) {
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
        if (Status.isSuccessful(res.status)) res.body[Either[ApiClientError, EbayBrowseResult]]
        else res.body[Either[ApiClientError, EbayErrorResponse]].flatMap(toApiClientError(res.status))
      }
      .recover(ApiClientError.recoverFromHttpCallFailure.andThen(_.asLeft))
    EitherT(searchResponse).map(_.itemSummaries.getOrElse(Seq()))
  }

  def getItem(accessToken: String, itemId: String): FutureErrorOr[ListingDetails] = {
    val getItemResponse = request(s"${ebayConfig.baseUri}${ebayConfig.itemPath}/$itemId", accessToken)
      .get()
      .map { res =>
        if (Status.isSuccessful(res.status)) res.body[Either[ApiClientError, EbayItem]]
        else res.body[Either[ApiClientError, EbayErrorResponse]].flatMap(toApiClientError(res.status))
      }
      .recover(ApiClientError.recoverFromHttpCallFailure.andThen(_.asLeft))
    EitherT(getItemResponse).map(toListingDetails)
  }

  private def request(url: String, accessToken: String): WSRequest =
    client.url(url).addHttpHeaders(defaultHeaders: _*).addHttpHeaders(HeaderNames.AUTHORIZATION -> s"Bearer $accessToken")

  private def toApiClientError[A](status: Int)(ebayErrorResponse: EbayErrorResponse): Either[ApiClientError, A] = status match {
    case 429 | 403 | 401 => AuthError(s"ebay account has expired: $status").asLeft[A]
    case _ => ebayErrorResponse.errors.headOption
        .fold(status.toString)(_.message)
        .asLeft[A]
        .leftMap(e => HttpError(status, s"error sending request to ebay search api: $e"))
  }

  private def toListingDetails(item: EbayItem): ListingDetails =
    ListingDetails(
      url = item.itemWebUrl,
      title = item.title,
      shortDescription = item.shortDescription,
      description = item.description.map(_.replaceAll("(?i)<[^>]*>", "")).map(_.substring(0, 500)),
      image = item.image.imageUrl,
      buyingOptions = item.buyingOptions,
      sellerName = item.seller.username,
      price = item.price.value,
      condition = item.condition,
      datePosted = Instant.now,
      dateEnded = item.itemEndDate,
      properties = item.localizedAspects.map(prop => prop.name -> prop.value).toMap
    )
}
