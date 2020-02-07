package controllers

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


class VideoGameControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with MockitoSugar with ArgumentMatchersSugar {
  import scala.concurrent.ExecutionContext.Implicits.global

  val videoGame = VideoGameBuilder.build("super mario 3")
  val videoGame2 = VideoGameBuilder.build("Battlefield 1", resellPrice = None)

  "VideoGameController GET" should {

    "return list of video games" in {
      val service = mock[VideoGameService]
      when(service.getLatest(anyInt)).thenReturn(successResponse(List(videoGame, videoGame2)))

      val controller = new VideoGameController(service, stubControllerComponents())

      val itemsResponse = controller.getAll(10).apply(FakeRequest(GET, "/"))

      status(itemsResponse) mustBe OK
      contentType(itemsResponse) mustBe Some("application/json")
      contentAsString(itemsResponse) must include ("Welcome to Play")
    }
  }

  def successResponse[A](response: A): FutureErrorOr[A] = {
    EitherT.right[ApiClientError](Future(response))
  }
}
