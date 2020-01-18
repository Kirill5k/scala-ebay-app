package clients.ebay

import cats.data.EitherT
import cats.implicits._
import clients.ebay.auth.EbayAuthClient
import clients.ebay.browse.EbayBrowseClient
import domain.ApiClientError
import domain.ApiClientError.FutureErrorOr
import org.mockito.captor.ArgCaptor
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class VideoGameSearchClientSpec extends PlaySpec with ScalaFutures with MockitoSugar with ArgumentMatchersSugar {
  import scala.concurrent.ExecutionContext.Implicits.global

  val accessToken = "access-token"

  "VideoGameSearchClient" should {

    "search for ps4, xbox one and switch games" in {
      val searchParamsCaptor = ArgCaptor[Map[String, String]]
      val (authClient, browseClient) = mockEbayClients
      val videoGameSearchClient = new VideoGameSearchClient(authClient, browseClient)

      when(authClient.accessToken).thenReturn(successResponse(accessToken))
      when(browseClient.search(any, any)).thenReturn(successResponse(Seq()))

      val response = videoGameSearchClient.getItemsListedInLastMinutes(15)

      whenReady(response.value, timeout(10 seconds), interval(500 millis)) { items =>
        items must be (Right(Seq()))
        verify(authClient, times(3)).accessToken
        verify(browseClient, times(3)).search(eqTo(accessToken), searchParamsCaptor)
        searchParamsCaptor.values.map(_("q")) must be (Seq("PS4", "XBOX ONE", "SWITCH"))
        searchParamsCaptor.value("limit") must be ("200")
        searchParamsCaptor.value("category_ids") must be ("139973")
        searchParamsCaptor.value("filter") must startWith ("conditionIds:%7B1000|1500|2000|2500|3000|4000|5000%7D,deliveryCountry:GB,price:[0..100],priceCurrency:GBP,itemLocationCountry:GB,buyingOptions:%7BFIXED_PRICE%7D,itemStartDate:[")
      }
    }
  }

  def mockEbayClients: (EbayAuthClient, EbayBrowseClient) = {
    val authClient = mock[EbayAuthClient]
    val browseClient = mock[EbayBrowseClient]
    (authClient, browseClient)
  }

  def successResponse[A](response: A): FutureErrorOr[A] = {
    EitherT.right[ApiClientError](Future(response))
  }
}
