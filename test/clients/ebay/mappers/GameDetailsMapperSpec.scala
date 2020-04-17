package clients.ebay.mappers

import java.time.Instant

import domain.{Packaging, ListingDetails}
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
      gameDetails.packaging must be (Packaging.Single)
      gameDetails.isBundle must be (false)
    }

    "map uncommon platform spellings" in {
      val platforms = Map(
        "PS4" -> "PS4",
        "PLAYSTATION4" -> "PS4",
        "PLAYSTATION 4" -> "PS4",
        "PLAYSTATION 3" -> "PS3",
        "XBONE" -> "XBOX ONE",
        "XB ONE" -> "XBOX ONE",
        "XB 1" -> "XBOX ONE"
      )

      forAll (platforms) { platform =>
        val details = GameDetailsMapper.from(testListing.copy(title = s"Call of Duty: Infinite Warfare ${platform._1}", properties = Map()))
        details.platform must be (Some(platform._2))
      }
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

    "map bundles" in {
      val listingDetails = testListing.copy(title = "job lot 5 PS4 Games")

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("job lot 5 Games"))
      gameDetails.platform must be (Some("PS4"))
      gameDetails.packaging must be (Packaging.Bundle)
      gameDetails.isBundle must be (true)
    }

    "remove rubbish words from title" in {
      val titles = List(
        "Call of Duty: Infinite Warfare - game of the year goty free and fast 1st class post",
        "Call of Duty: Infinite Warfare - game of the year goty quick 1st class signed post",
        "Call of Duty: Infinite Warfare - game of the year edition goty",
        "Call of Duty: Infinite Warfare - video game for the playstation vr PLAYSTATION 4 2015",
        "Call of Duty: Infinite Warfare - video game for the PLAYSTATION 4",
        "Call of Duty: Infinite Warfare - new fast post vr required",
        "Call of Duty: Infinite Warfare - new Fast free post for PS4 game 2020",
        "Call of Duty: Infinite Warfare - super fast and superfree UK post",
        "Call of Duty: Infinite Warfare - Fast and Free shipping",
        "Call of Duty: Infinite Warfare - day one edition - day 0 ed - day 1 E - day 1",
        "Call of Duty: Infinite Warfare HD - double pack - premium online edition - XBOX 1 GAME",
        "Call of Duty: Infinite Warfare - premium edt -- Legacy pro ed - elite edition",
        "Call of Duty: Infinite Warfare - brand new and sealed",
        "Call of Duty: Infinite Warfare the official videogame - new unopened",
        "BEST PS4 GAME Call of Duty: Infinite Warfare",
        "Limited run 170 Call of Duty: Infinite Warfare",
        "Playstation 4/PAL-Call of Duty: Infinite Warfare NEW",
        "Marvel's Call of Duty: Infinite Warfare • new and factory Sealed •",
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
