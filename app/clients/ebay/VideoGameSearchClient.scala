package clients.ebay

import cats.implicits._
import clients.ebay.auth.EbayAuthClient
import clients.ebay.browse.{EbayBrowseClient, EbayBrowseResponse}
import clients.ebay.browse.EbayBrowseResponse.EbayItemSummary
import clients.ebay.mappers.EbayItemMapper._
import domain.ApiClientError.FutureErrorOr
import domain.ItemDetails.GameDetails
import domain.ListingDetails
import javax.inject._

import scala.concurrent.ExecutionContext

@Singleton
class VideoGameSearchClient @Inject()(val ebayAuthClient: EbayAuthClient, val ebayBrowseClient: EbayBrowseClient)(implicit val ex: ExecutionContext)
  extends EbaySearchClient[GameDetails] {

  private val DEFAULT_SEARCH_FILTER = "conditionIds:{1000|1500|2000|2500|3000|4000|5000}," +
    "deliveryCountry:GB," +
    "price:[0..100]," +
    "priceCurrency:GBP," +
    "itemLocationCountry:GB,"

  protected val categoryId: Int = 139973
  protected val searchQueries: Seq[String] = List("PS4", "XBOX ONE", "SWITCH")

  protected val newlyListedSearchFilterTemplate: String = DEFAULT_SEARCH_FILTER + "buyingOptions:{FIXED_PRICE},itemStartDate:[%s]"

  override protected def removeUntrusted(itemSummary: EbayItemSummary): Boolean =
    hasTrustedSeller(itemSummary)

  override protected def toDomain(items: Seq[Option[EbayBrowseResponse.EbayItem]]): Seq[(GameDetails, ListingDetails)] =
    items.flatMap(_.toList).map(_.as[GameDetails])
}
