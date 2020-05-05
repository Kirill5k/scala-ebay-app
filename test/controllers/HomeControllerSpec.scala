package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test._
import play.api.test.Helpers._

class HomeControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  override def fakeApplication = new GuiceApplicationBuilder()
    .configure("ebay.credentials" -> List())
    .configure("telegram.botKey" -> "bot-1")
    .configure("telegram.mainChannelId" -> "c-main")
    .configure("telegram.secondaryChannelId" -> "c-secondary")
    .configure("mongodb.uri" -> "mongodb://localhost:12345/mongo-test")
    .build()

  "HomeController GET" should {

    "render the index page from a new instance of controller" in {
      val controller = new HomeController(stubControllerComponents())
      val home = controller.index().apply(FakeRequest(GET, "/"))

      status(home) mustBe OK
      contentType(home) mustBe Some("text/html")
      contentAsString(home) must include ("Scala eBay app")
    }

    "render the index page from the application" in {
      val controller = inject[HomeController]
      val home = controller.index().apply(FakeRequest(GET, "/"))

      status(home) mustBe OK
      contentType(home) mustBe Some("text/html")
      contentAsString(home) must include ("Scala eBay app")
    }

    "render the index page from the router" in {
      val request = FakeRequest(GET, "/")
      val home = route(app, request).get

      status(home) mustBe OK
      contentType(home) mustBe Some("text/html")
      contentAsString(home) must include ("Scala eBay app")
    }
  }
}
