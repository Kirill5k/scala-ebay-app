package clients.cex

import cats.data.EitherT
import cats.implicits._
import configs.CexConfig
import domain.ResellPrice
import exceptions.ApiClientError
import javax.inject.Inject
import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}

class CexClient @Inject() (config: Configuration, client: WSClient)(implicit ex: ExecutionContext) {

  private val logger: Logger = Logger(getClass)

  private val cexConfig = config.get[CexConfig]("cex")
  private val searchRequest = client
    .url(s"${cexConfig.baseUri}${cexConfig.searchPath}")
    .addHttpHeaders("Accept" -> "application/json")
    .addHttpHeaders("Content-Type" -> "application/json")

  def findResellPrice(query: String): Future[Either[Throwable, ResellPrice]] = {
    val searchResponse = searchRequest.withQueryStringParameters("q" -> query).get()
      .map(res =>
        if (Status.isSuccessful(res.status)) Right(res.body[JsValue].as[CexSearchResponse])
        else Left(ApiClientError(res.status, s"error sending request to cex: ${res.statusText}"))
      )
      .recover {
        case error: Throwable => Left(ApiClientError(Status.INTERNAL_SERVER_ERROR, error.getMessage))
      }

    EitherT(searchResponse)
      .map(_.response.data.boxes)
      .map(findMinResellPrice(query, _))
      .value
  }

  private def findMinResellPrice(query: String, searchResults: Seq[SearchResult]): ResellPrice = {
    logger.info(s"search '$query' returned ${searchResults.size} results")
    if (searchResults.isEmpty) ResellPrice.empty()
    else {
      val minPriceSearchResult: SearchResult = searchResults.minBy(_.exchangePrice)
      ResellPrice(BigDecimal.valueOf(minPriceSearchResult.cashPrice), BigDecimal.valueOf(minPriceSearchResult.exchangePrice))
    }
  }
}
