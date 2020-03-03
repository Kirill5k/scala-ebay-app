package controllers

import java.time.Instant

import cats.effect.IO
import domain.ApiClientError.HttpError
import domain.{ResellPrice, VideoGameBuilder}
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatestplus.play._
import play.api.test.Helpers._
import play.api.test._
import services.VideoGameService


class VideoGameControllerSpec extends PlaySpec with MockitoSugar with ArgumentMatchersSugar {
  import scala.concurrent.ExecutionContext.Implicits.global

  val videoGame = VideoGameBuilder.build("super mario 3", datePosted = Instant.ofEpochMilli(1577836800000L))
  val videoGame2 = VideoGameBuilder.build("Battlefield 1", datePosted = Instant.ofEpochMilli(1577836800000L), resellPrice = None)
  val videoGame3 = VideoGameBuilder.build("Battlefield 1", datePosted = Instant.ofEpochMilli(1577836800000L), resellPrice = Some(ResellPrice(BigDecimal.valueOf(10), BigDecimal.valueOf(5))))

  "VideoGameController GET" should {

    "return list of video games" in {
      val service = mock[VideoGameService]
      when(service.getLatest(any, any, any)).thenReturn(IO.pure(List(videoGame, videoGame2)))

      val controller = new VideoGameController(service, stubControllerComponents())

      val from = Instant.now()
      val to = Instant.now().plusSeconds(100)
      val itemsResponse = controller.getAll(Some(100), Some(from), Some(to)).apply(FakeRequest())

      status(itemsResponse) mustBe OK
      contentType(itemsResponse) mustBe Some("application/json")
      contentAsString(itemsResponse) mustBe ("""[{"itemDetails":{"name":"super mario 3","platform":"XBOX ONE","releaseYear":"2019","genre":"Action"},"listingDetails":{"url":"https://www.ebay.co.uk/itm/super-mario-3","title":"super mario 3","shortDescription":"super mario 3 xbox one 2019. Condition is New. Game came as part of bundle and not wanted. Never playes. Dispatched with Royal Mail 1st Class Large Letter.","description":null,"image":"https://i.ebayimg.com/images/g/0kcAAOSw~5ReGFCQ/s-l1600.jpg","buyingOptions":["FIXED_PRICE"],"sellerName":"168.robinhood","price":32.99,"condition":"New","datePosted":"2020-01-01T00:00:00Z","dateEnded":null,"properties":{"Game Name":"super mario 3","Release Year":"2019","Platform":"Microsoft Xbox One","Genre":"Action"}},"resellPrice":{"cash":100,"exchange":80}},{"itemDetails":{"name":"Battlefield 1","platform":"XBOX ONE","releaseYear":"2019","genre":"Action"},"listingDetails":{"url":"https://www.ebay.co.uk/itm/battlefield-1","title":"Battlefield 1","shortDescription":"Battlefield 1 xbox one 2019. Condition is New. Game came as part of bundle and not wanted. Never playes. Dispatched with Royal Mail 1st Class Large Letter.","description":null,"image":"https://i.ebayimg.com/images/g/0kcAAOSw~5ReGFCQ/s-l1600.jpg","buyingOptions":["FIXED_PRICE"],"sellerName":"168.robinhood","price":32.99,"condition":"New","datePosted":"2020-01-01T00:00:00Z","dateEnded":null,"properties":{"Game Name":"Battlefield 1","Release Year":"2019","Platform":"Microsoft Xbox One","Genre":"Action"}},"resellPrice":null}]""")
      verify(service).getLatest(Some(100), Some(from), Some(to))
    }

    "return summary of video games" in {
      val service = mock[VideoGameService]
      when(service.getLatest(any, any, any)).thenReturn(IO.pure(List(videoGame, videoGame2, videoGame3)))

      val controller = new VideoGameController(service, stubControllerComponents())

      val from = Instant.now()
      val to = Instant.now().plusSeconds(100)
      val itemsResponse = controller.summary(Some(from), Some(to)).apply(FakeRequest(GET, "/summary"))

      status(itemsResponse) mustBe OK
      contentType(itemsResponse) mustBe Some("application/json")
      contentAsString(itemsResponse) mustBe ("""{"total":3,"unrecognized":{"total":1,"items":[{"name":"Battlefield 1 XBOX ONE","url":"https://www.ebay.co.uk/itm/battlefield-1"}]},"profitable":{"total":1,"items":[{"name":"super mario 3 XBOX ONE","url":"https://www.ebay.co.uk/itm/super-mario-3"}]},"_type":"ResellableItemsSummaryResponse"}""")
      verify(service).getLatest(None, Some(from), Some(to))
    }

    "return error" in {
      val service = mock[VideoGameService]
      when(service.getLatest(any, any, any)).thenReturn(IO.raiseError(HttpError(400, "bad request")))

      val controller = new VideoGameController(service, stubControllerComponents())

      val itemsResponse = controller.getAll(None, None, None).apply(FakeRequest())

      status(itemsResponse) mustBe INTERNAL_SERVER_ERROR
      contentType(itemsResponse) mustBe Some("application/json")
      contentAsString(itemsResponse) mustBe ("""{"message":"bad request"}""")
      verify(service).getLatest(None, None, None)
    }
  }
}
