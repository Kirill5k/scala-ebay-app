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
    "(demo|game)( )?(code|disc|key|cart|pass)", "(cd|unused|digital|upgrade|no)( )?(redeem )?(game|code|key)",
    "(software|cartridge(s)?|cart|game|disc(s)?|cover|box|sleeve|book) only",
    "(case|variety|accessor(ies|y)|storage|charge|robot) (kit|box)",
    "(replacement|cover|carry|travel|carrying|just the|no|hard|storage|game|vault|phone|card|foreign|metal|protective)\\s+(pouch|case|bag)",
    "(read|see) (detail|desc|post)", "please(?s).*read", "read(?s).*please", "(docking|charging) (station|stand)", "download",
    "credits", "instant delivery", "official server", "damaged", "Option File", "offline", "unlock all",
    "coin", "skins", "collectors box", "(german|promo|demo|french|japan(ese)?|dutch) (import|item|disc|vers|copy)", "soundtrack", "poster",
    "arabic", "(no|protective|case|silicone|phone|style) cover(s)?", "promotional game",
    "sniper thumbs", "(game|skin) grip", "thumb( )?stick", "(screen|grip) (protector|combat|stick)", "leg strap", "Cleaning Cloth",
    "dual( )?shock", "efigs", "gamepad", "(toy|joy|ring)(\\s+)?con", "controller", "stand holder", "memory card", "SpaBag",
    "headset", "\\bhdmi\\b", "\\busb\\b", "\\bhdd\\b", "(nintendo|switch) labo", "steering wheel", "wristband", "horipad",
    "100k", "50k", "\\bDL( )?C\\b", "pve", "starter pack", "million bells", "k eso", "gift toy",
    "pre(\\s+|-)?(order|sale)", "season pass", "(steel|art)( )?book", "ring fit", "lego dimension", "minifigure", "figure(s)? bundle", "collectable", "collectible",
    "membership", "subscription card", "12 month", "dynamic theme", "themes", "account",
    "level boosting", "gamer score", "power( )?level", "trophy service", "platinum trophy",
    "samsung", "huawei", "iphone",
    "\\bhori\\b", "\\bDE\\b", "ID59z", "\\bemail\\b",
    "ark survival(\\s+(\\w+|\\d+)){5,}",
    "diablo 3(\\s+(\\w+|\\d+)){6,}", "fortnite",
    "villager(?s).*animal crossing", "animal crossing(?s).* (diy|recipe|fossil|dino|egg|gold)",
    "gta(?s).* (money|online|million)",
    "fallout 76(\\s+(\\w+|\\d+)){5,}", "((\\w+|\\d+)\\s+){3,}fallout 76", "fallout(?s).* (plan|50|100|steel|leed|stimpack|power|cap|armo)",
    "fifa(?s).* (money|milli|gener|player|gold|point)",
    "borderlands 3(\\s+(\\w+|\\d+)){5,}",
    "rocket(?s).* (decal|wheel|goal|explos)", "((\\w+|\\d+)\\s+){1,}rocket league(\\s+(\\w+|\\d+)){2,}", "((\\w+|\\d+)\\s+){3,}rocket league",
    "((\\w+|\\d+)\\s+){3,}swordshield", "((\\w+|\\d+)\\s+){3,}pokemon", "pokemon(\\s+(\\w+|\\d+)){6,}"
  ).mkString("^.*?(?i)(", "|", ").*$").r

  override protected val categoryId: Int = 139973
  override protected val searchQueries: List[String] = List("PS4", "XBOX ONE", "SWITCH")

  override protected val newlyListedSearchFilterTemplate: String = DEFAULT_SEARCH_FILTER + "buyingOptions:{FIXED_PRICE},itemStartDate:[%s]"

  override protected def removeUnwanted(itemSummary: EbayItemSummary): Boolean =
    !LISTING_NAME_TRIGGER_WORDS.matches(itemSummary.title.replaceAll("[^a-zA-Z0-9 ]", ""))
}
