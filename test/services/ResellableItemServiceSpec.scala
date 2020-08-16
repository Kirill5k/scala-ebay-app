package services

import cats.effect.IO
import domain.{ItemDetails, ResellableItemBuilder}
import org.mockito.ArgumentMatchersSugar
import org.mockito.scalatest.MockitoSugar
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike
import repositories.{ResellableItemRepository, VideoGameRepository}

class ResellableItemServiceSpec extends AsyncWordSpecLike with Matchers with MockitoSugar with ArgumentMatchersSugar {

  val videoGame = ResellableItemBuilder.videoGame("super mario 3")

  "A VideoGameService" should {
    "check if item is new" in {
     val repository = mock[VideoGameRepository]
      when(repository.existsByUrl(any)).thenReturn(IO.pure(true))

      val service = new VideoGameService(repository)

      val isNewResult = service.isNew(videoGame)

      isNewResult.unsafeToFuture().map { isNew =>
        verify(repository).existsByUrl(videoGame.listingDetails.url)
        isNew must be (false)
      }
    }

    "store item in db" in {
     val repository = mock[VideoGameRepository]
      when(repository.save(any)).thenReturn(IO.pure(()))
      val service = new VideoGameService(repository)

      val saveResult = service.save(videoGame)

      saveResult.unsafeToFuture().map { saved =>
        verify(repository).save(videoGame)
        saved must be (())
      }
    }

    "get latest items from db" in {
     val repository = mock[VideoGameRepository]
      when(repository.findAll(any, any, any)).thenReturn(IO.pure(List(videoGame)))
      val service = new VideoGameService(repository)

      val latestResult = service.get(Some(10), None, None)

      latestResult.unsafeToFuture().map { latest =>
        verify(repository).findAll(Some(10), None, None)
        latest must be (List(videoGame))
      }
    }
  }
}
