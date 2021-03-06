package clients.ebay

import java.time.Instant
import java.time.temporal.ChronoField.MILLI_OF_SECOND
import java.util.concurrent.TimeUnit

import cats.effect.IO
import clients.ebay.auth.EbayAuthClient
import clients.ebay.browse.EbayBrowseClient
import clients.ebay.browse.EbayBrowseResponse.{EbayItem, EbayItemSummary}
import clients.ebay.mappers.EbayItemMapper
import clients.ebay.params.EbaySearchParams
import common.Logging
import common.errors.ApiClientError
import common.errors.ApiClientError.AuthError
import domain.{ItemDetails, ResellableItem, SearchQuery}
import fs2.Stream
import javax.inject.{Inject, Singleton}
import net.jodah.expiringmap.{ExpirationPolicy, ExpiringMap}

@Singleton
class EbaySearchClient @Inject()(
    val ebayAuthClient: EbayAuthClient,
    val ebayBrowseClient: EbayBrowseClient
) extends Logging {

  private val MIN_FEEDBACK_SCORE   = 6
  private val MIN_FEEDBACK_PERCENT = 90

  private[ebay] val itemsIds = ExpiringMap
    .builder()
    .expirationPolicy(ExpirationPolicy.CREATED)
    .expiration(60, TimeUnit.MINUTES)
    .build[String, String]()

  def findItemsListedInLastMinutes[D <: ItemDetails](
      query: SearchQuery,
      minutes: Int
  )(
      implicit mapper: EbayItemMapper[D],
      params: EbaySearchParams[D]
  ): Stream[IO, ResellableItem[D]] = {
    val time   = Instant.now.minusSeconds(minutes * 60).`with`(MILLI_OF_SECOND, 0)
    val filter = params.searchFilterTemplate.format(time).replaceAll("\\{", "%7B").replaceAll("}", "%7D")

    Stream(query)
      .map(getSearchParams(filter, params.categoryId))
      .map(searchForItems(params.removeUnwanted))
      .flatMap(Stream.evalSeq)
      .evalMap(getCompleteItem)
      .unNone
      .evalTap(item => IO(itemsIds.put(item.itemId, "")))
      .map(mapper.toDomain)
      .handleErrorWith(switchAccountIfItHasExpired)
  }

  private def getSearchParams(filter: String, categoryId: Int)(query: SearchQuery): Map[String, String] =
    Map(
      "category_ids" -> categoryId.toString,
      "filter"       -> filter,
      "limit"        -> "200",
      "q"            -> query.value
    )

  private def searchForItems(removeUnwanted: EbayItemSummary => Boolean)(searchParams: Map[String, String]): IO[Seq[EbayItemSummary]] =
    for {
      token <- ebayAuthClient.accessToken
      items <- ebayBrowseClient.search(token, searchParams)
      goodItems = items.filter(isNew).filter(hasTrustedSeller).filter(removeUnwanted)
      _         = logger.info(s"search ${searchParams("q")} returned ${goodItems.size} new items (total - ${items.size})")
    } yield goodItems

  private def getCompleteItem(itemSummary: EbayItemSummary): IO[Option[EbayItem]] =
    for {
      token <- ebayAuthClient.accessToken
      item  <- ebayBrowseClient.getItem(token, itemSummary.itemId)
    } yield item

  private val hasTrustedSeller: EbayItemSummary => Boolean = itemSummary =>
    (for {
      feedbackPercentage <- itemSummary.seller.feedbackPercentage
      if feedbackPercentage > MIN_FEEDBACK_PERCENT
      feedbackScore <- itemSummary.seller.feedbackScore
      if feedbackScore > MIN_FEEDBACK_SCORE
    } yield ()).isDefined

  private val isNew: EbayItemSummary => Boolean =
    itemSummary => !itemsIds.containsKey(itemSummary.itemId)

  private def switchAccountIfItHasExpired[D <: ItemDetails]: PartialFunction[Throwable, Stream[IO, ResellableItem[D]]] = {
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
