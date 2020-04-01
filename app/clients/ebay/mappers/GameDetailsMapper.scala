package clients.ebay.mappers

import cats.implicits._
import domain.ItemDetails.GameDetails
import domain.ListingDetails

private[mappers] object GameDetailsMapper {

  private val TITLE_WORDS_FILTER = List(
    "(dbl|double|triple|twin) (pack|pk)",
    "day (one|1|zero|0)( (\\bE\\b|edition|\\bed\\b))?",
    "(goty|game of the year|legacy( pro)?|premium( online)?|(digital )?deluxe|standard|ultimate( evil)?) (\\bed\\b|edition|\\bedt\\b)",
    "(fast\\s+(and )?)?free(\\s+fast)? (pp|shipping|post|delivery|p\\s+p)",
    "(video( )?)?game for( the)?( playstation)?( vr)?",
    "(brand|game) (new|neuf|nuevo)", "(new( and)?)?( factory)?\\s+sealed",
    "(great|(very )?good|incredible|excellent|amazing) (condition|value|prices)",
    "(super rare|limited run|new|pal) game(s)?",
    "limited run( \\d+)?",
    "Used", "very good", "unopened", "reorderable", "next day dispatch",
    "Expertly Refurbished Product", "Quality guaranteed", "Highly Rated eBay Seller", "fully tested",
    "official", "remaster(ed)?", "directors cut", "ctr", "original", "english", "deluxe", "standard", "\\bgoty\\b", "game of the( year)?",
    "Warner Bros", "ubisoft", "currys", "blu-ray", "for playstation vr", "bonus level",
    "Microsoft", "playstation 4", "Nintendo switch", "sony", "ps4", "playstation", "nintendo", "switch", "\\bxb(o)?\\b",
    "xbox( one|360)?",
    "\\bTom clancy(s)?\\b", "\\bUK\\b( seller|version)?",
    "\\bpal\\b", "\\ben\\b", "\\beu\\b", "\\bes\\b", "\\bvgc\\b", "\\ban\\b",
    "\\bns\\b", "\\bvr\\b( compatible)?", "\\bnsw\\b", "\\bsft\\b", "\\bsave s\\b", "\\bhits\\b", "\\bdmc\\b",
    "\\bremake\\b", "\\bhd\\b",
    "videogames", "videogame fasting",
    "NEW$", "^NEW", "\\bMarvels\\b", "^best", "^software", "^rare",
    "[^\\p{L}\\p{N}\\p{P}\\p{Z}]"
  ).mkString("(?i)", "|", "")

  private val PLATFORMS_MATCH_REGEX = List(
    "PS4", "PLAYSTATION 4",
    "NINTENDO SWITCH", "SWITCH",
    "XBOX ONE", "XBOX 1", "XB1", "XBONE", "X BOX ONE", "XBOX 360"
  ).mkString("(?i)", "|", "").r

  private val PLATFORM_MAPPINGS: Map[String, String] = Map(
    "SONY PLAYSTATION 4" -> "PS4",
    "PLAYSTATION 4" -> "PS4",
    "SONY PLAYSTATION 3" -> "PS3",
    "SONY PLAYSTATION 2" -> "PS2",
    "SONY PLAYSTATION 1" -> "PS1",
    "SONY PLAYSTATION" -> "PS4",
    "PLAYSTATION 2" -> "PS2",
    "NINTENDO SWITCH" -> "SWITCH",
    "MICROSOFT XBOX ONE" -> "XBOX ONE",
    "XBONE" -> "XBOX ONE",
    "X BOX ONE" -> "XBOX ONE",
    "XBOX 1" -> "XBOX ONE",
    "XB1" -> "XBOX ONE",
    "MICROSOFT XBOX 360" -> "XBOX 360",
    "MICROSOFT XBOX" -> "XBOX",
  )

  def from(listingDetails: ListingDetails): GameDetails = {
    GameDetails(
      name = mapName(listingDetails),
      platform = mapPlatform(listingDetails),
      genre = mapGenre(listingDetails),
      releaseYear = listingDetails.properties.get("Release Year")
    )
  }

  private def mapName(listingDetails: ListingDetails): Option[String] = {
    val title = listingDetails.properties.getOrElse("Game Name", listingDetails.title).replaceAll("[`—“”!•£&#,’'*()/|:.\\[\\]]", "")
    PLATFORMS_MATCH_REGEX.split(title)
      .find(_.nonEmpty)
      .getOrElse(title)
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
  }

  private def mapPlatform(listingDetails: ListingDetails): Option[String] = {
    PLATFORMS_MATCH_REGEX.findFirstIn(listingDetails.title)
      .orElse(listingDetails.properties.get("Platform").map(_.split(",|/")(0)))
      .map(_.toUpperCase.trim)
      .map(platform => PLATFORM_MAPPINGS.getOrElse(platform, platform))
  }

  private def mapGenre(listingDetails: ListingDetails): Option[String] = {
    listingDetails.properties.get("Genre").orElse(listingDetails.properties.get("Sub-Genre"))
  }
}
