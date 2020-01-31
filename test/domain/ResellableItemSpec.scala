package domain

import org.scalatest.{MustMatchers, WordSpec}

class ResellableItemSpec extends WordSpec with MustMatchers {

  "A VideoGame" should {
    val game = VideoGameBuilder.build("super mario 3", platform = "SWITCH")

    "return search query string" in {
      val query = game.searchQuery
      query must be (Some("super mario 3 SWITCH"))
    }

    "return none is some of the parameters are missing" in {
      val query = game.copy(itemDetails = game.itemDetails.copy(platform = None)).searchQuery
      query must be (None)
    }
  }

  "A MobilePhone" should {
    val phone = MobilePhoneBuilder.build("apple", "iphone 6", "Space Grey")

    "return search query string" in {
      val query = phone.searchQuery
      query must be (Some("apple iphone 6 16GB Space Grey Unlocked"))
    }

    "return none is some of the parameters are missing" in {
      val query = phone.copy(itemDetails = phone.itemDetails.copy(model = None)).searchQuery
      query must be (None)
    }
  }
}
