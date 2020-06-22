package clients.ebay.mappers

import cats.implicits._
import domain.ItemDetails.GameDetails
import domain.{Packaging, ListingDetails}

private[mappers] object GameDetailsMapper {

  private val LEVEL1_TITLE_WORDS_REPLACEMENTS = List(
    "(?<=.{12})((new|rare|(very )?good) )?(\\b(for( the)?|((only|playable) )?on)\\b )?((sony )?play( )?station|(?<!(Playstation(?s).*))ps\\d|(microsoft )?xbox (1|one|360)|nintendo switch|(nintendo )?\\bwii( u)?\\b)(?s).*",
    "(gold )?((greatest|playstation) )?\\bhits\\b",
    "day (one|1|zero|0)( (edition|\\be(d)?(i)?(t)?(i)?\\b))?(?s).*$",
    "(\\bHD\b|exclusive|special|limited collectors|definitive|atlas|platinum|complete|standard|std|classic(s)?|(\\d+(th)?)? anniversary|remastered|elite|\\beu\\b|coll(ector(s)?)?|ltd|goty|(action )?game of the|legacy( pro)?|unlimited|premium|(digital )?deluxe|ultimat)(?s).* (collection|edition|\\be(d)?(i)?(t)?(i)?\\b)(?s).*$",
    "(the )?((new|pristine|inc)\\s+)?(super|cheap( )?)?(free|fast|quick)?(\\s+)?(and )?(super( )?)?(prompt|free|fast|quick|(next|same) day|tracked|speedy|worldwide)(?s).* (dispatch|ship(ping)?|post(age)?|delivery|p(\\s+)?p).*$",
    "(?<=.{12})((brand\\s+)?new(?s).*)?((factory |un)?sealed|unopened|shrinkwrapped)(?s).*$",
    "(?<=.{20})\\b(single player|Family Fun|((kids|fun) )?adventure|console single|tactical|3rd-person|rpg|fps|survival|action|racing|role|wrestling|fighting)\\b(?s).* game(?s).*"
  ).mkString("(?i)", "|", "")

  private val LEVEL2_TITLE_WORDS_REPLACEMENTS = List(
    "((new|rare) )?((very )?good )?(\\b(for|((only|playable) )?on)\\b )?(sony |microsoft )?(play( )?st(a)?(t)?(i)?(o)?(n)?(( )?\\d)?|x( )?box(( )?(one|\\d+))?|ps\\d|\\bxb( )?(o(ne)?|\\d+)?\\b|(nintendo )?(switch|\\bwii( u)?\\b))( (edition|version))?(\\s+new)?( 20\\d\\d)?",
    "for (the )?playstation(\\s+)?vr", "((ps( )?)?(vr|move)|kinect) (required|compatible)",
    "(dbl|double|triple|twin|expansion) (pack|pk)",
    "(1st|2nd|first) class.*$", "(boxed|complete) (with|case)(?s).*$", "exclusive to(?s).*$", "((comes )?with|no|missing|plus|inc(ludes|luding)?) (booklet|original|instructions|box|map|case|manual)(?s).*$", "(the )?disc(s)? (are|is|in)(?s).*$",
    "(new )?(((very|super) )?rare|limited run|(\\d+ )?new|pal|physical|great|boxed|full|complete|boxed( and)?\\s+complete) game(s)?( \\d+)?( new)?",
    "(in )?(great|(very )?good|incredible|ex(cellent)?|amazing|mint|superb|working|perfect|used|(fully )?tested|immaculate|fantastic) (working|good|(working )?order|cond(ition)?|value|prices)",
    "Warner Bros", "ubisoft", "currys", "Take( |-)?(Two|2)( Interactive)?", "(EA|2k) (dice|music|sports|games)", "James Camerons", "\\bTom clancy(s)?\\b",
    "Bethesda(s)?( Softworks)?", "Hideo Kojima", "(bandai )?namco", "rockstar games", "James Bond", "Activision", "Peter Jacksons", "Naughty Dog",
    "Microsoft( 20\\d\\d)?", "sony", "electronic arts", "nintendo", "square enix", "Dreamworks", "Disney Pixar", "WB Games", "Bend Studio",
    "[^\\p{L}\\p{N}\\p{P}\\p{Z}]",
    "\\d{6,}(\\w+)?"
  ).mkString("(?i)", "|", "")

  private val LEVEL3_TITLE_WORDS_REPLACEMENTS = List(
    "Strategy Combat", "(First Person|FPS) Shooter", "(american|soccer) football( 20\\d\\d)?", "golf sports",
    "(the )?((action|official|console|gold|kids|children)(?s).*)?(video( )?)?game(s)?( (good|boxed|console|of the year))?( 20\\d\\d)?", "nuevo",
    "\\bpegi( \\d+)?\\b(?s).*$", "(\\d+th|(20|ten) year) (anniversary|celebration)", "(2 )?disc(s)?( mint)?", "platinum", "brand new", "\\bID\\d+\\w", "18\\s+years",
    "limited run( \\d+)?", "box( )?set", "pre(-| )?(owned|enjoyed)", "compatible", "physical copy", "steel( )?box", "no scratches", "instructions included",
    "((barely|condition|never) )?used(( very)? good)?( (game|condition))?", "very good", "reorderable", "sent same day", "in stock( now)?", "pre(\\s+)?release", "played once", "best price", "Special Reserve",
    "Expertly Refurbished Product", "(quality|value) guaranteed", "eBay Seller", "fully (working|tested)", "from eBays biggest seller", "Order By 4pm", "Ultimate Fighting Championship",
    "remaster(ed)?", "directors cut", "\\bctr\\b", "original", "english", "deluxe", "standard", "\\bgoty\\b", "multi(-| )?lang(uage)?( in game)?", "freepost",
    "blu-ray", "bonus level", "Console Exclusive", "playable on", "Definitive Experience", "Highly Rated", "official$", "essentials", "classic(s)?", "boxed( and)? complete",
    "(\\bUK\\b|\\bEU\\b|genuine|european)(( |-)(new|only|seller|version|stock|import))?", "For age(s)? \\d+(\\+)?", "must see", "see pics",
    "\\bpal\\b(\\s+\\d+)?( version)?", "\\ben\\b", "\\bcr\\b", "\\bnc\\b", "\\bfr\\b", "\\bes\\b", "\\bvg(c| condition)?\\b", "\\ban\\b", "\\bLTD\\b", "\\b\\w+VG\\b",
    "\\bns\\b", "\\bnsw\\b", "\\bsft\\b", "\\bsave s\\b", "\\bdmc\\b", "\\bBNI(B|P)\\b", "\\bNSO\\b", "\\bNM\\b", "\\bLRG\\b", "\\bUE\b",
    "\\bremake\\b", "(ultra )?\\b(u)?hd\\b", "\\b4k\\b", "\\buns\\b", "\\bx360\\b", "\\bstd\\b", "\\bpsh\\b", "\\bAMP\\b", "\\bRPG\\b", "\\bBBFC\\b", "\\bPG(13)?\\b",
    "\\bDVD\\b", "\\bAND\\b", "\\bNTSC\\b", "\\bWi1\\b", "\\bMarvels\\b",
    "\\bMarvels\\b", "NEW$", "very rare",
    "complete", "very rare"
  ).mkString("(?i)", "|", "")

  private val EDGE_WORDS_REPLACEMENTS = List(
    "^(\\s)?(((brand )?NEW|BNIB)\\s+)?(and )?SEALED",
    "^\\bMARVEL\\b", "^SALE", "NEW$", "^BOXED", "^SALE", "^NEW", "^best", "^software", "un( |-)?opened$", "rare$", "^rare",
    "^bargain","bargain$", "mint$", "\\bfor\\b$", "premium$", "very$", "\\bLIMITED\\b$", "tested$", "\\bON\\b$", "\\bBY\\b$",
    "boxed$"
  ).mkString("(?i)", "|", "")

  private val PLATFORMS_MATCH_REGEX = List(
    "PS\\d", "PLAYSTATION(\\s+)?(\\d)",
    "NINTENDO SWITCH", "SWITCH", "\\bWII( )?U\\b", "\\bWII\\b",
    "X( )?B(OX)?(\\s+)?(ONE|\\d+)", "X360", "XBOX"
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
    "SONYPLAYSTATION" -> "PS",
    "PLAYSTATION" -> "PS",
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
      .replaceAll(EDGE_WORDS_REPLACEMENTS, "")
      .replaceAll(LEVEL1_TITLE_WORDS_REPLACEMENTS, "")
      .replaceAll(LEVEL2_TITLE_WORDS_REPLACEMENTS, "")
      .replaceAll(LEVEL3_TITLE_WORDS_REPLACEMENTS, "")
      .replaceFirst("(?i)(?<=.{5})(the )?\\w+(?=\\s+(\\be(d)?(i)?(t)?(i)?(o)?(n)?\\b|coll(ection)?)) (\\be(d)?(i)?(t)?(i)?(o)?(n)?\\b|coll(ection)?)(?s).*$", "")
      .replaceAll("é", "e")
      .replaceAll("(?i)playerunknown", "Player Unknown")
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
      .replaceAll("\\s+", " ")
      .trim()
      .replaceAll(EDGE_WORDS_REPLACEMENTS, "")
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
