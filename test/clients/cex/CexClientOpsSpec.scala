package clients.cex

import domain.{MobilePhoneBuilder, VideoGameBuilder}
import org.scalatest.{MustMatchers, WordSpec}

class CexClientOpsSpec extends WordSpec with MustMatchers {
  import CexClientOps._

  "A VideoGame" should {
    val game = VideoGameBuilder.build("super mario 3", platform = "SWITCH")

    "return search query string" in {
      val query = game.itemDetails.searchQuery
      query must be (Some("super mario 3 SWITCH"))
    }

    "return none is some of the parameters are missing" in {
      val query = game.itemDetails.copy(platform = None).searchQuery
      query must be (None)
    }
  }

  "A MobilePhone" should {
    val phone = MobilePhoneBuilder.build("apple", "iphone 6", "Space Grey")

    "return search query string" in {
      val query = phone.itemDetails.searchQuery
      query must be (Some("apple iphone 6 16GB Space Grey Unlocked"))
    }

    "return none is some of the parameters are missing" in {
      val query = phone.itemDetails.copy(model = None).searchQuery
      query must be (None)
    }
  }
}
