package clients.cex

import java.util.concurrent.TimeUnit

import cats.effect.IO
import cats.implicits._
import common.Logging
import common.config.AppConfig
import common.errors.ApiClientError
import common.errors.ApiClientError.JsonParsingError
import common.resources.SttpBackendResource
import domain.{ItemDetails, PurchasableItem, ResellPrice, SearchQuery}
import io.circe.generic.auto._
import javax.inject.{Inject, Singleton}
import net.jodah.expiringmap.{ExpirationPolicy, ExpiringMap}
import sttp.client._
import sttp.client.circe._
import sttp.model.{HeaderNames, MediaType, StatusCode, Uri}

@Singleton
class CexClient @Inject()(catsSttpBackendResource: SttpBackendResource[IO]) extends Logging {
  import CexClient._

  private val cexConfig = AppConfig.load().cex

  private[cex] val searchResultsCache = ExpiringMap
    .builder()
    .expirationPolicy(ExpirationPolicy.CREATED)
    .expiration(24, TimeUnit.HOURS)
    .build[SearchQuery, Option[ResellPrice]]()

  def findResellPrice(query: SearchQuery): IO[Option[ResellPrice]] =
    if (searchResultsCache.containsKey(query)) IO.pure(searchResultsCache.get(query))
    else
      search(uri"${cexConfig.baseUri}/v3/boxes?q=${query.value}")
        .map(_.flatMap(getMinResellPrice))
        .flatTap { rp =>
          if (rp.isEmpty) IO(logger.warn(s"search '${query.value}' returned 0 results"))
          else IO(searchResultsCache.put(query, rp))
        }

  def getCurrentStock(query: SearchQuery): IO[List[PurchasableItem[ItemDetails.Generic]]] =
    search(uri"${cexConfig.baseUri}/v3/boxes?q=${query.value}&inStock=1&inStockOnline=1")
      .map(_.flatMap(_.response.data).fold(List[SearchResult]())(_.boxes))
      .flatTap(res => IO(logger.info(s""""${query.value}" stock request returned ${res.size} results""")))
      .map { res =>
        res.map(sr => PurchasableItem.generic(sr.boxName, sr.ecomQuantityOnHand, sr.sellPrice))
      }

  private def search(uri: Uri): IO[Option[CexSearchResponse]] =
    catsSttpBackendResource.get.use { implicit b =>
      basicRequest
        .get(uri)
        .contentType(MediaType.ApplicationJson)
        .header(HeaderNames.Accept, MediaType.ApplicationJson.toString())
        .response(asJson[CexSearchResponse])
        .send()
        .flatMap { r =>
          r.code match {
            case s if s.isSuccess =>
              val searchResponse = r.body.left.map {
                case DeserializationError(_, e) => JsonParsingError(s"error parsing json: $e")
                case e                          => JsonParsingError(s"error parsing json: ${e.getMessage}")
              }
              IO.fromEither(searchResponse).map(Some(_))
            case StatusCode.TooManyRequests =>
              IO(logger.error(s"too many requests to cex")) *>
                IO.pure(None)
            case s =>
              IO(logger.error(s"error sending price query to cex: $s\n${r.body.fold(_.body, _.toString)}")) *>
                IO.raiseError(ApiClientError.HttpError(s.code, s"error sending request to cex: $s"))
          }
        }
    }

  private def getMinResellPrice(searchResponse: CexSearchResponse): Option[ResellPrice] =
    for {
      data     <- searchResponse.response.data
      cheapest <- data.boxes.minByOption(_.exchangePrice)
    } yield ResellPrice(BigDecimal.valueOf(cheapest.cashPrice), BigDecimal.valueOf(cheapest.exchangePrice))
}

object CexClient {
  final case class SearchResult(
      boxId: String,
      boxName: String,
      categoryName: String,
      sellPrice: Double,
      exchangePrice: Double,
      cashPrice: Double,
      ecomQuantityOnHand: Int
  )

  final case class SearchResults(
      boxes: List[SearchResult],
      totalRecords: Int,
      minPrice: Double,
      maxPrice: Double
  )

  final case class SearchResponse(data: Option[SearchResults])

  final case class CexSearchResponse(response: SearchResponse)
}
