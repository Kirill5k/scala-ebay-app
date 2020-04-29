package clients.ebay.mappers

import cats.implicits._
import domain.ItemDetails.GameDetails
import domain.{Packaging, ListingDetails}

private[mappers] object GameDetailsMapper {

  private val TITLE_WORDS_FILTER = List(
    "(video( )?)?game for( the)?( playstation)?(\\s+(vr|\\d+))?", "for playstation( )?vr", "psvr required",
    "((greatest|playstation) )?\\bhits\\b",
    "(good )?(for )?((sony )?playstation(( )?\\d)?|x( )?box(( )?(one|\\d+))?|ps\\d|\\bxb( )?(o(ne)?|\\d+)?\\b|(nintendo )?switch)(\\s+\\bgame\\b)?(\\s+new)?(\\s+complete)?(\\s+edition)?( 20\\d\\d)?( good)?",
    "(dbl|double|triple|twin) (pack|pk)",
    "day (one|1|zero|0)( (edition|\\be(d)?(i)?(t)?(i)?\\b))?",
    "(1st|2nd) class( signed)?",
    "(eu|coll(ectors)?|ltd|goty|(action )?game of the year|legacy( pro)?|limited|premium( online)?|(digital )?deluxe|standard|ultimat(?s).*) (collection|edition|\\be(d)?(i)?(t)?(i)?\\b)",
    "(new\\s+)?(super( )?)?(free|fast|quick)?(\\s+)?(and )?(super( )?)?(free|fast|quick|(next|same) day|tracked|speedy)(?s).* (dispatch|shipping|post(age)?|delivery|p(\\s+)?p)",
    "((brand )?new(?s).*)?((factory |un)?sealed|unopened|shrinkwrapped)", "(complete )?(brand|game) (new|neuf|nuevo)",
    "(great|(very )?good|incredible|ex(cellent)?|amazing|mint|superb) (condition|value|prices)",
    "(new )?((super )?rare|limited run|(\\d+ )?new|pal|great|boxed|full|complete|boxed( and)?\\s+complete) game(s)?( \\d+)?",
    "limited run( \\d+)?", "box( )?set", "pre(-| )?owned", "compatible", "inc manual",
    "Used", "very good", "reorderable", "sent same day", "in stock( now)?", "pre(\\s+)?release", "played once", "best price",
    "Expertly Refurbished Product", "(quality|value) guaranteed", "Highly Rated eBay Seller", "fully tested", "from eBays biggest seller", "Order By 4pm",
    "remaster(ed)?", "directors cut", "\\bctr\\b", "original", "english", "deluxe", "standard", "\\bgoty\\b", "(action )?game of the( year)?", "multi-language",
    "Warner Bros", "ubisoft", "currys", "blu-ray", "bonus level",
    "Microsoft","sony", "electronic arts", "nintendo", "square enix", "ea sport(s)?", "(bandai )?namco", "no scratches",
    "\\bTom clancy(s)?\\b", "(\\bUS?\\b|\\bUK\\b|\\bEU\\b|genuine)(( |-)(new|only|seller|version|stock|import))?",
    "\\bRPG\\b( game)?", "role( )?playng( game)?", "Adventure Role( playing)?", "Strategy (combat|Management)",
    "tactical shooter", "action (adventure|fighting)( game)?", "fps( survival)? shooter( game)?", "fighting multi( |-)?player( game)?",
    "\\bpal\\b", "\\ben\\b", "\\bcr\\b", "\\bnc\\b", "\\bfr\\b", "\\bes\\b", "\\bvgc\\b", "\\ban\\b", "\\bpegi( \\d+)?\\b", "\\bLTD\\b",
    "\\bns\\b", "\\bvr\\b( (compatible|required))?", "\\bnsw\\b", "\\bsft\\b", "\\bsave s\\b", "\\bdmc\\b", "\\bBNIB\\b", "\\bNSO\\b", "\\bNM\\b",
    "\\bremake\\b", "\\bhd\\b", "\\b4k\\b", "\\buns\\b", "\\bx360\\b",
    "(the )?(official )?video(\\s+)?game(s)?( fasting)?",
    "^\\bMARVEL\\b", "^SALE", "NEW$", "^SEALED", "^NEW", "\\bMarvels\\b", "^best", "^software", "very rare",
    "[^\\p{L}\\p{N}\\p{P}\\p{Z}]",
    "\\d{6,}"
  ).mkString("(?i)", "|", "")

  private val PLATFORMS_MATCH_REGEX = List(
    "PS4", "PLAYSTATION(\\s+)?(\\d)",
    "NINTENDO SWITCH", "SWITCH",
    "XB(OX)?(\\s+)?(ONE|\\d+)", "XBOX 1", "XB1", "XBONE", "X BOX ONE", "XBOX 360"
  ).mkString("(?i)", "|", "").r

  private val BUNDLE_MATCH_REGEX = List(
    "(new|multiple|PS4|xbox one) games", "bundle", "job(\\s+)?lot"
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
    "XBOXONE" -> "XBOX ONE",
    "XBONE" -> "XBOX ONE",
    "MICROSOFTXBOXONE" -> "XBOX ONE",
    "MICROSOFTXBOX360" -> "XBOX 360",
    "MICROSOFTXBOX" -> "XBOX",
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
      .replaceAll(TITLE_WORDS_FILTER, "")
      .replaceFirst("(?i)\\w+(?=\\s+(\\be(d)?(i)?(t)?(i)?(o)?(n)?\\b|coll(ection)?)) (\\be(d)?(i)?(t)?(i)?(o)?(n)?\\b|coll(ection)?)", "")
      .replaceAll("é", "e")
      .replaceAll("(?i)(playerunknown)", "Player Unknown")
      .replaceAll("(?i)(littlebigplanet)", "Little Big Planet")
      .replaceAll("(?i)(farcry)", "Far Cry")
      .replaceAll("(?i)(superheroes)", "Super Heroes")
      .replaceAll("(?i)(W2K)", "WWE 2k")
      .replaceAll("(?i)(NierAutomata)", "Nier Automata")
      .replaceAll("(?i)(fifa 2020)", "FIFA 20")
      .replaceAll("(?i)(witcher iii)", "witcher 3")
      .replaceAll("(?i)(\\bnsane\\b)", "N Sane")
      .replaceAll(" +", " ")
      .replaceAll("(?i)( -|- | –|– )", " ")
      .replaceFirst("(?i)(^-)", "")
      .trim()
      .some

  private def mapPlatform(listingDetails: ListingDetails): Option[String] = {
    PLATFORMS_MATCH_REGEX.findFirstIn(listingDetails.title.withoutSpecialChars)
      .orElse(listingDetails.properties.get("Platform").map(_.split(",|/")(0)))
      .map(_.toUpperCase.trim)
      .map(_.replaceAll(" ", ""))
      .map(platform => PLATFORM_MAPPINGS.getOrElse(platform, platform))
  }

  private def mapGenre(listingDetails: ListingDetails): Option[String] = {
    listingDetails.properties.get("Genre").orElse(listingDetails.properties.get("Sub-Genre"))
  }

  implicit class StringOps(private val str: String) extends AnyVal {
    def withoutSpecialChars: String = str.replaceAll("[?_;`—“”!•£&#,’'*()|:.\\[\\]]", "").replaceAll("/", " ")
  }
}
