package cex

import exceptions.ApiClientError
import play.api.libs.ws.BodyReadable
import io.circe.generic.auto._
import io.circe.parser._


private[cex] case class SearchResult(boxId: String, boxName: String, categoryName: String, sellPrice: Int, exchangePrice: Int, cashPrice: Int)

private[cex] case class SearchResults(boxes: Seq[SearchResult], totalRecords: Int, minPrice: Int, maxPrice: Int)

private[cex] case class SearchResponse(data: Option[SearchResults])

private[cex] case class CexSearchResponse(response: SearchResponse)

private[cex] object CexSearchResponse {
  implicit val searchResponseBodyReadable = BodyReadable[Either[ApiClientError, CexSearchResponse]] { response =>
    import play.shaded.ahc.org.asynchttpclient.{ Response => AHCResponse }
    val ahcResponse = response.underlying[AHCResponse]
    val responseString = ahcResponse.getResponseBody
    decode[CexSearchResponse](responseString).left.map(ApiClientError.jsonParsingError)
  }
}
