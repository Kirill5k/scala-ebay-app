package clients.ebay.mappers

import cats.implicits._
import domain.ItemDetails.GameDetails
import domain.{Packaging, ListingDetails}

private[mappers] object GameDetailsMapper {

  private val TITLE_WORDS_FILTER = List(
    "(video( )?)?game for( the)?( playstation)?(\\s+(vr|\\d+))?",
    "(dbl|double|triple|twin) (pack|pk)",
    "day (one|1|zero|0)( (\\bE\\b|edition|\\bed\\b))?",
    "(goty|game of the year|legacy( pro)?|premium( online)?|(digital )?deluxe|standard|ultimate( evil)?) (\\bed\\b|edition|\\bedt\\b)",
    "(fast\\s+(and )?)?free(\\s+fast)? (pp|shipping|post|delivery|p\\s+p)",
    "(brand|game) (new|neuf|nuevo)", "(new( and)?)?( factory)?\\s+sealed",
    "(great|(very )?good|incredible|excellent|amazing) (condition|value|prices)",
    "(super rare|limited run|new|pal) game(s)?",
    "limited run( \\d+)?",
    "Used", "very good", "unopened", "reorderable", "next day dispatch",
    "Expertly Refurbished Product", "Quality guaranteed", "Highly Rated eBay Seller", "fully tested",
    "official", "remaster(ed)?", "directors cut", "ctr", "original", "english", "deluxe", "standard", "\\bgoty\\b", "game of the( year)?",
    "Warner Bros", "ubisoft", "currys", "blu-ray", "for playstation vr", "bonus level",
    "playstation((\\s+)?\\d+)?", "xbox((\\s+)?(one|\\d+))?", "ps\\d+", "\\bxb(\\s+)?(one|\\d+)?\\b",
    "Microsoft", "Nintendo switch", "sony", "nintendo", "switch",
    "\\bTom clancy(s)?\\b", "\\bUK\\b( seller|version|stock)?", "Adventure Role( playing)?",
    "\\bpal\\b", "\\ben\\b", "\\beu\\b", "\\bes\\b", "\\bvgc\\b", "\\ban\\b", "\\bpegi( \\d+)?\\b",
    "\\bns\\b", "\\bvr\\b( compatible)?", "\\bnsw\\b", "\\bsft\\b", "\\bsave s\\b", "\\bhits\\b", "\\bdmc\\b",
    "\\bremake\\b", "\\bhd\\b",
    "videogames", "videogame fasting",
    "NEW$", "^NEW", "\\bMarvels\\b", "^best", "^software", "^rare",
    "[^\\p{L}\\p{N}\\p{P}\\p{Z}]"
  ).mkString("(?i)", "|", "")

  private val PLATFORMS_MATCH_REGEX = List(
    "PS4", "PLAYSTATION(\\s+)?(\\d+)",
    "NINTENDO SWITCH", "SWITCH",
    "XB(OX)?(\\s+)?(ONE|\\d)", "XBOX 1", "XB1", "XBONE", "X BOX ONE", "XBOX 360"
  ).mkString("(?i)", "|", "").r

  private val BUNDLE_MATCH_REGEX = List(
    "(new|multiple|PS4) games", "bundle", "job(\\s+)?lot"
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
      .replaceFirst("(?i)\\w+(?=\\s+(edition|\\bed\\b|\\bedt\\b)) (edition|\\bed\\b|\\bedt\\b)", "")
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
    def withoutSpecialChars: String = str.replaceAll("[`—“”!•£&#,’'*()|:.\\[\\]]", "").replaceAll("/", " ")
  }
}
