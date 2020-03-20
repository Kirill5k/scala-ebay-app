package clients.ebay.mappers

import java.time.Instant

import domain.ListingDetails
import org.scalatest._

class GameDetailsMapperSpec extends WordSpec with MustMatchers {

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

    "remove redundant words from title" in {
      val listingDetails = testListing.copy(title = "\uD83D\uDC96 Call of Duty: Modern Warfareused * | Limited Edition - Remastered: & Game new (Xbox One) ", properties = Map())

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("Call of Duty Modern Warfare"))
      gameDetails.platform must be (Some("XBOX ONE"))
    }

    "map title with unusual format" in {
      val listingDetails = testListing.copy(title = "(Xbox One) Tom Clancy's Call of Duty: Modern Warfare - VR vr NEW", properties = Map())

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("Call of Duty Modern Warfare"))
      gameDetails.platform must be (Some("XBOX ONE"))
    }

    "split pubg title" in {
      val listingDetails = testListing.copy(title = "NEW playerunknowns battlegrounds pal PSVR", properties = Map())

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

    "remove PAL and save s from title" in {
      val listingDetails = testListing.copy(title = "Playstation 4/PAL-Rise Of The Tomb Raider NEW", properties = Map())

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("Rise Of The Tomb Raider"))
    }

    "remove other random characters" in {
      val listingDetails = testListing.copy(title = "Marvel's Spider-man • New • Sealed •", properties = Map())

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("Spider-man"))
    }

    "remove edition from title" in {
      val listingDetails = testListing.copy(title = "Call of Duty: Infinite Warfare -- Legacy Edition", properties = Map())

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("Call of Duty Infinite Warfare"))
    }

    "remove ed from title" in {
      val listingDetails = testListing.copy(title = "Call of Duty: Infinite Warfare -- Legacy ed", properties = Map())

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("Call of Duty Infinite Warfare"))
    }

    "remove HD and double pack from title" in {
      val listingDetails = testListing.copy(title = "Assassins Creed - Rogue HD - double pack - XBOX 1 GAME NEUF", properties = Map())

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("Assassins Creed  Rogue"))
      gameDetails.platform must be (Some("XBOX ONE"))
    }

    "remove day one edition from title" in {
      val listingDetails = testListing.copy(title = "Assassins Creed - Rogue - day one edition - day 0 ed - day 1 E - day 1", properties = Map())

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("Assassins Creed  Rogue"))
    }
  }
}
