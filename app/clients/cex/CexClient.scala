package clients.cex

import cats.data.EitherT
import cats.implicits._
import domain.{ApiClientError, ResellPrice}
import domain.ApiClientError._
import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Logger}
import play.api.http.{HeaderNames, Status}
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext
import CexSearchResponse._

@Singleton
class CexClient @Inject() (config: Configuration, client: WSClient)(implicit ex: ExecutionContext) {

  private val logger: Logger = Logger(getClass)

  private val cexConfig = config.get[CexConfig]("cex")
  private val searchRequest = client
    .url(s"${cexConfig.baseUri}${cexConfig.searchPath}")
    .addHttpHeaders(HeaderNames.ACCEPT -> "application/json")
    .addHttpHeaders(HeaderNames.CONTENT_TYPE -> "application/json")

  def findResellPrice(query: String): FutureErrorOr[Option[ResellPrice]] = {
    val searchResponse = searchRequest.withQueryStringParameters("q" -> query).get()
      .map(res =>
        res.status match {
          case status if Status.isSuccessful(status) => res.body[Either[ApiClientError, CexSearchResponse]].map(findMinResellPrice(query))
          case Status.TOO_MANY_REQUESTS => none[ResellPrice].asRight[ApiClientError]
          case status => HttpError(status, s"error sending request to cex: ${res.statusText}").asLeft
        }
      )
      .recover(ApiClientError.recoverFromHttpCallFailure.andThen(_.asLeft))

    EitherT(searchResponse)
  }

  private def findMinResellPrice(query: String)(searchResponse: CexSearchResponse): Option[ResellPrice] = {
    val searchResults = searchResponse.response.data.map(_.boxes).getOrElse(Seq())
    logger.info(s"search '$query' returned ${searchResults.size} results")
    if (searchResults.isEmpty) None
    else {
      val minPriceSearchResult: SearchResult = searchResults.minBy(_.exchangePrice)
      ResellPrice(BigDecimal.valueOf(minPriceSearchResult.cashPrice), BigDecimal.valueOf(minPriceSearchResult.exchangePrice)).some
    }
  }
}
