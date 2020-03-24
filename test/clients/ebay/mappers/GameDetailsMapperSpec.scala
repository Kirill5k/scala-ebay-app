package clients.ebay.mappers

import java.time.Instant

import domain.ListingDetails
import org.scalatest._

class GameDetailsMapperSpec extends WordSpec with MustMatchers with Inspectors {

  val testListing = ListingDetails(
    "https://www.ebay.co.uk/itm/Call-of-Duty-Modern-Warfare-Xbox-One-/274204760218",
    "Call of Duty: Modern Warfare Limited Edition (Xbox One)",
    Some("Call of Duty: Modern Warfare (Xbox One). Condition is New. Dispatched with Royal Mail 1st Class Large Letter."),
    None,
    Some("https://i.ebayimg.com/images/g/PW4AAOSweS5eHsrk/s-l1600.jpg"),
    List("FIXED_PRICE"),
    Some("boris999"),
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

    "map platform from title even if it exists in properties" in {
      val listingDetails = testListing.copy(properties = testListing.properties + ("Platform" -> "Xbox 360"))

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.platform must be (Some("XBOX ONE"))
    }


    "get details from title if properties are missing" in {
      val listingDetails = testListing.copy(properties = Map())

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("Call of Duty Modern Warfare"))
      gameDetails.platform must be (Some("XBOX ONE"))
      gameDetails.releaseYear must be (None)
      gameDetails.genre must be (None)
    }

    "split pubg title" in {
      val listingDetails = testListing.copy(title = "NEW playerunknowns battlegrounds pal PSVR Ultimate Evil Ed", properties = Map())

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("Player Unknowns battlegrounds PSVR"))
    }

    "leave new in the middle of title" in {
      val listingDetails = testListing.copy(title = "pal Wolfenstein: The NEW Colosus", properties = Map())

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("Wolfenstein The NEW Colosus"))
    }

    "remove new from the end and beginning of title" in {
      val listingDetails = testListing.copy(title = "NEW LEGO Marvel Avengers New", properties = Map())

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("LEGO Marvel Avengers"))
    }

    "remove rubbish words from title" in {
      val titles = List(
        "Call of Duty: Infinite Warfare - video game for the PLAYSTATION 4",
        "Call of Duty: Infinite Warfare - Fast and Free shipping",
        "Call of Duty: Infinite Warfare - day one edition - day 0 ed - day 1 E - day 1",
        "Call of Duty: Infinite Warfare HD - double pack - premium online edition - XBOX 1 GAME NEUF",
        "Call of Duty: Infinite Warfare - premium edt -- Legacy pro ed - elite edition",
        "Playstation 4/PAL-Call of Duty: Infinite Warfare NEW",
        "Marvel's Call of Duty: Infinite Warfare • New • Sealed •",
        "(Xbox One) Tom Clancy's Call of Duty: Infinite Warfare deluxe edition - VR vr NEW",
        "\uD83D\uDC96 Call of Duty: Infinite Warfareused * | Limited Edition - Remastered: & Game new (Xbox One) "
      )

      forAll (titles) { title =>
        val details = GameDetailsMapper.from(testListing.copy(title = title, properties = Map()))
        details.name must be (Some("Call of Duty Infinite Warfare"))
      }
    }
  }
}
