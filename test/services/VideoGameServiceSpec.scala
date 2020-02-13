package services

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import clients.cex.CexClient
import clients.ebay.VideoGameEbayClient
import clients.telegram.TelegramClient
import domain.VideoGameBuilder
import domain.ResellableItem.VideoGame
import fs2.Stream
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import repositories.VideoGameRepository


class VideoGameServiceSpec extends AnyWordSpec with Matchers with AsyncIOSpec with MockitoSugar with ArgumentMatchersSugar {
  import scala.concurrent.ExecutionContext.Implicits.global

  val videoGame = VideoGameBuilder.build("super mario 3")
  val videoGame2 = VideoGameBuilder.build("Battlefield 1", resellPrice = None)

  "VideoGameService" should {
    "return new items from ebay" in {
      val (repository, ebayClient, telegramClient, cexClient) = mockDependecies
      val searchResponse = List((videoGame.itemDetails, videoGame.listingDetails), (videoGame2.itemDetails, videoGame2.listingDetails))
      when(ebayClient.getItemsListedInLastMinutes(anyInt)).thenReturn(Stream.evalSeq(IO.pure(searchResponse)))
      when(cexClient.findResellPrice(videoGame.itemDetails)).thenReturn(IO.pure(videoGame.resellPrice))
      when(cexClient.findResellPrice(videoGame2.itemDetails)).thenReturn(IO.pure(None))

      val service = new VideoGameService(repository, ebayClient, telegramClient, cexClient)

      val latestItemsResponse = service.getLatestFromEbay(10)

      latestItemsResponse.compile.toList.asserting(_ must be (List(videoGame, videoGame2)))

      verify(ebayClient).getItemsListedInLastMinutes(10)
      verify(cexClient).findResellPrice(videoGame.itemDetails)
      verify(cexClient).findResellPrice(videoGame2.itemDetails)
    }

    "check if item is new" in {
      val (repository, ebayClient, telegramClient, cexClient) = mockDependecies
      when(repository.existsByUrl(any)).thenReturn(IO.pure(true))

      val service = new VideoGameService(repository, ebayClient, telegramClient, cexClient)

      val isNew = service.isNew(videoGame)

      isNew.asserting(_ must be (false))
      verify(repository).existsByUrl(videoGame.listingDetails.url)
    }

    "store item in db" in {
      val (repository, ebayClient, telegramClient, cexClient) = mockDependecies
      when(repository.save(any)).thenReturn(IO.pure(()))
      val service = new VideoGameService(repository, ebayClient, telegramClient, cexClient)

      val save = service.save(videoGame)

      save.asserting(_ must be (()))

      verify(repository).save(videoGame)
    }

    "get latest items from db" in {
      val (repository, ebayClient, telegramClient, cexClient) = mockDependecies
      when(repository.findAll(any)).thenReturn(IO.pure(List(videoGame)))
      val service = new VideoGameService(repository, ebayClient, telegramClient, cexClient)

      val latestResult = service.getLatest(10)

      latestResult.asserting(_ must be (List(videoGame)))
      verify(repository).findAll(10)
    }

    "send notification message" in {
      val (repository, ebayClient, telegramClient, cexClient) = mockDependecies
      when(telegramClient.sendMessageToMainChannel(any[VideoGame])).thenReturn(IO.pure(()))
      val service = new VideoGameService(repository, ebayClient, telegramClient, cexClient)

      val notificationResult = service.sendNotification(videoGame)

      notificationResult.asserting(_ must be (()))
      verify(telegramClient).sendMessageToMainChannel(videoGame)
    }
  }

  def mockDependecies: (VideoGameRepository, VideoGameEbayClient, TelegramClient, CexClient) = {
    val repository = mock[VideoGameRepository]
    val ebayClient = mock[VideoGameEbayClient]
    val telegramClient = mock[TelegramClient]
    val cexClient = mock[CexClient]
    (repository, ebayClient, telegramClient, cexClient)
  }
}
