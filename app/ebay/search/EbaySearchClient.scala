package ebay.search

import cats.data.EitherT
import cats.implicits._
import ebay.EbayConfig
import exceptions.ApiClientError.FutureErrorOr
import exceptions.{ApiClientError, AuthError, HttpError}
import javax.inject.Inject
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.JsValue
import play.api.libs.ws.{WSClient}
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext

class EbaySearchClient @Inject()(config: Configuration, client: WSClient)(implicit ex: ExecutionContext) {
  private val logger: Logger = Logger(getClass)

  private val ebayConfig = config.get[EbayConfig]("ebay")

  private val defaultHeaders = Map(
    HeaderNames.CONTENT_TYPE -> "application/json",
    HeaderNames.ACCEPT -> "application/json",
    "X-EBAY-C-MARKETPLACE-ID" -> "EBAY_GB"
  ).toList

  private val searchRequest = client.url(s"${ebayConfig.baseUri}${ebayConfig.searchPath}").addHttpHeaders(defaultHeaders: _*)
  private val getItemRequest = client.url(s"${ebayConfig.baseUri}${ebayConfig.itemPath}").addHttpHeaders(defaultHeaders: _*)
}
