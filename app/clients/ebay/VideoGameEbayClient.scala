package clients.ebay

import clients.ebay.auth.EbayAuthClient
import clients.ebay.browse.EbayBrowseClient
import clients.ebay.browse.EbayBrowseResponse.EbayItemSummary
import clients.ebay.mappers.EbayItemMapper
import domain.ItemDetails.GameDetails
import javax.inject._


@Singleton
class VideoGameEbayClient @Inject()(val ebayAuthClient: EbayAuthClient, val ebayBrowseClient: EbayBrowseClient) extends EbaySearchClient[GameDetails] {

  implicit override protected val m: EbayItemMapper[GameDetails] = EbayItemMapper.gameDetailsMapper

  private val DEFAULT_SEARCH_FILTER = "conditionIds:{1000|1500|2000|2500|3000|4000|5000}," +
    "deliveryCountry:GB," +
    "price:[0..100]," +
    "priceCurrency:GBP," +
    "itemLocationCountry:GB,"

  private val LISTING_NAME_TRIGGER_WORDS = List(
    "game (code|disc|key|cart)", "(unused|digital|upgrade|download|no) (game|code|key|download)",
    "(software|cartridge|cart|game|disc|cover|box|sleeve) only",
    "(case|variety|accessories) (kit|box)",
    "(cover|carry|travel|carrying|just the|no|hard|storage|game|vault) (case|bag)",
    "(read|see) (detail|desc|post)",
    "digitalcode", "style covers", "credits",
    "coin", "skins", "collectors box", "dutch import", "german version", "soundtrack", "poster",
    "arabic", "case cover",
    "grip stick", "sniper thumbs", "skin grip", "thumbsticks", "grip combat",
    "toycon", "toy con", "dualshock", "efigs", "gamepad", "joycon", "joy con", "controller", "stand holder",
    "headset", "\\bhdmi\\b", "\\busb\\b", "\\bhdd\\b", "dual shock", "dualshock", "nintendo labo",
    "player generator", "100k", "50k", "dlc", "pve", "starter pack",
    "preorder", "season pass", "steelbook", "ring fit", "lego dimensions", "minifigure", "collectable", "collectible",
    "family membership", "12 month", "dynamic theme", "account", "game(\\s+)?pass",
    "phone case", "phone covers", "samsung", "huawei", "iphone",
    "\\bhori\\b",
    "ark survival(\\s+(\\w+|\\d+)){5,}",
    "diablo 3(\\s+(\\w+|\\d+)){6,}",
    "fifa 20(\\s+(\\w+|\\d+)){5,}", "fallout 76(\\s+(\\w+|\\d+)){5,}", "((\\w+|\\d+)\\s+){3,}fallout 76", "borderlands 3(\\s+(\\w+|\\d+)){5,}",
    "((\\w+|\\d+)\\s+){1,}rocket league(\\s+(\\w+|\\d+)){2,}", "((\\w+|\\d+)\\s+){3,}rocket league", "rocket league(\\s+(\\w+|\\d+)){5,}",
    "((\\w+|\\d+)\\s+){3,}swordshield", "((\\w+|\\d+)\\s+){3,}pokemon", "pokemon(\\s+(\\w+|\\d+)){6,}"
  ).mkString("^.*?(?i)(", "|", ").*$").r

  override protected val categoryId: Int = 139973
  override protected val searchQueries: List[String] = List("PS4", "XBOX ONE", "SWITCH")

  override protected val newlyListedSearchFilterTemplate: String = DEFAULT_SEARCH_FILTER + "buyingOptions:{FIXED_PRICE},itemStartDate:[%s]"

  override protected def removeUnwanted(itemSummary: EbayItemSummary): Boolean =
    !LISTING_NAME_TRIGGER_WORDS.matches(itemSummary.title.replaceAll("[^a-zA-Z0-9 ]", ""))
}
