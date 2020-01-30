package clients.ebay.mappers

import java.net.URI
import java.time.Instant

import domain.ListingDetails
import org.scalatest._

class GameDetailsMapperSpec extends WordSpec with MustMatchers {

  val testListing = ListingDetails(
    new URI("https://www.ebay.co.uk/itm/Call-of-Duty-Modern-Warfare-Xbox-One-/274204760218"),
    "Call of Duty: Modern Warfare (Xbox One)",
    Some("Call of Duty: Modern Warfare (Xbox One). Condition is New. Dispatched with Royal Mail 1st Class Large Letter."),
    None,
    new URI("https://i.ebayimg.com/images/g/PW4AAOSweS5eHsrk/s-l1600.jpg"),
    Seq("FIXED_PRICE"),
    "boris999",
    BigDecimal.valueOf(10),
    "USED",
    Instant.now,
    None,
    Map("Platform" -> "Microsoft Xbox One", "Game Name" -> "Call of Duty: Modern Warfare", "Release Year" -> "2019", "Genre" -> "Action")
  )

  "GameDetailsMapper" should {

    "get details from properties if they are present" in {
      val listingDetails = testListing.copy()

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("Call of Duty Modern Warfare"))
      gameDetails.platform must be (Some("XBOX ONE"))
      gameDetails.releaseYear must be (Some("2019"))
      gameDetails.genre must be (Some("Action"))
    }

    "get details from title if properties are missing" in {
      val listingDetails = testListing.copy(properties = Map())

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("Call of Duty Modern Warfare"))
      gameDetails.platform must be (Some("XBOX ONE"))
      gameDetails.releaseYear must be (None)
      gameDetails.genre must be (None)
    }

    "remove redundant words from title" in {
      val listingDetails = testListing.copy(title = "\uD83D\uDC96 Call of Duty: Modern Warfareused * | Limited Edition - Remastered:  Game new (Xbox One) ", properties = Map())

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("Call of Duty Modern Warfare Limited Edition"))
      gameDetails.platform must be (Some("XBOX ONE"))
    }

    "map title with unusual format" in {
      val listingDetails = testListing.copy(title = "(Xbox One) Tom Clancy's Call of Duty: Modern Warfare", properties = Map())

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("Call of Duty Modern Warfare"))
      gameDetails.platform must be (Some("XBOX ONE"))
    }
  }
}
