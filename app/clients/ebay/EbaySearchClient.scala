package clients.ebay

import java.time.Instant
import java.time.temporal.ChronoField.MILLI_OF_SECOND
import java.util.concurrent.TimeUnit

import cats.implicits._
import clients.ebay.auth.EbayAuthClient
import clients.ebay.browse.EbayBrowseClient
import clients.ebay.browse.EbayBrowseResponse.{EbayItem, EbayItemSummary}
import clients.ebay.mappers.EbayItemMapper
import clients.ebay.mappers.EbayItemMapper._
import domain.ApiClientError.{AuthError, FutureErrorOr}
import domain.{ApiClientError, ItemDetails, ListingDetails}
import net.jodah.expiringmap.{ExpirationPolicy, ExpiringMap}
import play.api.Logger

import scala.concurrent.ExecutionContext

trait EbaySearchClient[A <: ItemDetails] {
  private val logger: Logger = Logger(getClass)

  private val MIN_FEEDBACK_SCORE = 6
  private val MIN_FEEDBACK_PERCENT = 90

  private val itemsIds = ExpiringMap.builder()
    .expirationPolicy(ExpirationPolicy.CREATED)
    .expiration(60, TimeUnit.MINUTES)
    .build[String, String]()

  implicit protected def ex: ExecutionContext
  implicit protected def m: EbayItemMapper[A]

  protected def ebayAuthClient: EbayAuthClient
  protected def ebayBrowseClient: EbayBrowseClient
  protected def categoryId: Int
  protected def searchQueries: List[String]
  protected def newlyListedSearchFilterTemplate: String

  protected def removeUnwanted(itemSummary: EbayItemSummary): Boolean

  def getItemsListedInLastMinutes(minutes: Int): FutureErrorOr[Seq[(A, ListingDetails)]] = {
    val time = Instant.now.minusSeconds(minutes * 60).`with`(MILLI_OF_SECOND, 0)
    val filter = newlyListedSearchFilterTemplate.format(time).replaceAll("\\{", "%7B").replaceAll("}", "%7D")
    searchQueries
      .map(getSearchParams(filter, _))
      .map(searchForItems)
      .sequence
      .map(_.flatten.filter(removeUnwanted))
      .flatMap(_.map(getCompleteItem).sequence)
      .map(transformItemsToDomain)
      .leftMap(switchAccountIfItHasExpired)
  }

  private def getSearchParams(filter: String, query: String): Map[String, String] =
    Map(
      "category_ids" -> categoryId.toString,
      "filter" -> filter,
      "limit" -> "200",
      "q" -> query
    )

  private def searchForItems(searchParams: Map[String, String]): FutureErrorOr[Seq[EbayItemSummary]] =
    for {
      token <- ebayAuthClient.accessToken()
      itemSummaries <- ebayBrowseClient.search(token, searchParams)
    } yield {
      logger.info(s"search ${searchParams("q")} returned ${itemSummaries.size} items")
      itemSummaries
    }

  private def getCompleteItem(itemSummary: EbayItemSummary): FutureErrorOr[Option[EbayItem]] =
    for {
      token <- ebayAuthClient.accessToken()
      item <- ebayBrowseClient.getItem(token, itemSummary.itemId)
    } yield item

  private def transformItemsToDomain(items: Seq[Option[EbayItem]]): Seq[(A, ListingDetails)] =
    for {
      item <- items.flatten
    } yield {
      itemsIds.put(item.itemId, "")
      item.as[A]
    }

  protected val hasTrustedSeller: EbayItemSummary => Boolean = itemSummary => {
    for {
      feedbackPercentage <- itemSummary.seller.feedbackPercentage
      if feedbackPercentage > MIN_FEEDBACK_PERCENT
      feedbackScore <- itemSummary.seller.feedbackScore
      if feedbackScore > MIN_FEEDBACK_SCORE
    } yield ()
  }.isDefined

  protected val isNew: EbayItemSummary => Boolean = itemSummary => !itemsIds.containsKey(itemSummary.itemId)

  protected val switchAccountIfItHasExpired: PartialFunction[ApiClientError, ApiClientError] = {
    case error @ AuthError(message) =>
      logger.warn(s"switching ebay account: ${message}")
      ebayAuthClient.switchAccount()
      error
    case error => error
  }
}
