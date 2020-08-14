package services

import domain.{VideoGameBuilder}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec


class ResellableOpsSpec extends AnyWordSpec with Matchers {
  import NotificationService._

  "A VideoGame" should {
    val game = VideoGameBuilder.build("super mario 3", platform = "SWITCH")

    "return notification message string" in {
      val query = game.notificationMessage
      query must be (Some("""NEW "super mario 3 SWITCH" - ebay: £32.99, cex: £80(142%)/£100 https://www.ebay.co.uk/itm/super-mario-3"""))
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
