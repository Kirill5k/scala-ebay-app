package clients.ebay.mappers

import cats.implicits._
import domain.ItemDetails.GameDetails
import domain.ListingDetails

private[mappers] object GameDetailsMapper {

  private val TITLE_WORDS_FILTER = List(
    "Used", "Brand new", "Factory Sealed", "Sealed", "Game new", "New and sealed", "new game", "Great Condition", "official", "great value", "game nuevo",
    "Microsoft", "playstation 4", "Nintendo switch", "sony", "ps4", "nintendo", "blu-ray", "switch", "xbox 360", "xbox one",
    "day one edition", "day 1", "remastered", "Hits", "premium", "directors cut", "ctr", "original",
    "Free post", "pal game", "Uk pal", "Mirror", "currys",
    "Tom clancys",
    "\\bpal\\b", "\\bvr\\b",
    "reorderable",
    "video game for", "videogames", "videogame fasting",
    "[^\\p{L}\\p{N}\\p{P}\\p{Z}]"
  ).mkString("(?i)", "|", "")

  private val PLATFORMS_MATCH_REGEX = List("PS4", "PLAYSTATION 4", "NINTENDO SWITCH", "SWITCH", "XBOX ONE", "XBONE")
    .mkString("(?i)", "|", "").r

  private val PLATFORM_MAPPINGS: Map[String, String] = Map(
    "SONY PLAYSTATION 4" -> "PS4",
    "PLAYSTATION 4" -> "PS4",
    "SONY PLAYSTATION 3" -> "PS3",
    "SONY PLAYSTATION 2" -> "PS2",
    "SONY PLAYSTATION 1" -> "PS1",
    "PLAYSTATION 2" -> "PS2",
    "NINTENDO SWITCH" -> "SWITCH",
    "MICROSOFT XBOX ONE" -> "XBOX ONE",
    "XBONE" -> "XBOX ONE",
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
    val title = listingDetails.properties.getOrElse("Game Name", listingDetails.title).replaceAll("[#,’'*()/|:.\\[\\]]", "")
    PLATFORMS_MATCH_REGEX.split(title)
      .headOption
      .filter(!_.isEmpty)
      .getOrElse(title)
      .replaceAll(TITLE_WORDS_FILTER, "")
      .replaceFirst("(?i)\\w+(?=\\s+edition) edition", "")
      .replaceAll("é", "e")
      .replaceAll("(?i)(playerunknown)", "Player Unknown")
      .replaceAll("(?i)(littlebigplant)", "Little Big Planet")
      .replaceAll("(?i)(farcry)", "Far Cry")
      .replaceAll(" +", " ")
      .replaceAll("(?i)( -|- )", " ")
      .replaceFirst("^-", "")
      .trim()
      .some
  }

  private def mapPlatform(listingDetails: ListingDetails): Option[String] = {
    listingDetails.properties.get("Platform")
      .map(platform => platform.split(",|/")(0))
      .orElse(PLATFORMS_MATCH_REGEX.findFirstIn(listingDetails.title))
      .map(_.toUpperCase.trim)
      .map(platform => PLATFORM_MAPPINGS.getOrElse(platform, platform))
  }

  private def mapGenre(listingDetails: ListingDetails): Option[String] = {
    listingDetails.properties.get("Genre").orElse(listingDetails.properties.get("Sub-Genre"))
  }
}
