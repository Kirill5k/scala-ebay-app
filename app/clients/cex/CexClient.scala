package clients.cex

import java.util.concurrent.TimeUnit

import cats.data.EitherT
import cats.implicits._
import domain.{ApiClientError, ItemDetails, ResellPrice}
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
  import CexClientOps._
  private val logger: Logger = Logger(getClass)

  private val cexConfig = config.get[CexConfig]("cex")
  private val searchRequest = client
    .url(s"${cexConfig.baseUri}${cexConfig.searchPath}")
    .addHttpHeaders(HeaderNames.ACCEPT -> "application/json")
    .addHttpHeaders(HeaderNames.CONTENT_TYPE -> "application/json")

  private[cex] val searchResultsCache = ExpiringMap.builder()
    .expirationPolicy(ExpirationPolicy.CREATED)
    .expiration(24, TimeUnit.HOURS)
    .build[String, Option[ResellPrice]]()

  def findResellPrice(itemDetails: ItemDetails): FutureErrorOr[Option[ResellPrice]] = {
    EitherT.rightT[Future, ApiClientError](itemDetails.searchQuery).flatMap {
      case Some(query) if searchResultsCache.containsKey(query) =>
        EitherT.rightT[Future, ApiClientError](searchResultsCache.get(query))
      case Some(query) =>
        findResellPrice(query)
      case None =>
        logger.warn(s"not enough details to query for resell price $itemDetails")
        EitherT.rightT[Future, ApiClientError](none[ResellPrice])
    }
  }

  private def findResellPrice(query: String): FutureErrorOr[Option[ResellPrice]] =
    EitherT(searchRequest.withQueryStringParameters("q" -> query).get()
      .map { res =>
        res.status match {
          case status if Status.isSuccessful(status) => res.body[Either[ApiClientError, CexSearchResponse]].map(findMinResellPrice(query))
          case Status.TOO_MANY_REQUESTS => none[ResellPrice].asRight[ApiClientError]
          case status => HttpError(status, s"error sending request to cex: ${res.statusText}").asLeft
        }
      }
      .recover(ApiClientError.recoverFromHttpCallFailure.andThen(_.asLeft)))

  private def findMinResellPrice(query: String)(searchResponse: CexSearchResponse): Option[ResellPrice] = {
    val resellPrice = searchResponse.response.data
      .map(_.boxes).getOrElse(Seq())
      .minByOption(_.exchangePrice)
      .map(minPriceResult => ResellPrice(BigDecimal.valueOf(minPriceResult.cashPrice), BigDecimal.valueOf(minPriceResult.exchangePrice)))

    if (resellPrice.isEmpty) logger.warn(s"search '$query' returned 0 results")

    searchResultsCache.put(query, resellPrice)
    resellPrice
  }
}
