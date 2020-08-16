package services

import cats.effect.IO
import clients.cex.CexClient
import clients.ebay.EbaySearchClient
import domain.{ItemDetails, ResellableItem, ResellableItemBuilder, SearchQuery}
import fs2.Stream
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec


class EbayDealsSearchServiceSpec extends AsyncWordSpec with Matchers with MockitoSugar with ArgumentMatchersSugar {

  val videoGame = ResellableItemBuilder.videoGame("super mario 3")
  val videoGame2 = ResellableItemBuilder.videoGame("Battlefield 1", resellPrice = None)

  "An EbayVideoGameSearchService" should {
    "return new items from ebay" in {
      val (ebayClient, cexClient) = mockDependecies
      val searchResponse = List(videoGame, videoGame2)

      when(ebayClient.findItemsListedInLastMinutes[ItemDetails.Game](any[SearchQuery], anyInt)(any, any))
        .thenReturn(Stream.evalSeq(IO.pure(searchResponse)))

      doReturn(IO.pure(videoGame.resellPrice))
        .doReturn(IO.pure(None))
        .when(cexClient).findResellPrice(any[SearchQuery])

      val service = new EbayVideoGameSearchService(ebayClient, cexClient)

      val latestItemsResponse = service.searchEbay(SearchQuery("xbox"), 10)

      latestItemsResponse.compile.toList.unsafeToFuture().map { items =>
        verify(ebayClient).findItemsListedInLastMinutes[ItemDetails.Game](SearchQuery("xbox"), 10)
        verify(cexClient, times(2)).findResellPrice(any[SearchQuery])
        items must be (List(videoGame, videoGame2))
      }
    }

    "leave resell price as None when not enough details for query" in {
      val (ebayClient, cexClient) = mockDependecies
      val itemDetails = videoGame.itemDetails.copy(platform = None)
      val searchResponse = List(videoGame.copy(itemDetails = itemDetails, resellPrice = None))

      when(ebayClient.findItemsListedInLastMinutes[ItemDetails.Game](any[SearchQuery], anyInt)(any, any))
        .thenReturn(Stream.evalSeq(IO.pure(searchResponse)))

      val service = new EbayVideoGameSearchService(ebayClient, cexClient)

      val latestItemsResponse = service.searchEbay(SearchQuery("xbox"), 10)

      latestItemsResponse.compile.toList.unsafeToFuture().map { items =>
        verify(ebayClient).findItemsListedInLastMinutes[ItemDetails.Game](SearchQuery("xbox"), 10)
        verify(cexClient, never).findResellPrice(any[SearchQuery])
        items must be (List(ResellableItem(itemDetails, videoGame.listingDetails, videoGame.price, None)))
      }
    }
  }

  def mockDependecies: (EbaySearchClient, CexClient) = {
    val ebayClient = mock[EbaySearchClient]
    val cexClient = mock[CexClient]
    (ebayClient, cexClient)
  }
}
