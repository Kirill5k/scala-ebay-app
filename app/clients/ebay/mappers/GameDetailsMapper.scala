package clients.ebay.mappers

import cats.implicits._
import domain.ItemDetails.GameDetails
import domain.{Packaging, ListingDetails}

private[mappers] object GameDetailsMapper {

  private val PRIMARY_TITLE_WORDS_REPLACEMENTS = List(
    "(?<=.{12})(new )?((sony )?playstation|ps\\d|(microsoft )?xbox (1|one|360)|nintendo switch|(nintendo )?\\bwii( u)?\\b)(?s).*",
    "((very )?good )?(for )?(sony |microsoft )?(playst(a)?(t)?(i)?(o)?(n)?(( )?\\d)?|x( )?box(( )?(one|\\d+))?|ps\\d|\\bxb( )?(o(ne)?|\\d+)?\\b|(nintendo )?(switch|\\bwii( u)?\\b))( edition)?(\\s+new)?( 20\\d\\d)?",
    "for (the )?playstation(\\s+)?vr", "(psvr|kinect) required",
    "(gold )?((greatest|playstation) )?\\bhits\\b",
    "day (one|1|zero|0)( (edition|\\be(d)?(i)?(t)?(i)?\\b))?(?s).*$",
    "(classic(s)?|(\\d+(th)?)? anniversary|remastered|elite|\\beu\\b|coll(ector(s)?)?|ltd|goty|(action )?game of the|legacy( pro)?|(un)?limited|premium|(digital )?deluxe|standard|ultimat)(?s).* (collection|edition|\\be(d)?(i)?(t)?(i)?\\b)(?s).*$",
    "(dbl|double|triple|twin|expansion) (pack|pk)",
    "(the )?(new\\s+)?(super|cheap( )?)?(free|fast|quick)?(\\s+)?(and )?(super( )?)?(prompt|free|fast|quick|(next|same) day|tracked|speedy)(?s).* (dispatch|shipping|post(age)?|delivery|p(\\s+)?p).*$",
    "(1st|2nd|first) class.*$", "(boxed|complete) (with|case)(?s).*$", "exclusive to(?s).*$", "(with|no|missing) (map|case|manual)(?s).*$", "(the )?disc(s)? (are|is|in)(?s).*$",
    "((brand )?new(?s).*)?((factory |un)?sealed|unopened|shrinkwrapped)(?s).*$",
    "(new )?((super )?rare|limited run|(\\d+ )?new|pal|physical|great|boxed|full|complete|boxed( and)?\\s+complete) game(s)?( \\d+)?( new)?",
    "(in )?(great|(very )?good|incredible|ex(cellent)?|amazing|mint|superb|working|perfect|used) (good|working order|condition|value|prices)",
    "(single player|adventure|console single|tactical|3rd-person|rpg|fps|survival|action|role|fighting)(?s).* game(?s).*",
    "[^\\p{L}\\p{N}\\p{P}\\p{Z}]",
    "\\d{6,}"
  ).mkString("(?i)", "|", "")

  private val SECONDARY_TITLE_WORDS_REPLACEMENTS = List(
    "(the )?((action|official|console|gold) )?(video( )?)?game(s)?( (of the year|for( the)?))?",
    "(complete )?(brand|game) (new|neuf|nuevo)", "\\bpegi( \\d+)?\\b(?s).*$", "\\d+th anniversary", "disc (mint|vgc)",
    "limited run( \\d+)?", "box( )?set", "pre(-| )?(owned|enjoyed)", "compatible", "inc manual", "physical copy", "steel( )?box",
    "used( good)?( game)?", "very good", "reorderable", "sent same day", "in stock( now)?", "pre(\\s+)?release", "played once", "best price",
    "Expertly Refurbished Product", "(quality|value) guaranteed", "eBay Seller", "fully (working|tested)", "from eBays biggest seller", "Order By 4pm",
    "remaster(ed)?", "directors cut", "\\bctr\\b", "original", "english", "deluxe", "standard", "\\bgoty\\b", "multi(-| )?lang(uage)?( in game)?",
    "Warner Bros", "ubisoft", "currys", "blu-ray", "bonus level", "Console Exclusive", "playable on", "Definitive Experience",
    "Microsoft","sony", "electronic arts", "nintendo", "square enix", "ea sport(s)?", "(bandai )?namco", "no scratches",
    "Take( |-)?Two( Interactive)?", "2k games", "Bethesda( Softworks)?", "Hideo Kojima", "Highly Rated", "James Camerons",
    "\\bTom clancy(s)?\\b", "(\\bUK\\b|\\bEU\\b|genuine|european)(( |-)(new|only|seller|version|stock|import))?",
    "\\bpal\\b( \\d+)", "\\ben\\b", "\\bcr\\b", "\\bnc\\b", "\\bfr\\b", "\\bes\\b", "\\bvg(c)?\\b", "\\ban\\b", "\\bLTD\\b", "\\b\\w+VG\\b",
    "\\bns\\b", "\\bvr\\b( (compatible|required))?", "\\bnsw\\b", "\\bsft\\b", "\\bsave s\\b", "\\bdmc\\b", "\\bBNIB\\b", "\\bNSO\\b", "\\bNM\\b", "\\bLRG\\b",
    "\\bremake\\b", "\\bhd\\b", "\\b4k\\b", "\\buns\\b", "\\bx360\\b", "\\bstd\\b", "\\bpsh\\b", "\\bAMP\\b", "\\bRPG\\b",
    "official$", "esssentials", "classic(s)?", "boxed complete",
    "\\bMarvels\\b", "^\\bMARVEL\\b", "^SALE", "NEW$", "^BOXED", "^SALE", "^SEALED", "^NEW", "^best", "^software", "very rare", "rare$", "bargain$", "mint$",
  ).mkString("(?i)", "|", "")

  private val PLATFORMS_MATCH_REGEX = List(
    "PS\\d", "PLAYSTATION(\\s+)?(\\d)",
    "NINTENDO SWITCH", "SWITCH", "\\bWII U\\b", "\\bWII\\b",
    "XB(OX)?(\\s+)?(ONE|\\d+)"
  ).mkString("(?i)", "|", "").r

  private val BUNDLE_MATCH_REGEX = List(
    "(new|multiple|PS4|PS3|xbox one|switch|wii( u)?) games", "bundle", "job(\\s+)?lot"
  ).mkString("(?i)", "|", "").r

  private val PLATFORM_MAPPINGS: Map[String, String] = Map(
    "SONYPLAYSTATION4" -> "PS4",
    "PLAYSTATION4" -> "PS4",
    "SONYPLAYSTATION3" -> "PS3",
    "PLAYSTATION3" -> "PS3",
    "SONYPLAYSTATION2" -> "PS2",
    "PLAYSTATION2" -> "PS2",
    "SONYPLAYSTATION1" -> "PS1",
    "SONYPLAYSTATION" -> "PS4",
    "NINTENDOSWITCH" -> "SWITCH",
    "XBOX1" -> "XBOX ONE",
    "XBOX360" -> "XBOX 360",
    "XB1" -> "XBOX ONE",
    "XB360" -> "XBOX 360",
    "XBOXONE" -> "XBOX ONE",
    "XBONE" -> "XBOX ONE",
    "MICROSOFTXBOXONE" -> "XBOX ONE",
    "MICROSOFTXBOX360" -> "XBOX 360",
    "MICROSOFTXBOX" -> "XBOX",
    "XBOX" -> "XBOX",
    "WIIU" -> "WII U",
    "WII" -> "WII"
  )

  def from(listingDetails: ListingDetails): GameDetails = {
    val isBundle = BUNDLE_MATCH_REGEX.findFirstIn(listingDetails.title.withoutSpecialChars).isDefined
    GameDetails(
      name = if (isBundle) sanitizeTitle(listingDetails.title) else sanitizeTitle(listingDetails.properties.getOrElse("Game Name", listingDetails.title)),
      platform = mapPlatform(listingDetails),
      genre = mapGenre(listingDetails),
      releaseYear = listingDetails.properties.get("Release Year"),
      packaging = if (isBundle) Packaging.Bundle else Packaging.Single
    )
  }

  private def sanitizeTitle(title: String): Option[String] =
    title
      .withoutSpecialChars
      .replaceAll(PRIMARY_TITLE_WORDS_REPLACEMENTS, "")
      .replaceAll(SECONDARY_TITLE_WORDS_REPLACEMENTS, "")
      .replaceFirst("(?i)\\w+(?=\\s+(\\be(d)?(i)?(t)?(i)?(o)?(n)?\\b|coll(ection)?)) (\\be(d)?(i)?(t)?(i)?(o)?(n)?\\b|coll(ection)?)(?s).*$", "")
      .replaceAll("é", "e")
      .replaceAll("(?i)(playerunknown)", "Player Unknown")
      .replaceAll("(?i)(littlebigplanet)", "Little Big Planet")
      .replaceAll("(?i)(farcry)", "Far Cry")
      .replaceAll("(?i)(superheroes)", "Super Heroes")
      .replaceAll("(?i)(W2K)", "WWE 2k")
      .replaceAll("(?i)(NierAutomata)", "Nier Automata")
      .replaceAll("(?i)(fifa 2020)", "FIFA 20")
      .replaceAll("(?i)(witcher iii)", "witcher 3")
      .replaceAll("(?i)(diablo 3)", "diablo iii")
      .replaceAll("(?i)(\\bnsane\\b)", "N Sane")
      .replaceAll("(?i)(\\bww2|ww11\\b)", "wwii")
      .replaceAll("(?i)(\\bcod\\b)", "Call of Duty")
      .replaceAll("(?i)(\\bgta\\b)", "Grand Theft Auto ")
      .replaceAll("(?i)(\\bIIII\\b)", "4")
      .replaceAll("(?i)((the|\\ba\\b)? Telltale( game)?( series)?)", " Telltale")
      .replaceAll("-|:", " ")
      .replaceAll(" +", " ")
      .trim()
      .some

  private def mapPlatform(listingDetails: ListingDetails): Option[String] = {
    PLATFORMS_MATCH_REGEX.findFirstIn(listingDetails.title.withoutSpecialChars)
      .orElse(listingDetails.properties.get("Platform").map(_.split(",|/")(0)))
      .map(_.toUpperCase.trim)
      .map(_.replaceAll(" |-", ""))
      .map(platform => PLATFORM_MAPPINGS.getOrElse(platform, platform))
  }

  private def mapGenre(listingDetails: ListingDetails): Option[String] = {
    listingDetails.properties.get("Genre").orElse(listingDetails.properties.get("Sub-Genre"))
  }

  implicit class StringOps(private val str: String) extends AnyVal {
    def withoutSpecialChars: String = str.replaceAll("[?_;`—–“”!•£&#,’'*()|.\\[\\]]", "").replaceAll("/", " ")
  }
}
