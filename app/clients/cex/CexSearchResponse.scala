package clients.cex

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._


case class SearchResult(boxId: String, boxName: String, categoryName: String, sellPrice: Int, exchangePrice: Int, cashPrice: Int)

case class SearchResults(boxes: Seq[SearchResult], totalRecords: Int, minPrice: Int, maxPrice: Int)

object SearchResults {
  def empty(): SearchResults = SearchResults(Seq(), 0, 0, 0)
}

case class SearchResponse(data: SearchResults)

case class CexSearchResponse(response: SearchResponse)

object CexSearchResponse {
  implicit val searchResultReads: Reads[SearchResult] = (
      (JsPath \ "boxId").read[String] and
      (JsPath \ "boxName").read[String] and
      (JsPath \ "categoryName").read[String] and
      (JsPath \ "sellPrice").read[Int] and
      (JsPath \ "exchangePrice").read[Int] and
      (JsPath \ "cashPrice").read[Int]
    )(SearchResult.apply _)

  implicit val searchResultsReads: Reads[SearchResults] = (
      (JsPath \ "boxes").read[Seq[SearchResult]] and
      (JsPath \ "totalRecords").read[Int] and
      (JsPath \ "minPrice").read[Int] and
      (JsPath \ "maxPrice").read[Int]
    )(SearchResults.apply _)

  implicit val searchResponseReads: Reads[SearchResponse] = (JsPath \ "data").readNullable[SearchResults].map {
    case Some(data) => SearchResponse(data)
    case None => SearchResponse(SearchResults.empty())
  }

  implicit val cexSearchResponseReads: Reads[CexSearchResponse] = (JsPath \ "response").read[SearchResponse].map(CexSearchResponse.apply)
}
