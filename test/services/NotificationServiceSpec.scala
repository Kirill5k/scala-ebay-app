package services

import cats.effect.IO
import clients.telegram.TelegramClient
import domain.VideoGameBuilder
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec

class NotificationServiceSpec extends AsyncWordSpec with Matchers with MockitoSugar with ArgumentMatchersSugar {

  val videoGame = VideoGameBuilder.build("super mario 3", platform = "SWITCH")

  "A TelegramNotificationService" should {

    "send notification message" in {
      val client = mock[TelegramClient]
      when(client.sendMessageToMainChannel(any[String])).thenReturn(IO.pure(()))
      val service = new TelegramNotificationService(client)

      val notificationResult = service.cheapItem(videoGame)

      notificationResult.unsafeToFuture().map {
        verify(client).sendMessageToMainChannel("""NEW "super mario 3 SWITCH" - ebay: £32.99, cex: £80(142%)/£100 https://www.ebay.co.uk/itm/super-mario-3""")
        _ must be (())
      }
    }
  }
}
