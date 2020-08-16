package clients.ebay

import clients.ebay.browse.EbayBrowseResponse.EbayItemSummary
import domain.ItemDetails

object params {

  sealed trait EbaySearchParams[D <: ItemDetails] {
    def categoryId: Int
    def searchFilterTemplate: String
    def removeUnwanted: EbayItemSummary => Boolean
  }

  implicit val videoGameSearchParams = new EbaySearchParams[ItemDetails.Game] {

    private val DEFAULT_SEARCH_FILTER = "conditionIds:{1000|1500|2000|2500|3000|4000|5000}," +
      "itemLocationCountry:GB," +
      "deliveryCountry:GB," +
      "price:[0..90]," +
      "priceCurrency:GBP," +
      "itemLocationCountry:GB,"

    private val LISTING_NAME_TRIGGER_WORDS = List(
      "(rune|perk|skill|(e)?xp(erience)?) (stats|points)",
      "(demo|game)( )?(code|disc|key|cart|pass)", "(store|reservation|access|cd|unused|digital|upgrade|test|beta|no)( )?(redeem )?(game|code|key)",
      "(software|cartridge(s)?|cart|game|disk|disc(s)?( \\d)?|cover|box|inlay|sleeve|book|cd|collection|manual) only",
      "(case|variety|accessor(ies|y)|storage|charge|robot) (system|set|kit|box)", "no dis(c|k)", "Season( \\d)? Pass",
      "(canvas|replacement|cover|carry|travel(er)?|commuter|carrying|just( the)?|no|hard|storage|game|vault|phone|card|foreign|metal|protection|protective)\\s+(pouch|case|bag)",
      "(read|see) (detail|desc|post)", "please(?s).*read", "read(?s).*please", "(docking|charging) (station|stand)", "download",
      "credits", "instant delivery", "official server", "damaged", "Option File", "offline", "unlock all", "online", "mini dock",
      "coin", "skins", "collectors box", "soundtrack", "poster", "protection accessor", "PLAYSTAND", "Press Release", "unboxed",
      "(no|protective|case|silicone|phone|style) cover(s)?", "promotional game", "starter pack", "Microphone", "KontrolFreek",
      "sniper thumbs", "(game|skin|thumb|Silicone|floating) grip", "thumb( )?stick", "(screen|grip) (protector|combat|stick)", "leg strap", "Cleaning Cloth",
      "dual( )?(shock|charge)", "efigs", "gamepad", "(toy|joy|ring)(\\s+)?con", "controller", "(card|stand) holder", "memory card", "(Spa|messenger)?( )?Bag", "keyring",
      "headset", "\\bhdmi\\b", "\\bUSB\\b", "\\bhdd\\b", "(nintendo|switch) labo", "(steering|racing|driving|official|nintendo|wii) wheel", "wristband", "horipad", "(cord|power|hdmi)( )?(adapter|level|supply)", "tv tuner",
      "100k", "50k", "\\bDL( )?C\\b", "pve", "starter (set|pack|bundle|kit)", "figure(s)? bundle", "k eso", "(mini|gift) toy", "pad pro", "cable pack", "dvd player",
      "pre(\\s+|-)?(order|sale)", "season pass", "(steel|art)( )?book", "(game|mini)( )?figure", "collectable", "collectible", "remote control", "(aux|charg(ing|e)|power) cable",
      "membership", "(subscription|gift)( |-)?card", "12 month", "(wallpaper|dynamic) theme", "themes", "account", "achievement pack",
      "(xp|level|lvl) boost", "gamer score", "trophy service", "platinum trophy", "arcade mini", "boosting levels",
      "samsung", "huawei", "iphone", "sandisk", "server", "wireless", "Tempered Glass", "Early Access", "beta test",
      "(usa|hungarian|scandinavian|asian|korea(n)?|polish|german|promo(tional)?|starter|demo|french|jap(an)?(ese)?|dutch|italian|spanish|us(a)?|digital|nordic) (release|cover|pack|box|import|item|disc|vers|copy)", "arabic", "slovakian", "czech",
      "\\bhori\\b", "\\bDE\\b", "ID59z", "\\bemail\\b",
      "ark survival(\\s+(\\w+|\\d+)){5,}", "Code(-| )?in(-| )?(a)?(-| )?Box",
      "diablo 3(\\s+(\\w+|\\d+)){6,}", "fortnite",
      "skylander", "lego dimension", "disney infinity", "ring fit",
      "villager(?s).*animal crossing", "animal crossing(?s).* (bell|million|diy|recipe|fossil|dino|egg|gold)",
      "gta(?s).* (money|online|million)",
      "(cod|of duty)(?s).* (boost|unlock|warzone)",
      "fallout 76(\\s+(\\w+|\\d+)){5,}", "((\\w+|\\d+)\\s+){3,}fallout 76",
      "(minigun|mask|fixer|rifle|laser|lvc|blood|hand|lmg|legend|magazin|coat|x5|bear|arm|vamp|uniform|plan|blueprint|suit|outfit|shot|flame|armo|50|100|steel|leed|stimpack|power|cap|armo|recipe|gun)(?s).*fallout",
      "fallout(?s).* (minigun|mask|fixer|rifle|laser|lvc|blood|hand|lmg|legend|magazin|coat|x5|bear|arm|vamp|uniform|plan|blueprint|suit|outfit|shot|flame|armo|50|100|steel|leed|stimpack|power|cap|armo|recipe|gun)",
      "fifa(?s).* (team|money|milli|gener|player|gold|point)",
      "borderlands 3(\\s+(\\w+|\\d+)){5,}", "borderlands(?s).* (mayhem|lvl|cash|x50|legendary|money|mod)",
      "rocket(?s).* (item|bod|car|fire|import|trade|inventor|rare|crate|decal|wheel|goal|explos)", "((\\w+|\\d+)\\s+){1,}rocket league(\\s+(\\w+|\\d+)){2,}", "((\\w+|\\d+)\\s+){3,}rocket league",
      "((\\w+|\\d+)\\s+){3,}swordshield", "((\\w+|\\d+)\\s+){3,}pokemon", "pokemon(\\s+(\\w+|\\d+)){6,}"
    ).mkString("^.*?(?i)(", "|", ").*$").r

    override val categoryId: Int = 139973

    override val searchFilterTemplate: String = DEFAULT_SEARCH_FILTER + "buyingOptions:{FIXED_PRICE},itemStartDate:[%s]"

    override val removeUnwanted: EbayItemSummary => Boolean =
      item => !LISTING_NAME_TRIGGER_WORDS.matches(item.title.replaceAll("[^a-zA-Z0-9 ]", ""))
  }
}