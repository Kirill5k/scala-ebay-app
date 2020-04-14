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
    "itemLocationCountry:GB," +
    "deliveryCountry:GB," +
    "price:[0..100]," +
    "priceCurrency:GBP," +
    "itemLocationCountry:GB,"

  private val LISTING_NAME_TRIGGER_WORDS = List(
    "(demo|game)( )?(code|disc|key|cart|pass)", "(unused|digital|upgrade|no)( )?(redeem )?(game|code|key)",
    "(software|cartridge(s)?|cart|game|disc(s)?|cover|box|sleeve|book) only",
    "(case|variety|accessories|storage|charge) (kit|box)",
    "(replacement|cover|carry|travel|carrying|just the|no|hard|storage|game|vault|phone|card|foreign|metal)\\s+(pouch|case|bag)",
    "(read|see) (detail|desc|post)", "please read", "(docking|charging) (station|stand)", "download",
    "credits", "instant delivery", "official server", "damaged", "Option File",
    "coin", "skins", "collectors box", "dutch import", "(german|promo|demo) (version|copy)", "soundtrack", "poster",
    "arabic", "(no|protective|case|silicone|phone|style) cover(s)?",
    "sniper thumbs", "(game|skin) grip", "thumb( )?stick", "grip (protector|combat|stick)", "leg strap", "Cleaning Cloth",
    "dual( )?shock", "efigs", "gamepad", "(toy|joy|ring)(\\s+)?con", "controller", "stand holder", "memory card", "SpaBag",
    "headset", "\\bhdmi\\b", "\\busb\\b", "\\bhdd\\b", "nintendo labo",
    "player generator", "100k", "50k", "\\bDL( )?C\\b", "pve", "starter pack", "fifa points", "million bells", "k eso",
    "pre(\\s+|-)?order", "season pass", "(steel|art)( )?book", "ring fit", "lego dimensions", "minifigure", "figure(s)? bundle", "collectable", "collectible",
    "family membership", "subscription card", "12 month", "dynamic theme", "themes", "account", "level boosting", "gamer score",
    "samsung", "huawei", "iphone",
    "\\bhori\\b", "\\bDE\\b", "ID59z",
    "ark survival(\\s+(\\w+|\\d+)){5,}",
    "diablo 3(\\s+(\\w+|\\d+)){6,}", "fortnite",
    "animal crossing (?s).* (diy|recipe|fossil|dino|egg)", "gta (?s).* (online|million)",
    "fallout 76(\\s+(\\w+|\\d+)){5,}", "((\\w+|\\d+)\\s+){3,}fallout 76", "fallout (?s).* (50|100|steel|leed|stimpack|power|cap|armo)",
    "fifa 20(\\s+(\\w+|\\d+)){5,}", "fifa (?s).* (gold|point)",
    "borderlands 3(\\s+(\\w+|\\d+)){5,}",
    "((\\w+|\\d+)\\s+){1,}rocket league(\\s+(\\w+|\\d+)){2,}", "((\\w+|\\d+)\\s+){3,}rocket league", "rocket league(\\s+(\\w+|\\d+)){5,}",
    "((\\w+|\\d+)\\s+){3,}swordshield", "((\\w+|\\d+)\\s+){3,}pokemon", "pokemon(\\s+(\\w+|\\d+)){6,}"
  ).mkString("^.*?(?i)(", "|", ").*$").r

  override protected val categoryId: Int = 139973
  override protected val searchQueries: List[String] = List("PS4", "XBOX ONE", "SWITCH")

  override protected val newlyListedSearchFilterTemplate: String = DEFAULT_SEARCH_FILTER + "buyingOptions:{FIXED_PRICE},itemStartDate:[%s]"

  override protected def removeUnwanted(itemSummary: EbayItemSummary): Boolean =
    !LISTING_NAME_TRIGGER_WORDS.matches(itemSummary.title.replaceAll("[^a-zA-Z0-9 ]", ""))
}
