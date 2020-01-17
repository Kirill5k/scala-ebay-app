package clients.ebay

import java.time.Instant
import java.time.temporal.ChronoField.MILLI_OF_SECOND

import cats.implicits._
import clients.ebay.auth.EbayAuthClient
import clients.ebay.browse.EbayBrowseClient
import clients.ebay.browse.EbayBrowseResponse.EbayItemSummary
import domain.ApiClientError.FutureErrorOr
import domain.{ItemDetails, ListingDetails}

import scala.concurrent.ExecutionContext

trait EbaySearchClient[A <: ItemDetails] {
  private val MIN_FEEDBACK_SCORE = 6
  private val MIN_FEEDBACK_PERCENT = 90

  protected def ex: ExecutionContext

  protected def ebayAuthClient: EbayAuthClient
  protected def ebayBrowseClient: EbayBrowseClient
  protected def categoryId: Int
  protected def newlyListedFilterTemplate: String
  protected def searchQueries: Seq[String]

  protected def search(params: Map[String, String]): FutureErrorOr[Seq[(A, ListingDetails)]]

  def getItemsListedInLastMinutes(minutes: Int): FutureErrorOr[Seq[(A, ListingDetails)]] = {
    val time = Instant.now.minusSeconds(minutes * 60).`with`(MILLI_OF_SECOND, 0)
    val filter = newlyListedFilterTemplate.format(time)
    searchQueries
      .map(getSearchParams(filter, _))
      .map(search)
      .toList
      .sequence
      .map(_.flatten)(ex)
  }

  protected def getSearchParams(filter: String, query: String): Map[String, String] = {
    Map(
      "category_ids" -> categoryId.toString,
      "filter" -> filter,
      "limit" -> "200",
      "q" -> query
    )
  }

  protected val hasTrustedSeller: EbayItemSummary => Boolean = itemSummary => {
    for {
      feedbackPercentage <- itemSummary.seller.feedbackPercentage
      if feedbackPercentage > MIN_FEEDBACK_PERCENT
      feedbackScore <- itemSummary.seller.feedbackScore
      if feedbackScore > MIN_FEEDBACK_SCORE
    } yield ()
  }.isDefined
}
