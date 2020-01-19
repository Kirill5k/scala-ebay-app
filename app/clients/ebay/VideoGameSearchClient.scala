package clients.ebay

import cats.implicits._
import clients.ebay.auth.EbayAuthClient
import clients.ebay.browse.{EbayBrowseClient, EbayBrowseResponse}
import clients.ebay.browse.EbayBrowseResponse.EbayItemSummary
import clients.ebay.mappers.EbayItemMapper._
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

  private val LISTING_NAME_TRIGGER_WORDS = List(
    "digital code", "digital-code", "download code", "upgrade code", "style covers", "no case", "credits", "read description",
    "coin", "skins", "bundle", "no game", "digital key", "download key", "just the case", "cartridge only", "disc only",
    "fallout 76(\\s\\w+){4,}", "borderlands 3(\\s\\w+){4,}", "rocket league(\\s\\w+){4,}",
    "player generator", "card generator"
  ).mkString("^.*?(?i)(", "|", ").*$").r

  protected val categoryId: Int = 139973
  protected val searchQueries: List[String] = List("PS4", "XBOX ONE", "SWITCH")

  protected val newlyListedSearchFilterTemplate: String = DEFAULT_SEARCH_FILTER + "buyingOptions:{FIXED_PRICE},itemStartDate:[%s]"

  override protected def removeUnwanted(itemSummary: EbayItemSummary): Boolean =
    hasTrustedSeller(itemSummary) && !LISTING_NAME_TRIGGER_WORDS.matches(itemSummary.title)

  override protected def toDomain(items: Seq[Option[EbayBrowseResponse.EbayItem]]): Seq[(GameDetails, ListingDetails)] =
    for {
      itemOpt <- items
      item <- itemOpt.toList
    } yield item.as[GameDetails]
}
