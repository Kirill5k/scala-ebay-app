package clients.cex

import java.util.concurrent.TimeUnit

import cats.effect.IO
import cats.implicits._
import domain.{ApiClientError, ItemDetails, ResellPrice}
import io.circe.generic.auto._
import javax.inject.{Inject, Singleton}
import net.jodah.expiringmap.{ExpirationPolicy, ExpiringMap}
import play.api.{Configuration, Logger}
import resources.SttpBackendResource
import sttp.client._
import sttp.client.circe._
import sttp.model.{HeaderNames, MediaType, StatusCode}

@Singleton
class CexClient @Inject()(config: Configuration, catsSttpBackendResource: SttpBackendResource[IO]) {
  import CexClient._

  private val log: Logger = Logger(getClass)

  private val cexConfig = config.get[CexConfig]("cex")

  private[cex] val searchResultsCache = ExpiringMap
    .builder()
    .expirationPolicy(ExpirationPolicy.CREATED)
    .expiration(24, TimeUnit.HOURS)
    .build[String, Option[ResellPrice]]()

  def findResellPrice(itemDetails: ItemDetails): IO[Option[ResellPrice]] =
    IO.pure(itemDetails.summary).flatMap {
      case Some(query) if searchResultsCache.containsKey(query) =>
        IO.pure(searchResultsCache.get(query))
      case Some(query) =>
        queryResellPrice(query)
      case None =>
        IO.delay(log.warn(s"not enough details to query for resell price $itemDetails")) *>
          IO.pure(none[ResellPrice])
    }

  private def queryResellPrice(query: String): IO[Option[ResellPrice]] = {
    catsSttpBackendResource.get.use { implicit b =>
      basicRequest
        .get(uri"${cexConfig.baseUri}/v3/boxes?q=${query}")
        .contentType(MediaType.ApplicationJson)
        .header(HeaderNames.Accept, MediaType.ApplicationJson.toString())
        .response(asJson[CexSearchResponse])
        .send()
        .flatMap { r =>
          r.code match {
            case status if status.isSuccess =>
              IO.fromEither(r.body.map(getMinResellPrice(query)).left.map(ApiClientError.recoverFromHttpCallFailure))
            case StatusCode.TooManyRequests =>
              IO(log.error(s"too many requests to cex")) *>
                IO.pure(none[ResellPrice])
            case status =>
              IO(log.error(s"error sending price query to cex: ${r.body} ${r}")) *>
                IO.raiseError(ApiClientError.HttpError(status.code, s"error sending request to cex: $status"))
          }
        }
    }
  }

  private def getMinResellPrice(query: String)(searchResponse: CexSearchResponse): Option[ResellPrice] = {
    val resellPrice = for {
      data     <- searchResponse.response.data
      cheapest <- data.boxes.minByOption(_.exchangePrice)
    } yield ResellPrice(BigDecimal.valueOf(cheapest.cashPrice), BigDecimal.valueOf(cheapest.exchangePrice))

    if (resellPrice.isEmpty) log.warn(s"search '$query' returned 0 results")
    else searchResultsCache.put(query, resellPrice)

    resellPrice
  }
}

object CexClient {
  final case class SearchResult(
      boxId: String,
      boxName: String,
      categoryName: String,
      sellPrice: Double,
      exchangePrice: Double,
      cashPrice: Double
  )

  final case class SearchResults(
      boxes: Seq[SearchResult],
      totalRecords: Int,
      minPrice: Double,
      maxPrice: Double
  )

  final case class SearchResponse(data: Option[SearchResults])

  final case class CexSearchResponse(response: SearchResponse)
}
