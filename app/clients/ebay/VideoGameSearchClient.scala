package clients.ebay

import java.time.Instant

import cats.implicits._
import clients.ebay.auth.EbayAuthClient
import clients.ebay.browse.EbayBrowseClient
import domain.ApiClientError.FutureErrorOr
import domain.ItemDetails.GameDetails
import domain.ListingDetails
import javax.inject._

import scala.concurrent.ExecutionContext

@Singleton
class VideoGameSearchClient @Inject()(ebayAuthClient: EbayAuthClient, ebayBrowseClient: EbayBrowseClient)(implicit ex: ExecutionContext)
  extends EbaySearchClient[GameDetails] {
  private val VIDEO_GAMES_CATEGORY_ID = 139973

  private val DEFAULT_FILTER = "conditionIds:{1000|1500|2000|2500|3000|4000|5000}," +
    "deliveryCountry:GB," +
    "price:[0..100]," +
    "priceCurrency:GBP," +
    "itemLocationCountry:GB,"

  private val NEWLY_LISTED_FILTER = DEFAULT_FILTER + "buyingOptions:{FIXED_PRICE},itemStartDate:[%s]"

  override def getItemsListedInLastMinutes(minutes: Int): FutureErrorOr[Seq[(GameDetails, ListingDetails)]] = {
    val filter = searchFilterWithTime(NEWLY_LISTED_FILTER, Instant.now.minusSeconds(minutes * 60))
    val params = searchParams(VIDEO_GAMES_CATEGORY_ID, filter)
    search(params)
  }

  private def search(params: Map[String, String]): FutureErrorOr[Seq[(GameDetails, ListingDetails)]] = {
    ebayAuthClient.accessToken().flatMap(t => ebayBrowseClient.search(t, params))
      .flatMap {
        _.filter(hasTrustedSeller)
          .map(item => ebayAuthClient.accessToken().flatMap(t => ebayBrowseClient.getItem(t, item.itemId)))
          .toList
          .sequence
      }
      .map {
        _.map(ld => (ld.as[GameDetails], ld))
      }
  }
}
