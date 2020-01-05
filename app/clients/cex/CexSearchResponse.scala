package clients.cex

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class SearchError(code: String, internalMessage: String)

case class SearchResult(boxId: String, boxName: String, categoryName: String, sellPrice: Int, exchangePrice: Int, cashPrice: Int)

case class SearchResults(boxes: Seq[SearchResult], totalRecords: Int, minPrice: Int, maxPrice: Int)

case class SearchResponse(ack: String, data: SearchResults, error: SearchError)

case class CexSearchResponse(response: SearchResponse)

object CexSearchResponse {
  implicit val searchErrorReads: Reads[SearchError] = (
    (JsPath \ "code").read[String] and
    (JsPath \ "internal_message").read[String]
  )(SearchError.apply _)

  implicit val searchResultReads: Reads[SearchResult] = (
      (JsPath \ "boxId").read[String] and
      (JsPath \ "boxName").read[String] and
      (JsPath \ "categoryName").read[String] and
      (JsPath \ "sellPrice").read[Int] and
      (JsPath \ "cashPrice").read[Int] and
      (JsPath \ "exchangePrice").read[Int]
    )(SearchResult.apply _)

  implicit val searchResultsReads: Reads[SearchResults] = (
      (JsPath \ "boxes").read[Seq[SearchResult]] and
      (JsPath \ "totalRecords").read[Int] and
      (JsPath \ "minPrice").read[Int] and
      (JsPath \ "maxPrice").read[Int]
    )(SearchResults.apply _)

  implicit val searchResponseReads: Reads[SearchResponse] = (
      (JsPath \ "ack").read[String] and
      (JsPath \ "data").read[SearchResults] and
      (JsPath \ "error").read[SearchError]
    )(SearchResponse.apply _)

  implicit val cexSearchResponseReads: Reads[CexSearchResponse] = ((JsPath \ "response").read[SearchResponse])(CexSearchResponse.apply _)
}
