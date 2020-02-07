package controllers

import java.time.Instant

import cats.data.EitherT
import cats.implicits._
import domain.{ApiClientError, VideoGameBuilder}
import domain.ApiClientError.FutureErrorOr
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test.Helpers._
import play.api.test._
import services.VideoGameService

import scala.concurrent.Future


class VideoGameControllerSpec extends PlaySpec with MockitoSugar with ArgumentMatchersSugar {
  import scala.concurrent.ExecutionContext.Implicits.global

  val videoGame = VideoGameBuilder.build("super mario 3", datePosted = Instant.ofEpochMilli(1577836800000L))
  val videoGame2 = VideoGameBuilder.build("Battlefield 1", datePosted = Instant.ofEpochMilli(1577836800000L), resellPrice = None)

  "VideoGameController GET" should {

    "return list of video games" in {
      val service = mock[VideoGameService]
      when(service.getLatest(anyInt)).thenReturn(successResponse(List(videoGame, videoGame2)))

      val controller = new VideoGameController(service, stubControllerComponents())

      val itemsResponse = controller.getAll(10).apply(FakeRequest(GET, "/"))

      status(itemsResponse) mustBe OK
      contentType(itemsResponse) mustBe Some("application/json")
      contentAsString(itemsResponse) mustBe ("""[{"itemDetails":{"name":"super mario 3","platform":"XBOX ONE","releaseYear":"2019","genre":"Action"},"listingDetails":{"url":"https://www.ebay.co.uk/itm/super-mario-3","title":"super mario 3","shortDescription":"super mario 3 xbox one 2019. Condition is New. Game came as part of bundle and not wanted. Never playes. Dispatched with Royal Mail 1st Class Large Letter.","description":null,"image":"https://i.ebayimg.com/images/g/0kcAAOSw~5ReGFCQ/s-l1600.jpg","buyingOptions":["FIXED_PRICE"],"sellerName":"168.robinhood","price":32.99,"condition":"New","datePosted":"2020-01-01T00:00:00Z","dateEnded":null,"properties":{"Game Name":"super mario 3","Release Year":"2019","Platform":"Microsoft Xbox One","Genre":"Action"}},"resellPrice":{"cash":100,"exchange":80}},{"itemDetails":{"name":"Battlefield 1","platform":"XBOX ONE","releaseYear":"2019","genre":"Action"},"listingDetails":{"url":"https://www.ebay.co.uk/itm/battlefield-1","title":"Battlefield 1","shortDescription":"Battlefield 1 xbox one 2019. Condition is New. Game came as part of bundle and not wanted. Never playes. Dispatched with Royal Mail 1st Class Large Letter.","description":null,"image":"https://i.ebayimg.com/images/g/0kcAAOSw~5ReGFCQ/s-l1600.jpg","buyingOptions":["FIXED_PRICE"],"sellerName":"168.robinhood","price":32.99,"condition":"New","datePosted":"2020-01-01T00:00:00Z","dateEnded":null,"properties":{"Game Name":"Battlefield 1","Release Year":"2019","Platform":"Microsoft Xbox One","Genre":"Action"}},"resellPrice":null}]""")
    }
  }

  def successResponse[A](response: A): FutureErrorOr[A] = {
    EitherT.right[ApiClientError](Future(response))
  }
}
