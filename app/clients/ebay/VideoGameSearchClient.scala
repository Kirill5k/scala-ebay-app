package clients.ebay

import cats.implicits._
import clients.ebay.auth.EbayAuthClient
import clients.ebay.browse.EbayBrowseClient
import clients.ebay.mappers.EbayItemMapper._
import domain.ApiClientError.FutureErrorOr
import domain.ItemDetails.GameDetails
import domain.ListingDetails
import javax.inject._

import scala.concurrent.ExecutionContext

@Singleton
class VideoGameSearchClient @Inject()(val ebayAuthClient: EbayAuthClient, val ebayBrowseClient: EbayBrowseClient)(implicit val ex: ExecutionContext)
  extends EbaySearchClient[GameDetails] {

  private val DEFAULT_SEARCH_FILTER = "conditionIds:%7B1000|1500|2000|2500|3000|4000|5000%7D," +
    "deliveryCountry:GB," +
    "price:[0..100]," +
    "priceCurrency:GBP," +
    "itemLocationCountry:GB,"

  protected val categoryId: Int = 139973
  protected val searchQueries: Seq[String] = List("PS4", "XBOX ONE", "SWITCH")

  protected val newlyListedSearchFilterTemplate: String = DEFAULT_SEARCH_FILTER + "buyingOptions:%7BFIXED_PRICE%7D,itemStartDate:[%s]"

  override def search(params: Map[String, String]): FutureErrorOr[Seq[(GameDetails, ListingDetails)]] = {
    searchForItems(params).flatMap { itemSummaries =>
      itemSummaries
        .filter(hasTrustedSeller)
        .map(getCompleteItem)
        .toList
        .sequence
    }
    .leftMap(switchAccountIfItHasExpired)
    .map { items =>
      items
        .flatMap(_.toList)
        .map(_.as[GameDetails])
    }
  }
}
