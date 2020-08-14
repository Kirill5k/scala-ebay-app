package services

import cats.effect.IO
import clients.cex.CexClient
import clients.ebay.VideoGameEbayClient
import clients.telegram.TelegramClient
import domain.{SearchQuery, VideoGameBuilder}
import domain.ResellableItem.VideoGame
import fs2.Stream
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import repositories.VideoGameRepository

import scala.concurrent.duration._
import scala.language.postfixOps

class VideoGameServiceSpec extends AnyWordSpec with Matchers with ScalaFutures with MockitoSugar with ArgumentMatchersSugar {
  import scala.concurrent.ExecutionContext.Implicits.global

  val videoGame = VideoGameBuilder.build("super mario 3")
  val videoGame2 = VideoGameBuilder.build("Battlefield 1", resellPrice = None)

  "VideoGameService" should {
    "return new items from ebay" in {
      val (repository, ebayClient, cexClient) = mockDependecies
      val searchResponse = List((videoGame.itemDetails, videoGame.listingDetails), (videoGame2.itemDetails, videoGame2.listingDetails))
      when(ebayClient.findItemsListedInLastMinutes(any[SearchQuery], anyInt)).thenReturn(Stream.evalSeq(IO.pure(searchResponse)))
      when(cexClient.findResellPrice(videoGame.itemDetails)).thenReturn(IO.pure(videoGame.resellPrice))
      when(cexClient.findResellPrice(videoGame2.itemDetails)).thenReturn(IO.pure(None))

      val service = new VideoGameService(repository, ebayClient, cexClient)

      val latestItemsResponse = service.searchEbay(SearchQuery("xbox"), 10)

      whenReady(latestItemsResponse.compile.toList.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { items =>
        items must be (List(videoGame, videoGame2))
        verify(ebayClient).findItemsListedInLastMinutes(SearchQuery("xbox"), 10)
        verify(cexClient).findResellPrice(videoGame.itemDetails)
        verify(cexClient).findResellPrice(videoGame2.itemDetails)
      }
    }

    "check if item is new" in {
      val (repository, ebayClient, cexClient) = mockDependecies
      when(repository.existsByUrl(any)).thenReturn(IO.pure(true))

      val service = new VideoGameService(repository, ebayClient, cexClient)

      val isNewResult = service.isNew(videoGame)

      whenReady(isNewResult.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { isNew =>
        isNew must be (false)
        verify(repository).existsByUrl(videoGame.listingDetails.url)
      }
    }

    "store item in db" in {
      val (repository, ebayClient, cexClient) = mockDependecies
      when(repository.save(any)).thenReturn(IO.pure(()))
      val service = new VideoGameService(repository, ebayClient, cexClient)

      val saveResult = service.save(videoGame)

      whenReady(saveResult.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { saved =>
        saved must be (())
        verify(repository).save(videoGame)
      }
    }

    "get latest items from db" in {
      val (repository, ebayClient, cexClient) = mockDependecies
      when(repository.findAll(any, any, any)).thenReturn(IO.pure(List(videoGame)))
      val service = new VideoGameService(repository, ebayClient, cexClient)

      val latestResult = service.get(Some(10), None, None)

      whenReady(latestResult.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { latest =>
        latest must be (List(videoGame))
        verify(repository).findAll(Some(10), None, None)
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
