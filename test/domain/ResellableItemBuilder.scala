package domain

import java.time.Instant

import domain.ItemDetails.{Game, Phone}
import domain.ResellableItem.{MobilePhone, VideoGame}

object ResellableItemBuilder {

  def videoGame(
      name: String,
      datePosted: Instant = Instant.now(),
      platform: Option[String] = Some("XBOX ONE"),
      resellPrice: Option[ResellPrice] = Some(ResellPrice(BigDecimal.valueOf(100), BigDecimal.valueOf(80)))
  ): VideoGame =
    ResellableItem(
      Game(Some(name), platform, Some("2019"), Some("Action")),
      ListingDetails(
        s"https://www.ebay.co.uk/itm/$name".toLowerCase.replaceAll(" ", "-"),
        name,
        Some(
          s"$name xbox one 2019. Condition is New. Game came as part of bundle and not wanted. Never playes. Dispatched with Royal Mail 1st Class Large Letter."
        ),
        None,
        Some("https://i.ebayimg.com/images/g/0kcAAOSw~5ReGFCQ/s-l1600.jpg"),
        List("FIXED_PRICE"),
        Some("168.robinhood"),
        BigDecimal.valueOf(32.99),
        "New",
        datePosted,
        None,
        Map(
          "Game Name"    -> name,
          "Release Year" -> "2019",
          "Platform"     -> "Microsoft Xbox One",
          "Genre"        -> "Action"
        )
      ),
      resellPrice
    )

  def mobilePhone(
      make: String,
      model: String,
      colour: String,
      storage: String = "16GB",
      datePosted: Instant = Instant.now()
  ): MobilePhone =
    ResellableItem(
      Phone(Some(make), Some(model), Some(colour), Some(storage), Some("Unlocked"), Some("USED")),
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
