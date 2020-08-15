package services

import cats.effect.IO
import clients.cex.CexClient
import domain.{PurchasableItemBuilder, SearchQuery, StockUpdate, StockUpdateType}
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec

class GenericPurchasableItemServiceSpec extends AsyncWordSpec with Matchers with MockitoSugar with ArgumentMatchersSugar {

  val query = SearchQuery("macbook")

  val mb1 = PurchasableItemBuilder.generic("Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/A", 2, 1950.0)
  val mb2 = PurchasableItemBuilder.generic("Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/B")

  "A GenericPurchasableItemService" should {

    "return empty list if cache is empty" in {
      val cexMock = mock[CexClient]
      val service = new GenericPurchasableItemService(cexMock)

      when(cexMock.getCurrentStock(query)).thenReturn(IO.pure(List(mb1, mb2)))

      val result = service.getStockUpdatesFromCex(query)

      result.unsafeToFuture().map { u =>
        verify(cexMock).getCurrentStock(query)
        service.searchHistory.contains(query)
        service.cache.get("Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/A") must be (mb1)
        service.cache.get("Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/B") must be (mb2)
        u must be (Nil)
      }
    }

    "return new in stock update if cache previously had some items" in {
      val cexMock = mock[CexClient]
      val service = new GenericPurchasableItemService(cexMock)
      service.searchHistory.add(query)
      service.cache.put("Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/A", mb1)
      when(cexMock.getCurrentStock(query)).thenReturn(IO.pure(List(mb1, mb2)))

      val result = service.getStockUpdatesFromCex(query)

      result.unsafeToFuture().map { u =>
        service.cache.get("Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/A") must be (mb1)
        service.cache.get("Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/B") must be (mb2)
        u must be (List(StockUpdate(StockUpdateType.New, mb2)))
      }
    }

    "return empty if no changes" in {
      val cexMock = mock[CexClient]
      val service = new GenericPurchasableItemService(cexMock)
      service.searchHistory.add(query)
      service.cache.put("Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/A", mb1)
      when(cexMock.getCurrentStock(query)).thenReturn(IO.pure(List(mb1)))

      val result = service.getStockUpdatesFromCex(query)

      result.unsafeToFuture().map { u =>
        service.cache.get("Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/A") must be (mb1)
        u must be (Nil)
      }
    }

    "return stock increase update if quantity increase" in {
      val cexMock = mock[CexClient]
      val service = new GenericPurchasableItemService(cexMock)
      service.searchHistory.add(query)
      service.cache.put(
        "Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/A",
        PurchasableItemBuilder.generic("Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/A", price = 1950.0)
      )
      when(cexMock.getCurrentStock(query)).thenReturn(IO.pure(List(mb1)))

      val result = service.getStockUpdatesFromCex(query)

      result.unsafeToFuture().map { u =>
        service.cache.get("Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/A") must be (mb1)
        u must be (List(StockUpdate(StockUpdateType.StockIncrease(1, 2), mb1)))
      }
    }

    "return stock decrease update if quantity decreased" in {
      val cexMock = mock[CexClient]
      val service = new GenericPurchasableItemService(cexMock)
      service.searchHistory.add(query)
      service.cache.put(
        "Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/A",
        PurchasableItemBuilder.generic("Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/A", 3, 1950.0)
      )
      when(cexMock.getCurrentStock(query)).thenReturn(IO.pure(List(mb1)))

      val result = service.getStockUpdatesFromCex(query)

      result.unsafeToFuture().map { u =>
        service.cache.get("Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/A") must be (mb1)
        u must be (List(StockUpdate(StockUpdateType.StockDecrease(3, 2), mb1)))
      }
    }

    "return price increase update if price increase" in {
      val cexMock = mock[CexClient]
      val service = new GenericPurchasableItemService(cexMock)
      service.searchHistory.add(query)
      service.cache.put(
        "Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/A",
        PurchasableItemBuilder.generic("Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/A", 2, 950.0)
      )
      when(cexMock.getCurrentStock(query)).thenReturn(IO.pure(List(mb1)))

      val result = service.getStockUpdatesFromCex(query)

      result.unsafeToFuture().map { u =>
        service.cache.get("Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/A") must be (mb1)
        u must be (List(StockUpdate(StockUpdateType.PriceRaise(BigDecimal(950.0), BigDecimal(1950.0)), mb1)))
      }
    }

    "return price drop update if price decrease" in {
      val cexMock = mock[CexClient]
      val service = new GenericPurchasableItemService(cexMock)
      service.searchHistory.add(query)
      service.cache.put(
        "Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/A",
        PurchasableItemBuilder.generic("Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/A", 2, 2950.0)
      )
      when(cexMock.getCurrentStock(query)).thenReturn(IO.pure(List(mb1)))

      val result = service.getStockUpdatesFromCex(query)

      result.unsafeToFuture().map { u =>
        service.cache.get("Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/A") must be (mb1)
        u must be (List(StockUpdate(StockUpdateType.PriceDrop(BigDecimal(2950.0), BigDecimal(1950.0)), mb1)))
      }
    }
  }
}
