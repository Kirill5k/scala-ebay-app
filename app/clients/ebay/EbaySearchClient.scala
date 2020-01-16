package clients.ebay

import java.time.Instant
import java.time.temporal.ChronoField.MILLI_OF_SECOND

import clients.ebay.browse.EbayBrowseResponse.EbayItemSummary
import domain.ApiClientError.FutureErrorOr
import domain.{ItemDetails, ListingDetails}

trait EbaySearchClient[A <: ItemDetails] {
  private val MIN_FEEDBACK_SCORE = 6
  private val MIN_FEEDBACK_PERCENT = 90

  def getItemsListedInLastMinutes(minutes: Int): FutureErrorOr[Seq[(A, ListingDetails)]]

  protected def searchParams(categoryId: Int, filter: String): Map[String, String] = {
    Map(
      "category_ids" -> categoryId.toString,
      "filter" -> filter,
      "limit" -> "200"
    )
  }

  protected def searchFilterWithTime(filter: String, time: Instant): String = {
    filter.format(time.`with`(MILLI_OF_SECOND, 0))
      .replaceAll("\\{", "%7B")
      .replaceAll("}", "%7D")
  }

  protected def hasTrustedSeller(itemSummary: EbayItemSummary): Boolean = {
    for {
      feedbackPercentage <- itemSummary.seller.feedbackPercentage
      if feedbackPercentage > MIN_FEEDBACK_PERCENT
      feedbackScore <- itemSummary.seller.feedbackScore
      if feedbackScore > MIN_FEEDBACK_SCORE
    } yield ()
  }.isDefined
}
