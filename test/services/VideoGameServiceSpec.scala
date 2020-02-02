package services

import cats.data.EitherT
import cats.implicits._
import clients.cex.CexClient
import clients.ebay.VideoGameEbayClient
import clients.telegram.TelegramClient
import domain.{ApiClientError, VideoGameBuilder}
import domain.ApiClientError.FutureErrorOr
import domain.ResellableItem.VideoGame
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import repositories.VideoGameRepository

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class VideoGameServiceSpec extends WordSpec with MustMatchers with ScalaFutures with MockitoSugar with ArgumentMatchersSugar {
  import scala.concurrent.ExecutionContext.Implicits.global

  val videoGame = VideoGameBuilder.build("super mario 3")

  "VideoGameService" should {
    "check if item is new" in {
      val (repository, ebayClient, telegramClient, cexClient) = mockDependecies
      when(repository.existsByUrl(any)).thenReturn(successResponse(true))

      val service = new VideoGameService(repository, ebayClient, telegramClient, cexClient)

      val result = service.isNew(videoGame)

      whenReady(result.value, timeout(10 seconds), interval(500 millis)) { isNew =>
        isNew must be (Right(false))
        verify(repository).existsByUrl(videoGame.listingDetails.url)
      }
    }

    "store item in db" in {
      val (repository, ebayClient, telegramClient, cexClient) = mockDependecies
      when(repository.save(any)).thenReturn(successResponse(()))
      val service = new VideoGameService(repository, ebayClient, telegramClient, cexClient)

      val result = service.save(videoGame)

      whenReady(result.value, timeout(10 seconds), interval(500 millis)) { value =>
        value must be (Right(()))
        verify(repository).save(videoGame)
      }
    }

    "get latest items from db" in {
      val (repository, ebayClient, telegramClient, cexClient) = mockDependecies
      when(repository.findAll(any)).thenReturn(successResponse(List(videoGame)))
      val service = new VideoGameService(repository, ebayClient, telegramClient, cexClient)

      val result = service.getLatest(10)

      whenReady(result.value, timeout(10 seconds), interval(500 millis)) { items =>
        items must be (Right(List(videoGame)))
        verify(repository).findAll(10)
      }
    }

    "send notification message" in {
      val (repository, ebayClient, telegramClient, cexClient) = mockDependecies
      when(telegramClient.sendMessageToMainChannel(any[VideoGame])).thenReturn(successResponse(()))
      val service = new VideoGameService(repository, ebayClient, telegramClient, cexClient)

      val result = service.sendNotification(videoGame)

      whenReady(result.value, timeout(10 seconds), interval(500 millis)) { value =>
        value must be (Right(()))
        verify(telegramClient).sendMessageToMainChannel(videoGame)
      }
    }
  }

  def mockDependecies: (VideoGameRepository, VideoGameEbayClient, TelegramClient, CexClient) = {
    val repository = mock[VideoGameRepository]
    val ebayClient = mock[VideoGameEbayClient]
    val telegramClient = mock[TelegramClient]
    val cexClient = mock[CexClient]
    (repository, ebayClient, telegramClient, cexClient)
  }

  def successResponse[A](response: A): FutureErrorOr[A] = {
    EitherT.right[ApiClientError](Future(response))
  }
}
