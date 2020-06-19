package clients.ebay.mappers

import cats.implicits._
import domain.ItemDetails.GameDetails
import domain.{Packaging, ListingDetails}

private[mappers] object GameDetailsMapper {

  private val PRIMARY_TITLE_WORDS_REPLACEMENTS = List(
    "^((NEW|BNIB)\\s+)?(and )?SEALED",
    "(?<=.{12})(new )?(\\b(for( the)?|(playable )?on)\\b )?((sony )?playstation|(?<!(Playstation(?s).*))ps\\d|(microsoft )?xbox (1|one|360)|nintendo switch|(nintendo )?\\bwii( u)?\\b)(?s).*",
    "((very )?good )?(\\b(for|(playable )?on)\\b )?(sony |microsoft )?(playst(a)?(t)?(i)?(o)?(n)?(( )?\\d)?|x( )?box(( )?(one|\\d+))?|ps\\d|\\bxb( )?(o(ne)?|\\d+)?\\b|(nintendo )?(switch|\\bwii( u)?\\b))( edition)?(\\s+new)?( 20\\d\\d)?",
    "for (the )?playstation(\\s+)?vr", "((ps( )?)?(vr|move)|kinect) (required|compatible)",
    "(gold )?((greatest|playstation) )?\\bhits\\b",
    "day (one|1|zero|0)( (edition|\\be(d)?(i)?(t)?(i)?\\b))?(?s).*$",
    "(game|atlas|platinum|complete|standard|std|classic(s)?|(\\d+(th)?)? anniversary|remastered|elite|\\beu\\b|coll(ector(s)?)?|ltd|goty|(action )?game of the|legacy( pro)?|(un)?limited|premium|(digital )?deluxe|standard|ultimat)(?s).* (collection|edition|\\be(d)?(i)?(t)?(i)?\\b)(?s).*$",
    "(dbl|double|triple|twin|expansion) (pack|pk)",
    "(the )?((new|pristine)\\s+)?(super|cheap( )?)?(free|fast|quick)?(\\s+)?(and )?(super( )?)?(prompt|free|fast|quick|(next|same) day|tracked|speedy)(?s).* (dispatch|shipping|post(age)?|delivery|p(\\s+)?p).*$",
    "(1st|2nd|first) class.*$", "(boxed|complete) (with|case)(?s).*$", "exclusive to(?s).*$", "((comes )?with|no|missing) (box|map|case|manual)(?s).*$", "(the )?disc(s)? (are|is|in)(?s).*$",
    "(?<=.{12})((brand\\s+)?new(?s).*)?((factory |un)?sealed|unopened|shrinkwrapped)(?s).*$",
    "(new )?((super )?rare|limited run|(\\d+ )?new|pal|physical|great|boxed|full|complete|boxed( and)?\\s+complete) game(s)?( \\d+)?( new)?",
    "(in )?(great|(very )?good|incredible|ex(cellent)?|amazing|mint|superb|working|perfect|used|tested) (good|(working )?order|condition|value|prices)",
    "(?<=.{20})\\b(single player|adventure|console single|tactical|3rd-person|rpg|fps|survival|action|racing|role|(wrestling )?fighting)\\b(?s).* game(?s).*",
    "Warner Bros", "ubisoft", "currys", "Take( |-)?(Two|2)( Interactive)?", "(EA|2k) (sports|games)", "James Camerons", "\\bTom clancy(s)?\\b",
    "Bethesda(s)?( Softworks)?", "Hideo Kojima", "(bandai )?namco", "rockstar games", "James Bond", "Activision", "Peter Jacksons", "Naughty Dog",
    "Microsoft", "sony", "electronic arts", "nintendo", "square enix", "Dreamworks", "Disney Pixar", "WB Games", "Bend Studio",
    "[^\\p{L}\\p{N}\\p{P}\\p{Z}]",
    "\\d{6,}\\w+"
  ).mkString("(?i)", "|", "")

  private val SECONDARY_TITLE_WORDS_REPLACEMENTS = List(
    "Strategy Combat",
    "(the )?((action|official|console|gold|kids)(?s).*)?(video( )?)?game(s)?( (console|of the year|for( the)?))?", "nuevo",
    "\\bpegi( \\d+)?\\b(?s).*$", "\\d+th anniversary", "disc( mint)?", "platinum", "brand new", "\\bID\\d+\\w", "18\\s+years",
    "limited run( \\d+)?", "box( )?set", "pre(-| )?(owned|enjoyed)", "compatible", "inc manual", "physical copy", "steel( )?box", "no scratches",
    "used( good)?( game)?", "very good", "reorderable", "sent same day", "in stock( now)?", "pre(\\s+)?release", "played once", "best price", "Special Reserve",
    "Expertly Refurbished Product", "(quality|value) guaranteed", "eBay Seller", "fully (working|tested)", "from eBays biggest seller", "Order By 4pm",
    "remaster(ed)?", "directors cut", "\\bctr\\b", "original", "english", "deluxe", "standard", "\\bgoty\\b", "multi(-| )?lang(uage)?( in game)?", "freepost",
    "blu-ray", "bonus level", "Console Exclusive", "playable on", "Definitive Experience", "Highly Rated", "official$", "essentials", "classic(s)?", "boxed complete",
    "(\\bUK\\b|\\bEU\\b|genuine|european)(( |-)(new|only|seller|version|stock|import))?", "For age(s)? \\d+(\\+)?", "complete", "must see", "\\b1\\b",
    "\\bpal\\b(\\s+\\d+)?( version)?", "\\ben\\b", "\\bcr\\b", "\\bnc\\b", "\\bfr\\b", "\\bes\\b", "\\bvg(c| condition)?\\b", "\\ban\\b", "\\bLTD\\b", "\\b\\w+VG\\b",
    "\\bns\\b", "\\bvr\\b( (compatible|required))?", "\\bnsw\\b", "\\bsft\\b", "\\bsave s\\b", "\\bdmc\\b", "\\bBNIB\\b", "\\bNSO\\b", "\\bNM\\b", "\\bLRG\\b", "\\bUE\b",
    "\\bremake\\b", "(ultra )?\\bhd\\b", "\\b4k\\b", "\\buns\\b", "\\bx360\\b", "\\bstd\\b", "\\bpsh\\b", "\\bAMP\\b", "\\bRPG\\b", "\\bBBFC\\b", "\\bPG(13)?\\b",
    "\\bMarvels\\b", "^\\bMARVEL\\b", "^SALE", "NEW$", "^BOXED", "^SALE", "^NEW", "^best", "^software", "^unopened", "very rare", "rare$", "bargain$", "mint$"
  ).mkString("(?i)", "|", "")

  private val PLATFORMS_MATCH_REGEX = List(
    "PS\\d", "PLAYSTATION(\\s+)?(\\d)",
    "NINTENDO SWITCH", "SWITCH", "\\bWII U\\b", "\\bWII\\b",
    "X( )?B(OX)?(\\s+)?(ONE|\\d+)", "X360"
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
    "X360" -> "XBOX 360",
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
      name = sanitizeTitle(listingDetails.title),
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
      .replaceAll("(?i)(Hello Neighbour)", "Hello Neighbor")
      .replaceAll("(?i)(fifa 2020)", "FIFA 20")
      .replaceAll("(?i)(witcher iii)", "witcher 3")
      .replaceAll("(?i)(diablo 3)", "diablo iii")
      .replaceAll("(?i)(\\bnsane\\b)", "N Sane")
      .replaceAll("(?i)(\\bww2|ww11\\b)", "wwii")
      .replaceAll("(?i)(\\bcod\\b)", "Call of Duty ")
      .replaceAll("(?i)(\\bgta\\b)", "Grand Theft Auto ")
      .replaceAll("(?i)(\\bMGS\\b)", "Metal Gear Solid ")
      .replaceAll("(?i)(\\bRainbow 6\\b)", "Rainbow Six ")
      .replaceAll("(?i)(\\bIIII\\b)", "4")
      .replaceAll("(?i)(\\bGW\\b)", "Garden Warfare ")
      .replaceAll("(?i)(\\bGW2\\b)", "Garden Warfare 2")
      .replaceAll("(?i)((the|\\ba\\b)? Telltale(\\s+series)?)", " Telltale")
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
      .map(_.trim)
  }

  private def mapGenre(listingDetails: ListingDetails): Option[String] = {
    listingDetails.properties.get("Genre").orElse(listingDetails.properties.get("Sub-Genre"))
  }

  implicit class StringOps(private val str: String) extends AnyVal {
    def withoutSpecialChars: String = str.replaceAll("[@~+\"{}?_;`—–“”!•£&#,’'*()|.\\[\\]]", "").replaceAll("/", " ")
  }
}
