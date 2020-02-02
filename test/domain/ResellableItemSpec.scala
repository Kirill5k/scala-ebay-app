package domain

import org.scalatest.{MustMatchers, WordSpec}

class ResellableItemSpec extends WordSpec with MustMatchers {
  import ResellableItemOps._

  "A VideoGame" should {
    val game = VideoGameBuilder.build("super mario 3", platform = "SWITCH")

    "return notification message string" in {
      val query = game.notificationMessage
      query must be (Some("""NEW "super mario 3 SWITCH" - ebay: £32.99, cex: £80(142%)/£100"""))
    }

    "return none if some of the item details are missing" in {
      val query = game.copy(itemDetails = game.itemDetails.copy(platform = None)).notificationMessage
      query must be (None)
    }

    "return none if resell price is missing" in {
      val query = game.copy(resellPrice = None).notificationMessage
      query must be (None)
    }
  }
}
