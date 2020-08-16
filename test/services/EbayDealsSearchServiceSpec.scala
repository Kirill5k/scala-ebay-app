package services

import cats.effect.IO
import clients.cex.CexClient
import clients.ebay.VideoGameEbayClient
import domain.{ResellableItem, ResellableItemBuilder, SearchQuery}
import fs2.Stream
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import repositories.VideoGameRepository

class EbayDealsSearchServiceSpec extends AsyncWordSpec with Matchers with MockitoSugar with ArgumentMatchersSugar {

  val videoGame = ResellableItemBuilder.videoGame("super mario 3")
  val videoGame2 = ResellableItemBuilder.videoGame("Battlefield 1", resellPrice = None)

  "An EbayVideoGameSearchService" should {
    "return new items from ebay" in {
      val (repository, ebayClient, cexClient) = mockDependecies
      val searchResponse = List(videoGame, videoGame2)

      when(ebayClient.findItemsListedInLastMinutes(any[SearchQuery], anyInt)).thenReturn(Stream.evalSeq(IO.pure(searchResponse)))

      doReturn(IO.pure(videoGame.resellPrice))
        .doReturn(IO.pure(None))
        .when(cexClient).findResellPrice(any[SearchQuery])

      val service = new EbayVideoGameSearchService(repository, ebayClient, cexClient)

      val latestItemsResponse = service.searchEbay(SearchQuery("xbox"), 10)

      latestItemsResponse.compile.toList.unsafeToFuture().map { items =>
        verify(ebayClient).findItemsListedInLastMinutes(SearchQuery("xbox"), 10)
        verify(cexClient, times(2)).findResellPrice(any[SearchQuery])
        items must be (List(videoGame, videoGame2))
      }
    }

    "leave resell price as None when not enough details for query" in {
      val (repository, ebayClient, cexClient) = mockDependecies
      val itemDetails = videoGame.itemDetails.copy(platform = None)
      val searchResponse = List(videoGame.copy(itemDetails = itemDetails, resellPrice = None))

      when(ebayClient.findItemsListedInLastMinutes(any[SearchQuery], anyInt)).thenReturn(Stream.evalSeq(IO.pure(searchResponse)))

      val service = new EbayVideoGameSearchService(repository, ebayClient, cexClient)

      val latestItemsResponse = service.searchEbay(SearchQuery("xbox"), 10)

      latestItemsResponse.compile.toList.unsafeToFuture().map { items =>
        verify(ebayClient).findItemsListedInLastMinutes(SearchQuery("xbox"), 10)
        verify(cexClient, never).findResellPrice(any[SearchQuery])
        items must be (List(ResellableItem(itemDetails, videoGame.listingDetails, videoGame.price, None)))
      }
    }

    "check if item is new" in {
      val (repository, ebayClient, cexClient) = mockDependecies
      when(repository.existsByUrl(any)).thenReturn(IO.pure(true))

      val service = new EbayVideoGameSearchService(repository, ebayClient, cexClient)

      val isNewResult = service.isNew(videoGame)

      isNewResult.unsafeToFuture().map { isNew =>
        verify(repository).existsByUrl(videoGame.listingDetails.url)
        isNew must be (false)
      }
    }

    "store item in db" in {
      val (repository, ebayClient, cexClient) = mockDependecies
      when(repository.save(any)).thenReturn(IO.pure(()))
      val service = new EbayVideoGameSearchService(repository, ebayClient, cexClient)

      val saveResult = service.save(videoGame)

      saveResult.unsafeToFuture().map { saved =>
        verify(repository).save(videoGame)
        saved must be (())
      }
    }

    "get latest items from db" in {
      val (repository, ebayClient, cexClient) = mockDependecies
      when(repository.findAll(any, any, any)).thenReturn(IO.pure(List(videoGame)))
      val service = new EbayVideoGameSearchService(repository, ebayClient, cexClient)

      val latestResult = service.get(Some(10), None, None)

      latestResult.unsafeToFuture().map { latest =>
        verify(repository).findAll(Some(10), None, None)
        latest must be (List(videoGame))
      }
    }
  }

  def mockDependecies: (VideoGameRepository, VideoGameEbayClient, CexClient) = {
    val repository = mock[VideoGameRepository]
    val ebayClient = mock[VideoGameEbayClient]
    val cexClient = mock[CexClient]
    (repository, ebayClient, cexClient)
  }
}
