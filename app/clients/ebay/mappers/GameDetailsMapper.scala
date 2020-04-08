package clients.ebay.mappers

import cats.implicits._
import domain.ItemDetails.GameDetails
import domain.{Packaging, ListingDetails}

private[mappers] object GameDetailsMapper {

  private val TITLE_WORDS_FILTER = List(
    "(video( )?)?game for( the)?( playstation)?(\\s+(vr|\\d+))?",
    "((greatest|playstation) )?\\bhits\\b",
    "(for )?((sony )?playstation(( )?\\d)?|x( )?box(( )?(one|\\d+))?|ps\\d|\\bxb( )?(o(ne)?|\\d+)?\\b|(nintendo )?switch)(\\s+\\bgame\\b)?(\\s+new)?(\\s+complete)?(\\s+edition)?( 20\\d\\d)?",
    "(dbl|double|triple|twin) (pack|pk)",
    "day (one|1|zero|0)( (\\bE\\b|edition|\\bed\\b|edt))?",
    "(ltd|goty|(action )?game of the year|legacy( pro)?|limited|premium( online)?|(digital )?deluxe|standard|ultimate( evil)?) (collection|\\bed\\b|edition|\\bedt\\b)",
    "(new\\s+)?(super( )?)?(free|fast|quick)?(\\s+)?(and )?(super( )?)?(free|fast|quick|next day)( UK)?( (1st|2nd) class( signed)?)? (dispatch|shipping|post(age)?|delivery|p(\\s+)?p)",
    "((brand )?new( and)?)?( factory)?\\s+((un)?sealed|unopened)", "(complete )?(brand|game) (new|neuf|nuevo)",
    "(great|(very )?good|incredible|excellent|amazing|mint) (condition|value|prices)",
    "((super )?rare|limited run|(\\d+ )?new|pal|great|boxed|full) game(s)?( \\d+)?",
    "limited run( \\d+)?", "(new )?boxed( and)? complete( game)?", "box( )?set", "pre(-| )?owned", "compatible",
    "Used", "very good", "reorderable", "(same|next) day dispatch", "in stock( now)?", "pre(\\s+)?release",
    "Expertly Refurbished Product", "(quality|value) guaranteed", "Highly Rated eBay Seller", "fully tested", "from eBays biggest seller",
    "(the )?official( game)?", "remaster(ed)?", "directors cut", "ctr", "original", "english", "deluxe", "standard", "\\bgoty\\b", "game of the( year)?",
    "Warner Bros", "ubisoft", "currys", "blu-ray", "for playstation( )?vr", "bonus level",
    "Microsoft","sony", "nintendo", "square enix", "ea sport(s)?", "(bandai )?namco", "no scratches",
    "Adventure Role( playing)?", "roleplayng", "Strategy Management",
    "\\bTom clancy(s)?\\b", "(\\bUK\\b|\\bEU\\b)( (new|only|seller|version|stock|import))?",
    "\\bpal\\b", "\\ben\\b", "\\bnc\\b", "\\bfr\\b", "\\bes\\b", "\\bvgc\\b", "\\ban\\b", "\\bpegi( \\d+)?\\b", "\\bLTD\\b",
    "\\bns\\b", "\\bvr\\b( (compatible|required))?", "\\bnsw\\b", "\\bsft\\b", "\\bsave s\\b", "\\bdmc\\b", "\\bBNIB\\b",
    "\\bremake\\b", "\\bhd\\b", "\\b4k\\b", "\\buns\\b", "\\bx360\\b",
    "(the )?video(\\s+)?game(s)?( fasting)?",
    "NEW$", "^NEW", "\\bMarvels\\b", "^best", "^software", "very rare",
    "[^\\p{L}\\p{N}\\p{P}\\p{Z}]"
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
      .replaceFirst("(?i)\\w+(?=\\s+(collection|edition|\\bed\\b|\\bed(i)?t(i)?\\b)) (collection|edition|\\bed\\b|\\bed(i)?t(i)?\\b)", "")
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
