package clients.ebay

import java.time.Instant
import java.time.temporal.ChronoField.MILLI_OF_SECOND
import java.util.concurrent.TimeUnit

import cats.effect.IO
import cats.implicits._
import clients.ebay.auth.EbayAuthClient
import clients.ebay.browse.EbayBrowseClient
import clients.ebay.browse.EbayBrowseResponse.{EbayItem, EbayItemSummary}
import clients.ebay.mappers.EbayItemMapper
import clients.ebay.mappers.EbayItemMapper._
import common.Logging
import common.errors.ApiClientError
import common.errors.ApiClientError.AuthError
import domain.{ItemDetails, ListingDetails}
import fs2.Stream
import net.jodah.expiringmap.{ExpirationPolicy, ExpiringMap}

trait EbaySearchClient[A <: ItemDetails] extends Logging {

  private val MIN_FEEDBACK_SCORE = 6
  private val MIN_FEEDBACK_PERCENT = 90

  private[ebay] val itemsIds = ExpiringMap.builder()
    .expirationPolicy(ExpirationPolicy.CREATED)
    .expiration(60, TimeUnit.MINUTES)
    .build[String, String]()

  implicit protected def m: EbayItemMapper[A]

  protected def ebayAuthClient: EbayAuthClient
  protected def ebayBrowseClient: EbayBrowseClient
  protected def categoryId: Int
  protected def searchQueries: List[String]
  protected def newlyListedSearchFilterTemplate: String

  protected def removeUnwanted(itemSummary: EbayItemSummary): Boolean

  def getItemsListedInLastMinutes(minutes: Int): Stream[IO, (A, ListingDetails)] = {
    val time = Instant.now.minusSeconds(minutes * 60).`with`(MILLI_OF_SECOND, 0)
    val filter = newlyListedSearchFilterTemplate.format(time).replaceAll("\\{", "%7B").replaceAll("}", "%7D")

    Stream.emits(searchQueries)
      .map(getSearchParams(filter, _))
      .map(searchForItems)
      .flatMap(Stream.evalSeq)
      .evalMap(getCompleteItem)
      .unNone
      .evalTap(item => IO(itemsIds.put(item.itemId, "")))
      .map(_.as[A])
      .handleErrorWith(switchAccountIfItHasExpired)
  }

  private def getSearchParams(filter: String, query: String): Map[String, String] =
    Map(
      "category_ids" -> categoryId.toString,
      "filter" -> filter,
      "limit" -> "200",
      "q" -> query
    )

  private def searchForItems(searchParams: Map[String, String]): IO[Seq[EbayItemSummary]] =
    for {
      token <- ebayAuthClient.accessToken()
      items <- ebayBrowseClient.search(token, searchParams)
      goodItems = items.filter(isNew).filter(hasTrustedSeller).filter(removeUnwanted)
      _ = logger.info(s"search ${searchParams("q")} returned ${goodItems.size} new items (total - ${items.size})")
    } yield goodItems

  private def getCompleteItem(itemSummary: EbayItemSummary): IO[Option[EbayItem]] =
    for {
      token <- ebayAuthClient.accessToken()
      item <- ebayBrowseClient.getItem(token, itemSummary.itemId)
    } yield item

  protected val hasTrustedSeller: EbayItemSummary => Boolean = itemSummary =>
    (for {
      feedbackPercentage <- itemSummary.seller.feedbackPercentage
      if feedbackPercentage > MIN_FEEDBACK_PERCENT
      feedbackScore <- itemSummary.seller.feedbackScore
      if feedbackScore > MIN_FEEDBACK_SCORE
    } yield ()).isDefined

  protected val isNew: EbayItemSummary => Boolean =
    itemSummary => !itemsIds.containsKey(itemSummary.itemId)

  protected val switchAccountIfItHasExpired: PartialFunction[Throwable, Stream[IO, (A, ListingDetails)]] = {
    case AuthError(message) =>
      logger.warn(s"auth error from ebay client, switching account: ${message}")
      ebayAuthClient.switchAccount()
      Stream.empty
    case error: ApiClientError =>
      logger.error(s"api client error while getting items from ebay: ${error.message}")
      Stream.empty
    case error =>
      logger.error(s"unexpected error while getting items from ebay: ${error.getMessage}", error)
      Stream.empty
  }
}
