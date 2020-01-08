package cex

import cats.data.EitherT
import cats.implicits._
import domain.ResellPrice
import exceptions.ApiClientError.FutureErrorOr
import exceptions.{ApiClientError, HttpError}
import javax.inject.Inject
import play.api.{Configuration, Logger}
import play.api.http.Status
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext

class CexClient @Inject() (config: Configuration, client: WSClient)(implicit ex: ExecutionContext) {

  private val logger: Logger = Logger(getClass)

  private val cexConfig = config.get[CexConfig]("cex")
  private val searchRequest = client
    .url(s"${cexConfig.baseUri}${cexConfig.searchPath}")
    .addHttpHeaders("Accept" -> "application/json")
    .addHttpHeaders("Content-Type" -> "application/json")

  def findResellPrice(query: String): FutureErrorOr[ResellPrice] = {
    val searchResponse = searchRequest.withQueryStringParameters("q" -> query).get()
      .map(res =>
        if (Status.isSuccessful(res.status)) res.body[JsValue].as[CexSearchResponse].asRight
        else HttpError(res.status, s"error sending request to cex: ${res.statusText}").asLeft
      )
      .recover(ApiClientError.recoverFromHttpCallFailure.andThen(_.asLeft))

    EitherT(searchResponse)
      .map(_.response.data.boxes)
      .map(findMinResellPrice(query, _))
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
