package services

import cats.effect.IO
import clients.telegram.TelegramClient
import domain.ItemDetails.GenericItemDetails
import domain.PurchasableItem.GenericPurchasableItem
import domain.{PurchasableItem, PurchasePrice, StockUpdate, StockUpdateType, VideoGameBuilder}
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec

class NotificationServiceSpec extends AsyncWordSpec with Matchers with MockitoSugar with ArgumentMatchersSugar {

  "A TelegramNotificationService" should {

    "send cheap item notification message" in {
      val client = mock[TelegramClient]
      when(client.sendMessageToMainChannel(any[String])).thenReturn(IO.pure(()))
      val service = new TelegramNotificationService(client)

      val videoGame = VideoGameBuilder.build("super mario 3", platform = "SWITCH")
      val notificationResult = service.cheapItem(videoGame)

      notificationResult.unsafeToFuture().map {
        verify(client).sendMessageToMainChannel("""NEW "super mario 3 SWITCH" - ebay: £32.99, cex: £80(142%)/£100 https://www.ebay.co.uk/itm/super-mario-3""")
        _ must be (())
      }
    }

    "stock update notification message" in {
      val client = mock[TelegramClient]
      when(client.sendMessageToSecondaryChannel(any[String])).thenReturn(IO.pure(()))
      val service = new TelegramNotificationService(client)

      val update = StockUpdate(
        StockUpdateType.PriceDrop(BigDecimal(100.0), BigDecimal(50.0)),
        GenericPurchasableItem(GenericItemDetails("macbook pro"), PurchasePrice(1, BigDecimal(50.0)))
      )
      val result = service.stockUpdate(update)
      result.unsafeToFuture().map {
        verify(client).sendMessageToSecondaryChannel("""STOCK UPDATE for macbook pro: Price has reduced from £100.0 to £50.0""")
        _ must be (())
      }
    }
  }
}
