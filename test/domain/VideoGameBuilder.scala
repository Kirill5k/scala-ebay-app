package domain

import java.net.URI
import java.time.Instant

import domain.ItemDetails.GameDetails
import domain.ResellableItem.VideoGame

object VideoGameBuilder {
  def get(name: String): VideoGame = VideoGame(
    GameDetails(Some(name), Some("XBOX ONE"), Some("2019"), Some("Action")),
    ListingDetails(
      new URI("https://www.ebay.co.uk/itm/game-xbox-one-2019-/333474293066"),
      name,
      Some(s"$name xbox one 2019. Condition is New. Game came as part of bundle and not wanted. Never playes. Dispatched with Royal Mail 1st Class Large Letter."),
      None,
      new URI("https://i.ebayimg.com/images/g/0kcAAOSw~5ReGFCQ/s-l1600.jpg"),
      Seq("FIXED_PRICE"),
      "168.robinhood",
      BigDecimal.valueOf(32.99),
      "New",
      Instant.now,
      None,
      Map(
        "Game Name" -> name,
        "Release Year" -> "2019",
        "Platform" -> "Microsoft Xbox One",
        "Genre" -> "Action"
      )
    ),
    Some(ResellPrice(BigDecimal.valueOf(10), BigDecimal.valueOf(20)))
  )
}
