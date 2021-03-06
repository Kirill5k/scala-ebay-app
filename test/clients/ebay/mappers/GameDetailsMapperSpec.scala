package clients.ebay.mappers

import java.time.Instant

import domain.{ListingDetails, Packaging}
import org.scalatest.Inspectors
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class GameDetailsMapperSpec extends AnyWordSpec with Matchers with Inspectors {

  val testListing = ListingDetails(
    "https://www.ebay.co.uk/itm/Call-of-Duty-Modern-Warfare-Xbox-One-/274204760218",
    "Call of Duty: Modern Warfare Limited Edition (Xbox One)",
    Some("Call of Duty: Modern Warfare (Xbox One). Condition is New. Dispatched with Royal Mail 1st Class Large Letter."),
    None,
    Some("https://i.ebayimg.com/images/g/PW4AAOSweS5eHsrk/s-l1600.jpg"),
    "USED",
    Instant.now,
    "EBAY:boris999",
    Map(
      "Platform" -> "Microsoft Xbox One",
      "Game Name" -> "Call of Duty: Modern Warfare",
      "Release Year" -> "2019",
      "Genre" -> "Action"
    )
  )

  "GameDetailsMapper" should {

    "get details from properties if they are present except for platform and title" in {
      val listingDetails = testListing.copy(title = "Call of Duty Modern Warfare 2 PS4")

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("Call of Duty Modern Warfare 2"))
      gameDetails.platform must be (Some("PS4"))
      gameDetails.releaseYear must be (Some("2019"))
      gameDetails.genre must be (Some("Action"))
      gameDetails.packaging must be (Packaging.Single)
    }

    "map uncommon platform spellings" in {
      val platforms = Map(
        "PS4" -> "PS4",
        "PS2" -> "PS2",
        "PS5" -> "PS5",
        "PLAYSTATION4" -> "PS4",
        "PLAYSTATION 4" -> "PS4",
        "PLAYSTATION 3" -> "PS3",
        "XBONE" -> "XBOX ONE",
        "XB ONE" -> "XBOX ONE",
        "XB 1" -> "XBOX ONE",
        "XB360" -> "XBOX 360",
        "XBOX 360" -> "XBOX 360",
        "X BOX ONE" -> "XBOX ONE",
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

    "map telltale game series to simply telltale" in {
      val listingDetails = testListing.copy(title = "Minecraft A Telltale Game Series", properties = Map())

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("Minecraft Telltale"))
    }


    "only map WII title from complete words" in {
      val listingDetails = testListing.copy(title = s"COD WWII for Sony Playstation 4", properties = Map())

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("Call of Duty WWII"))
      gameDetails.platform must be (Some("PS4"))
    }

    "get details from title if properties are missing" in {
      val listingDetails = testListing.copy(properties = Map())

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("Call of Duty Modern Warfare"))
      gameDetails.platform must be (Some("XBOX ONE"))
      gameDetails.releaseYear must be (None)
      gameDetails.genre must be (None)
    }

    "remove GT from gran turismo title" in {
      val listingDetails = testListing.copy(title = "Gran Turismo 6 GT6")

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("Gran Turismo 6"))
    }

    "split pubg title" in {
      val listingDetails = testListing.copy(title = "NEW Playerunknowns battlegrounds pal PSVR Ultimate Evil Ed", properties = Map())

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("Player Unknowns battlegrounds"))
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

    "keep word 'ultimate' if it is too far from 'edition'" in {
      val listingDetails = testListing.copy(title = "Marvel Ultimate Alliance 3: The Black Order - Standard Edition")

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("Marvel Ultimate Alliance 3 The Black Order"))
    }

    "map bundles" in {
      val listingDetails = testListing.copy(title = "job lot 5 PS4 Games")

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("job lot 5"))
      gameDetails.platform must be (Some("PS4"))
      gameDetails.packaging must be (Packaging.Bundle)
    }

    "keep collection if there is just 1 word" in {
      val listingDetails = testListing.copy(title = "Bioshock Collection PS4")

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("Bioshock Collection"))
    }

    "remove chars with code -1" in {
      val listingDetails = testListing.copy(title = "MINECRAFT XBOX 360 EDITION ")

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("MINECRAFT"))
    }

    "map gta and rdr to full title" in {
      val titles = Map(
        "gta5" -> "Grand Theft Auto 5",
        "gta 5" -> "Grand Theft Auto 5",
        "grand theft auto gta" -> "grand theft auto Grand Theft Auto",
        "rdr" -> "Red Dead Redemption",
        "sunset overdrive" -> "sunset overdrive",
      )

      forAll (titles) { case (title, expected) =>
        val listingDetails = testListing.copy(title = title)
        val gameDetails = GameDetailsMapper.from(listingDetails)
        gameDetails.name must be (Some(expected))
      }
    }

    "remove formula 1 if f1" in {
      val titles = Map(
        "formula 1 f1 2019" -> "f1 2019",
        "formula one f1 2019" -> "f1 2019",
        "f1 formula one 2019" -> "f1 2019",
      )

      forAll (titles) { case (title, expected) =>
        val listingDetails = testListing.copy(title = title)
        val gameDetails = GameDetailsMapper.from(listingDetails)
        gameDetails.name must be (Some(expected))
      }
    }

    "remove year after number" in {
      val titles = Map(
        "FIFA 19 2019" -> "FIFA 19",
        "FIFA 2019" -> "FIFA 19",
        "FIFA 2020" -> "FIFA 20",
        "WWE 2k17 2019" -> "WWE 2k17",
        "Call of Duty: Infinite Warfare 2 2019" -> "Call of Duty Infinite Warfare 2"
      )

      forAll (titles) { case (title, expected) =>
        val listingDetails = testListing.copy(title = title)
        val gameDetails = GameDetailsMapper.from(listingDetails)
        gameDetails.name must be (Some(expected))
      }
    }

    "remove wrestling after 2k17 title" in {
      val listingDetails = testListing.copy(title = "WWE 2k19 Wrestling")

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("WWE 2k19"))
    }

    "remove VR and PSVR from title" in {
      val listingDetails = testListing.copy(title = "Call of Duty: Infinite Warfare VR PSVR")

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("Call of Duty Infinite Warfare"))
    }

    "remove noise words from title" in {
      val titles = List(
        "Call of Duty Infinite Warfare £54.99",
        "Call of Duty Infinite Warfare game for kids",
        "Call of Duty Infinite Warfare also works on ps4",
        "Call of Duty Infinite Warfare also plays on ps4",
        "Call of Duty Infinite Warfare *brand new sealed*",
        "Call of Duty Infinite Warfare pre-owned multi-player xbox 360",
        "Call of Duty Infinite Warfare multi - player xbox 360",
        "Call of Duty-Infinite Warfare by rockstar games",
        "Call of Duty-Infinite Warfare from rockstar games",
        "Call of Duty-Infinite Warfare 20th Year Celebration",
        "Call of Duty-Infinite Warfare from EA sports",
        "Call of Duty-Infinite Warfare Activision presents",
        "Call of Duty-Infinite Warfare xbox 360 blah blah blah",
        "Call of Duty - Infinite Warfare playstation 4 blah blah blah",
        "Call of Duty - Infinite Warfare - Sony PS4 blah blah blah",
        "PS4 2019 Call of Duty: Infinite Warfare In Working Order",
        "Playstation 4 - PS4 - Call of Duty: Infinite Warfare In Working Order",
        "Playstation 4 PS4 game Call of Duty: Infinite Warfare superultimate edition",
        "Call of Duty: Infinite Warfare 20th anniversary FUVG ID7274z",
        "Call of Duty: Infinite Warfare classic edition foo",
        "Call of Duty: Infinite Warfare new in wrapper",
        "Call of Duty: Infinite Warfare only on playstation",
        "Call of Duty: Infinite Warfare PS4 Game UK PAL VR Compatible PREOWNED",
        "3307216096665BC Call of Duty: Infinite Warfare BRAND NEW SEALED",
        "3307216096665BC Call of Duty: Infinite Warfare new and sealed",
        "Call of Duty: Infinite Warfare classics edition foo",
        "*Brand New and Sealed* Call of Duty: Infinite Warfare",
        "New & Sealed Nintendo Switch Game Call of Duty: Infinite Warfare",
        "New & Sealed Call of Duty: Infinite Warfare PAL #1099",
        "Call of Duty: Infinite Warfare 20th anniversary edition",
        "Playstation 4 (PS4) Limited Run #62 - Call of Duty: Infinite Warfare fast and free delivery",
        "Limited Run #197: Call of Duty: Infinite Warfare Brigadier Edition (PS4 LRG)",
        "Sealed Call of Duty: Infinite Warfare - game of the year goty free and fast 1st class post",
        "Call of Duty: Infinite Warfare - game of the year goty quick 1st class signed post for playstation vr",
        "Call of Duty: Infinite Warfare - good for sony playstati",
        "Call of Duty: Infinite Warfare HD collection",
        "Call of Duty: Infinite Warfare only on Playstation 4",
        "Call of Duty: Infinite Warfare - game of the year edition goty",
        "Call of Duty: Infinite Warfare - video game for the playstation vr PLAYSTATION 4 2015",
        "Call of Duty: Infinite Warfare - video game for the PLAYSTATION 4",
        "Call of Duty: Infinite Warfare - new fast post vr required (foo-bar)",
        "Call of Duty: Infinite Warfare - new Fast free post for PS4 game 2020",
        "Call of Duty: Infinite Warfare includes mega awesome pack with goodies",
        "Call of Duty: Infinite Warfare - vr compatible psvr required",
        "Call of Duty: Infinite Warfare includes 5 bonus levels",
        "Call of Duty: Infinite Warfare with terminator bonus content",
        "Call of Duty: Infinite Warfare - super fast and superfree UK post",
        "Call of Duty: Infinite Warfare - Fast and Free shipping complete with manual and book",
        "Call of Duty: Infinite Warfare - day one edition - day 0 ed - day 1 E - day 1",
        "Call of Duty: Infinite Warfare HD - double pack - premium online edition - XBOX 1 GAME",
        "Call of Duty: Infinite Warfare - premium edt -- Legacy pro ed - elite edition with some bonuses",
        "Call of Duty: Infinite Warfare - brand new and factory sealed",
        "Call of Duty: Infinite Warfare (Day One Edition) [Ge VideoGames Amazing Value]",
        "Call of Duty: Infinite Warfare - complete free 1st class uk postage",
        "Call of Duty: Infinite Warfare the official authentic videogame - new unopened",
        "Call of Duty: Infinite Warfare for microsoft xbox one",
        "Call of Duty: Infinite Warfare comes with both manuals",
        "Call of Duty: Infinite Warfare with both manuals",
        "Call of Duty: Infinite Warfare VideoGames",
        "genuine brand new sealed Call of Duty: Infinite Warfare The game",
        "brand new Call of Duty: Infinite Warfare For ages 18+",
        "Call of Duty            Infinite            Warfare            ",
        "BEST PS4 GAME Call of Duty: Infinite Warfare EU-Import factory sealed next day dispatch free",
        "300076206 Limited run 170 Call of Duty: Infinite Warfare new boxed and complete game",
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

    "remove roman numbers followed by digit or vice versa" in {
      val titles = Map(
        "Call of Duty Black Ops iii 3" -> "Call of Duty Black Ops iii",
        "Call of Duty Black Ops iv 4" -> "Call of Duty Black Ops iv",
        "Call of Duty Black Ops ii 2" -> "Call of Duty Black Ops ii",
        "Call of Duty Black Ops 2 ii" -> "Call of Duty Black Ops 2",
        "witcher 3 iii" -> "witcher 3",
      )

      forAll (titles) { case (title, expected) =>
        val details = GameDetailsMapper.from(testListing.copy(title = title, properties = Map()))
        details.name must be (Some(expected))
      }
    }

    "quick test" in {
      val listingDetails = testListing.copy(title = "motorsport kids racing motorbike game")

      val gameDetails = GameDetailsMapper.from(listingDetails)

      gameDetails.name must be (Some("motorsport"))
    }
  }
}
