package clients.cex

import java.util.concurrent.TimeUnit

import cats.data.EitherT
import cats.implicits._
import domain.{ApiClientError, ResellPrice}
import domain.ApiClientError._
import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Logger}
import play.api.http.{HeaderNames, Status}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}
import CexSearchResponse._
import net.jodah.expiringmap.{ExpirationPolicy, ExpiringMap}

@Singleton
class CexClient @Inject() (config: Configuration, client: WSClient)(implicit ex: ExecutionContext) {
  private val logger: Logger = Logger(getClass)

  private val cexConfig = config.get[CexConfig]("cex")
  private val searchRequest = client
    .url(s"${cexConfig.baseUri}${cexConfig.searchPath}")
    .addHttpHeaders(HeaderNames.ACCEPT -> "application/json")
    .addHttpHeaders(HeaderNames.CONTENT_TYPE -> "application/json")

  private[cex] val searchResultsCache = ExpiringMap.builder()
    .expirationPolicy(ExpirationPolicy.CREATED)
    .expiration(24, TimeUnit.HOURS)
    .build[String, ResellPrice]()

  def findResellPrice(query: String): FutureErrorOr[Option[ResellPrice]] = {
    if (searchResultsCache.containsKey(query))
      EitherT.rightT[Future, ApiClientError](Some(searchResultsCache.get(query)))
    else
      EitherT(searchRequest.withQueryStringParameters("q" -> query).get()
        .map { res =>
          res.status match {
            case status if Status.isSuccessful(status) => res.body[Either[ApiClientError, CexSearchResponse]].map(findMinResellPrice(query))
            case Status.TOO_MANY_REQUESTS => none[ResellPrice].asRight[ApiClientError]
            case status => HttpError(status, s"error sending request to cex: ${res.statusText}").asLeft
          }
        }
        .recover(ApiClientError.recoverFromHttpCallFailure.andThen(_.asLeft)))
  }

  private def findMinResellPrice(query: String)(searchResponse: CexSearchResponse): Option[ResellPrice] = {
    val searchResults = searchResponse.response.data.map(_.boxes).getOrElse(Seq())
    if (searchResults.isEmpty) {
      logger.warn(s"search '$query' returned 0 results")
      None
    } else {
      val minPriceSearchResult: SearchResult = searchResults.minBy(_.exchangePrice)
      val resellPrice = ResellPrice(BigDecimal.valueOf(minPriceSearchResult.cashPrice), BigDecimal.valueOf(minPriceSearchResult.exchangePrice))
      searchResultsCache.put(query, resellPrice)
      resellPrice.some
    }
  }
}
