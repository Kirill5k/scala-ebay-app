package clients.cex

import java.util.concurrent.TimeUnit


import cats.effect.{ContextShift, IO}
import cats.implicits._
import domain.{ApiClientError, ItemDetails, ResellPrice}
import domain.ApiClientError._
import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Logger}
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext
import CexSearchResponse._
import net.jodah.expiringmap.{ExpirationPolicy, ExpiringMap}

@Singleton
class CexClient @Inject() (config: Configuration, client: WSClient)(implicit ex: ExecutionContext) {
  private implicit val cs: ContextShift[IO] = IO.contextShift(ex)

  private val log: Logger = Logger(getClass)

  private val cexConfig = config.get[CexConfig]("cex")
  private val searchRequest = client
    .url(s"${cexConfig.baseUri}${cexConfig.searchPath}")
    .addHttpHeaders(HeaderNames.ACCEPT -> "application/json")
    .addHttpHeaders(HeaderNames.CONTENT_TYPE -> "application/json")

  private[cex] val searchResultsCache = ExpiringMap.builder()
    .expirationPolicy(ExpirationPolicy.CREATED)
    .expiration(24, TimeUnit.HOURS)
    .build[String, Option[ResellPrice]]()

  def findResellPrice(itemDetails: ItemDetails): IOErrorOr[Option[ResellPrice]] =
    IO(itemDetails.summary).flatMap {
      case Some(query) if searchResultsCache.containsKey(query) =>
        IO(Right(searchResultsCache.get(query)))
      case Some(query) =>
        queryResellPrice(query)
      case None =>
        log.warn(s"not enough details to query for resell price $itemDetails")
        IO(Right(none[ResellPrice]))
    }

  private def queryResellPrice(query: String): IOErrorOr[Option[ResellPrice]] =
    IO.fromFuture(IO(searchRequest.withQueryStringParameters("q" -> query).get()
      .map { res =>
        res.status match {
          case status if isSuccessful(status) => res.body[Either[ApiClientError, CexSearchResponse]].map(getMinResellPrice(query))
          case TOO_MANY_REQUESTS => none[ResellPrice].asRight[ApiClientError]
          case status => HttpError(status, s"error sending request to cex: ${res.statusText}").asLeft
        }
      }
      .recover(ApiClientError.recoverFromHttpCallFailure.andThen(_.asLeft))))

  private def getMinResellPrice(query: String)(searchResponse: CexSearchResponse): Option[ResellPrice] = {
    val resellPrice = searchResponse.response.data
      .map(_.boxes).getOrElse(Seq())
      .minByOption(_.exchangePrice)
      .map(minPriceResult => ResellPrice(BigDecimal.valueOf(minPriceResult.cashPrice), BigDecimal.valueOf(minPriceResult.exchangePrice)))

    if (resellPrice.isEmpty) log.warn(s"search '$query' returned 0 results")
    else searchResultsCache.put(query, resellPrice)

    resellPrice
  }
}
