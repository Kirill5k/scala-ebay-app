package domain

import java.net.URI
import java.time.Instant

import domain.ItemDetails.PhoneDetails
import domain.ResellableItem.MobilePhone

object MobilePhoneBuilder {
  def build(make: String, model: String, colour: String, storage: String = "16GB", datePosted: Instant = Instant.now(), platform: String = "XBOX ONE"): MobilePhone =
    MobilePhone(
      PhoneDetails(Some(make), Some(model), Some(colour), Some(storage), Some("Unlocked"), Some("USED")),
      ListingDetails(
        s"https://www.ebay.co.uk/itm/$make-$model-$colour".toLowerCase.replaceAll(" ", "-"),
        s"$make $model $colour $storage",
        Some(s"$make $model $colour $storage. Condition is Used. Dispatched with Royal Mail 1st Class Small parcel."),
        None,
        Some("https://i.ebayimg.com/images/g/0kcAAOSw~5ReGFCQ/s-l1600.jpg"),
        List("FIXED_PRICE"),
        Some("168.robinhood"),
        BigDecimal.valueOf(99.99),
        "New",
        datePosted,
        None,
        Map()
      ),
      Some(ResellPrice(BigDecimal.valueOf(150), BigDecimal.valueOf(110)))
  )
}
