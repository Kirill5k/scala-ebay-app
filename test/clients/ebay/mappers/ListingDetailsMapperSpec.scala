package clients.ebay.mappers

import java.time.Instant

import domain.ItemDetails._
import domain.ListingDetails
import org.scalatest._
import clients.ebay.mappers.ListingDetailsMapper._

class ListingDetailsMapperSpec extends WordSpec with MustMatchers {

  val videoGameListing = ListingDetails(
    "https://www.ebay.co.uk/itm/Call-of-Duty-Modern-Warfare-Xbox-One-/274204760218",
    "Call of Duty: Modern Warfare (Xbox One)",
    Some("Call of Duty: Modern Warfare (Xbox One). Condition is New. Dispatched with Royal Mail 1st Class Large Letter."),
    None,
    "https://i.ebayimg.com/images/g/PW4AAOSweS5eHsrk/s-l1600.jpg",
    Seq("FIXED_PRICE"),
    "boris999",
    BigDecimal.valueOf(10),
    "USED",
    Instant.now,
    None,
    Map("Platform" -> "Microsoft Xbox One", "Game Name" -> "Call of Duty: Modern Warfare", "Release Year" -> "2019", "Genre" -> "Action")
  )

  val mobilePhoneListing = ListingDetails(
    "https://www.ebay.co.uk/itm/Call-of-Duty-Modern-Warfare-Xbox-One-/274204760218",
    "Samsung Galaxy S10 128gb UNLOCKED Prism Blue",
    Some("Samsung Galaxy S10 Used"),
    Some("Up For GrabsSamsung Galaxy S10 128gb UNLOCKED Prism BlueGood ConditionThe usual minor wear and Tear as you would expect from a used phone.It has been in a case with a screen protector since new however they appears tohave 1 x Deeper Scratch no more than 1cm long to the top left of the phone which does not affect the use of the phone nor does it show up when the screen is in use and you have got to look for it to see it when the screen is off.Comes with Wall Plug and Wire.I like the phone but unf"),
    "https://i.ebayimg.com/images/g/yOMAAOSw~5ReGEH2/s-l1600.jpg",
    Seq("FIXED_PRICE"),
    "boris999",
    BigDecimal.valueOf(10),
    "USED",
    Instant.now,
    None,
    Map(
      "Brand" -> "Samsung",
      "Model" -> "Samsung Galaxy S10",
      "Storage Capacity" -> "128 GB",
      "Network" -> "Unlocked",
      "Colour" -> "Pink",
      "Manufacturer Colour" -> "Rose Gold, Pink"
    )
  )

  "ListingDetailsMapper" should {

    "transform to GameDetails" in {
      val gameDetails = videoGameListing.as[GameDetails]

      gameDetails.name must be (Some("Call of Duty Modern Warfare"))
      gameDetails.platform must be (Some("XBOX ONE"))
      gameDetails.releaseYear must be (Some("2019"))
      gameDetails.genre must be (Some("Action"))
    }

    "transform to PhoneDetails" in {
      val phoneDetails = mobilePhoneListing.as[PhoneDetails]

      phoneDetails.make must be (Some("Samsung"))
      phoneDetails.model must be (Some("Samsung Galaxy S10"))
      phoneDetails.storageCapacity must be (Some("128GB"))
      phoneDetails.network must be (Some("Unlocked"))
      phoneDetails.colour must be (Some("Rose Gold"))
      phoneDetails.condition must be (Some("USED"))
    }
  }
}
