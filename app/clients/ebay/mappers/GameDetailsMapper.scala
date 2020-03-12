package clients.ebay.mappers

import cats.implicits._
import domain.ItemDetails.GameDetails
import domain.ListingDetails

private[mappers] object GameDetailsMapper {

  private val TITLE_WORDS_FILTER = List(
    "Used", "Brand new", "Factory Sealed", "New\\s+Sealed", "Sealed", "Game new", "New and sealed", "new game",
    "Great Condition", "official", "great value", "game nuevo", "Incredible Value", "great prices",
    "Microsoft", "playstation 4", "Nintendo switch", "sony", "ps4", "nintendo", "blu-ray", "switch", "xbox 360", "xbox one", "ubisoft",
    "day one edition", "day one", "day 1 edition", "day 1", "remastered", "Hits", "premium", "directors cut", "ctr", "original", "dbl pk", "double pk", "dbl pack", "double pack",
    "fast free post", "fast and free p\\s+p", "Free Shipping", "Free post", "pal game", "Mirror", "currys", "Highly Rated eBay Seller",
    "\\bTom clancys\\b", "\\bTom clancy\\b",
    "\\bpal\\b", "\\ben\\b", "\\beu\\b", "\\bUK\\b",
    "\\bns\\b", "\\bvr\\b", "\\bedt\\b", "\\bnsw\\b", "\\bsft\\b", "\\bsave s\\b",
    "\\bremake\\b", "\\bhd\\b", "\\bremaster\\b",
    "reorderable", "Expertly Refurbished Product", "Quality guaranteed", "Amazing Value",
    "video game for", "videogames", "videogame fasting",
    "NEW$", "^NEW", "\\b^Marvels\\b",
    "[^\\p{L}\\p{N}\\p{P}\\p{Z}]"
  ).mkString("(?i)", "|", "")

  private val PLATFORMS_MATCH_REGEX = List("PS4", "PLAYSTATION 4", "NINTENDO SWITCH", "SWITCH", "XBOX ONE", "XBOX 1", "XBONE", "XBOX 360")
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
    val title = listingDetails.properties.getOrElse("Game Name", listingDetails.title).replaceAll("[—“”!•£&#,’'*()/|:.\\[\\]]", "")
    PLATFORMS_MATCH_REGEX.split(title)
      .find(_.nonEmpty)
      .getOrElse(title)
      .replaceAll(TITLE_WORDS_FILTER, "")
      .replaceFirst("(?i)\\w+(?=\\s+(edition|\\bed\\b)) (edition|\\bed\\b)", "")
      .replaceAll("é", "e")
      .replaceAll("(?i)(playerunknown)", "Player Unknown")
      .replaceAll("(?i)(littlebigplanet)", "Little Big Planet")
      .replaceAll("(?i)(farcry)", "Far Cry")
      .replaceAll("(?i)(superheroes)", "Super Heroes")
      .replaceAll("(?i)(W2K)", "WWE 2k")
      .replaceAll("(?i)(NierAutomata)", "Nier Automata")
      .replaceAll("(?i)(fifa 2020)", "FIFA 20")
      .replaceAll(" +", " ")
      .replaceAll("(?i)( -|- )", " ")
      .replaceFirst("(?i)(^-)", "")
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
