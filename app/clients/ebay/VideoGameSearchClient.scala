package clients.ebay

import java.time.Instant
import java.time.temporal.ChronoField.MILLI_OF_SECOND

import cats.implicits._
import clients.ebay.auth.EbayAuthClient
import clients.ebay.browse.EbayBrowseClient
import clients.ebay.browse.EbayBrowseResponse.EbayItemSummary
import domain.ApiClientError.FutureErrorOr
import domain.ItemDetails.GameDetails
import domain.ListingDetails
import javax.inject._

import scala.concurrent.ExecutionContext

@Singleton
class VideoGameSearchClient @Inject()(val ebayAuthClient: EbayAuthClient, val ebayBrowseClient: EbayBrowseClient)(implicit ex: ExecutionContext)
  extends EbaySearchClient[GameDetails] {
  protected val categoryId: Int = 139973

  private val DEFAULT_FILTER = "conditionIds:%7B1000|1500|2000|2500|3000|4000|5000%7D," +
    "deliveryCountry:GB," +
    "price:[0..100]," +
    "priceCurrency:GBP," +
    "itemLocationCountry:GB,"

  protected val newlyListedFilterTemplate: String = DEFAULT_FILTER + "buyingOptions:%7BFIXED_PRICE%7D,itemStartDate:[%s]"

  override def search(params: Map[String, String]): FutureErrorOr[Seq[(GameDetails, ListingDetails)]] = {
    ebayAuthClient.accessToken().flatMap(t => ebayBrowseClient.search(t, params))
      .flatMap { itemSummaries =>
        itemSummaries
          .filter(hasTrustedSeller)
          .map(getListingDetails)
          .toList
          .sequence
      }
      .map { listings =>
        listings
          .flatMap(_.toList)
          .map(ld => (ld.as[GameDetails], ld))
      }
  }

  protected def getListingDetails(itemSummary: EbayItemSummary): FutureErrorOr[Option[ListingDetails]] =
    ebayAuthClient.accessToken().flatMap(t => ebayBrowseClient.getItem(t, itemSummary.itemId))
}
