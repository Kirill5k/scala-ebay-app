package clients.cex

import domain.ApiClientError
import domain.ApiClientError._
import play.api.libs.ws.BodyReadable
import io.circe.generic.auto._
import io.circe.parser._


private[cex] object CexSearchResponse {
  final case class SearchResult(boxId: String, boxName: String, categoryName: String, sellPrice: Double, exchangePrice: Double, cashPrice: Double)
  final case class SearchResults(boxes: Seq[SearchResult], totalRecords: Int, minPrice: Double, maxPrice: Double)
  final case class SearchResponse(data: Option[SearchResults])
  final case class CexSearchResponse(response: SearchResponse)

  implicit val searchResponseBodyReadable = BodyReadable[Either[ApiClientError, CexSearchResponse]] { response =>
    import play.shaded.ahc.org.asynchttpclient.{ Response => AHCResponse }
    val ahcResponse = response.underlying[AHCResponse]
    val responseString = ahcResponse.getResponseBody
    decode[CexSearchResponse](responseString).left.map(e => JsonParsingError(e.getMessage))
  }
}
