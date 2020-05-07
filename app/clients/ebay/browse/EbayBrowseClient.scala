package clients.ebay.browse

import cats.effect.IO
import cats.implicits._
import clients.ebay.browse.EbayBrowseResponse._
import common.config.AppConfig
import common.resources.SttpBackendResource
import domain.ApiClientError
import domain.ApiClientError._
import io.circe.generic.auto._
import javax.inject._
import play.api.Logger
import sttp.client._
import sttp.client.circe._
import sttp.model.{HeaderNames, MediaType, StatusCode}

@Singleton
private[ebay] class EbayBrowseClient @Inject()(catsSttpBackendResource: SttpBackendResource[IO]) {
  private val log: Logger = Logger(getClass)
  private val ebayConfig = AppConfig.load().ebay

  def search(accessToken: String, queryParams: Map[String, String]): IO[Seq[EbayItemSummary]] =
    catsSttpBackendResource.get.use { implicit b =>
      basicRequest
        .header("X-EBAY-C-MARKETPLACE-ID", "EBAY_GB")
        .header(HeaderNames.Accept, MediaType.ApplicationJson.toString())
        .contentType(MediaType.ApplicationJson)
        .auth.bearer(accessToken)
        .get(uri"${ebayConfig.baseUri}/buy/browse/v1/item_summary/search?$queryParams")
        .response(asJson[EbayBrowseResult])
        .send()
        .flatMap { r =>
          r.code match {
            case status if status.isSuccess =>
              IO.fromEither(r.body.map(_.itemSummaries.getOrElse(List())))
            case StatusCode.TooManyRequests | StatusCode.Forbidden | StatusCode.Unauthorized =>
              IO.raiseError(AuthError(s"ebay account has expired: ${r.code}"))
            case status =>
              IO(log.error(s"error sending search request to ebay: $status\n${r.body.fold(_.body, _.toString)}")) *>
                IO.raiseError(ApiClientError.HttpError(status.code, s"error sending request to ebay search api: $status"))
          }
        }
    }

  def getItem(accessToken: String, itemId: String): IO[Option[EbayItem]] =
    catsSttpBackendResource.get.use { implicit b =>
      basicRequest
        .header("X-EBAY-C-MARKETPLACE-ID", "EBAY_GB")
        .header(HeaderNames.Accept, MediaType.ApplicationJson.toString())
        .contentType(MediaType.ApplicationJson)
        .auth.bearer(accessToken)
        .get(uri"${ebayConfig.baseUri}/buy/browse/v1/item/$itemId")
        .response(asJson[EbayItem])
        .send()
        .flatMap { r =>
          r.code match {
            case status if status.isSuccess =>
              IO.fromEither(r.body.map(_.some))
            case StatusCode.NotFound =>
              IO.pure(None)
            case StatusCode.TooManyRequests | StatusCode.Forbidden | StatusCode.Unauthorized =>
              IO.raiseError(AuthError(s"ebay account has expired: ${r.code}"))
            case status =>
              IO(log.error(s"error getting item from ebay: $status\n${r.body.fold(_.body, _.toString)}")) *>
                IO.raiseError(ApiClientError.HttpError(status.code, s"error getting item from ebay search api: $status"))
          }
        }
    }
}
