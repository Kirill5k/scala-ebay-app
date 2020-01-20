package clients.ebay.mappers

import java.net.URI
import java.time.Instant

import domain.ListingDetails
import org.scalatest._

class PhoneDetailsMapperSpec extends WordSpec with MustMatchers {

  val testListing = ListingDetails(
    new URI("https://www.ebay.co.uk/itm/Call-of-Duty-Modern-Warfare-Xbox-One-/274204760218"),
    "Samsung Galaxy S10 128gb UNLOCKED Prism Blue",
    Some("Samsung Galaxy S10 Used"),
    Some("Up For GrabsSamsung Galaxy S10 128gb UNLOCKED Prism BlueGood ConditionThe usual minor wear and Tear as you would expect from a used phone.It has been in a case with a screen protector since new however they appears tohave 1 x Deeper Scratch no more than 1cm long to the top left of the phone which does not affect the use of the phone nor does it show up when the screen is in use and you have got to look for it to see it when the screen is off.Comes with Wall Plug and Wire.I like the phone but unf"),
    new URI("https://i.ebayimg.com/images/g/yOMAAOSw~5ReGEH2/s-l1600.jpg"),
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

  "PhoneDetailsMapper" should {

    "get details from properties if they are present" in {
      val listingDetails = testListing.copy()

      val phoneDetails = PhoneDetailsMapper.from(listingDetails)

      phoneDetails.make must be (Some("Samsung"))
      phoneDetails.model must be (Some("Samsung Galaxy S10"))
      phoneDetails.storageCapacity must be (Some("128GB"))
      phoneDetails.network must be (Some("Unlocked"))
      phoneDetails.colour must be (Some("Rose Gold"))
      phoneDetails.condition must be (Some("USED"))
    }

    "leave network as unlocked if it unrecognized" in {
      val listingDetails = testListing.copy(properties = Map("Network" -> "Tele 2"))

      val phoneDetails = PhoneDetailsMapper.from(listingDetails)

      phoneDetails.network must be (Some("Unlocked"))
    }

    "replace colour Gray with Grey" in {
      val listingDetails = testListing.copy(properties = Map("Manufacturer Colour" -> "Gray"))

      val phoneDetails = PhoneDetailsMapper.from(listingDetails)

      phoneDetails.colour must be (Some("Grey"))
    }

    "leave storage capacity empty if it is in MB" in {
      val listingDetails = testListing.copy(properties = Map("Storage Capacity" -> "128 MB"))

      val phoneDetails = PhoneDetailsMapper.from(listingDetails)

      phoneDetails.storageCapacity must be (None)
    }

    "map colour from Colour property if manufacture colour is missing" in {
      val listingDetails = testListing.copy(properties = Map("Colour" -> "Blue Topaz"))

      val phoneDetails = PhoneDetailsMapper.from(listingDetails)

      phoneDetails.colour must be (Some("Blue"))
    }

    val faultyDescriptions = List(
      "blah blah has a crack blah",
      "blah blah no touchid blah blah",
      "no touchid",
      "has cracked screen",
      "bla bla touch id doesn't work blah blah",
      "bla bla touch id doesnt work blah blah",
      "bla bla touch id can't work blah blah",
      "blah blah screen is cracked blah blah",
      "blah blah there is a crack blah blah",
      "blah spares/repairs blah",
      "please read \nneeds new screen!. Condition is Used. Dispatched with Royal Mail 1st Class.\n<br>"
    )

    "detect if phone is faulty base on description" in {
      for (description <- faultyDescriptions) {
        val listingDetails = testListing.copy(description = Some(description), shortDescription = None)

        val phoneDetails = PhoneDetailsMapper.from(listingDetails)

        phoneDetails.condition must be (Some("Faulty"))
      }
    }

    "detect if phone is faulty base on short description" in {
      for (description <- faultyDescriptions) {
        val listingDetails = testListing.copy(shortDescription = Some(description), description = None)

        val phoneDetails = PhoneDetailsMapper.from(listingDetails)

        phoneDetails.condition must be (Some("Faulty"))
      }
    }

    val faultyTitles = List(
      "good but smashed",
      "galaxy s8 smashed screen",
      "iphone for spares repairs",
      "iphone with Cracked Screen",
      "blah spares/repairs blah"
    )

    "detect if phone is faulty based on title" in {
      for (title <- faultyTitles) {
        val listingDetails = testListing.copy(title = title)

        val phoneDetails = PhoneDetailsMapper.from(listingDetails)

        phoneDetails.condition must be (Some("Faulty"))
      }
    }
  }
}
