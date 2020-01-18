package clients.ebay

import cats.data.EitherT
import cats.implicits._
import clients.ebay.auth.EbayAuthClient
import clients.ebay.browse.EbayBrowseClient
import clients.ebay.browse.EbayBrowseResponse.{EbayItem, EbayItemSummary, ItemImage, ItemPrice, ItemProperty, ItemSeller}
import domain.ApiClientError
import domain.ApiClientError.{AuthError, FutureErrorOr, HttpError}
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
        searchParamsCaptor.values.map(_("q")) must contain allOf ("PS4", "XBOX ONE", "SWITCH")
        searchParamsCaptor.value("limit") must be ("200")
        searchParamsCaptor.value("category_ids") must be ("139973")
        searchParamsCaptor.value("filter") must startWith ("conditionIds:%7B1000|1500|2000|2500|3000|4000|5000%7D,deliveryCountry:GB,price:[0..100],priceCurrency:GBP,itemLocationCountry:GB,buyingOptions:%7BFIXED_PRICE%7D,itemStartDate:[")
      }
    }

    "switch ebay account on autherror" in {
      val (authClient, browseClient) = mockEbayClients
      val videoGameSearchClient = new VideoGameSearchClient(authClient, browseClient)

      when(authClient.accessToken).thenReturn(successResponse(accessToken))
      when(browseClient.search(any, any)).thenReturn(errorResponse(AuthError("Too many requests")))

      val response = videoGameSearchClient.getItemsListedInLastMinutes(15)

      whenReady(response.value, timeout(10 seconds), interval(500 millis)) { items =>
        items must be (Left(AuthError("Too many requests")))
        verify(authClient, times(3)).accessToken
        verify(authClient, times(1)).switchAccount
        verify(browseClient, times(3)).search(eqTo(accessToken), anyMap[String, String])
      }
    }

    "return api client error on failure" in {
      val (authClient, browseClient) = mockEbayClients
      val videoGameSearchClient = new VideoGameSearchClient(authClient, browseClient)

      when(authClient.accessToken).thenReturn(successResponse(accessToken))

      doReturn(errorResponse(HttpError(400, "Bad request")))
        .doReturn(successResponse(ebayItemSummaries("item-1", "item-2")))
        .doReturn(successResponse(ebayItemSummaries("item-3", "item-4")))
        .when(browseClient).search(any, any)

      val response = videoGameSearchClient.getItemsListedInLastMinutes(15)

      whenReady(response.value, timeout(10 seconds), interval(500 millis)) { items =>
        items must be (Left(HttpError(400, "Bad request")))
        verify(authClient, times(3)).accessToken
        verify(browseClient, times(3)).search(eqTo(accessToken), anyMap[String, String])
        verify(browseClient, never).getItem(any, any)
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

  def errorResponse[A](error: ApiClientError): FutureErrorOr[A] = {
    EitherT.left[A](Future(error))
  }

  def ebayItemSummaries(ids: String*): Seq[EbayItemSummary] = {
    ids.map(id => EbayItemSummary(id, "ebay item", ItemPrice(BigDecimal.valueOf(30.00), "GBP"), ItemSeller("168.robinhood", Some(100), Some(150))))
  }

  def ebayItem: EbayItem =
    EbayItem(
      "item-1",
      "call of duty modern warfare xbox one 2019",
      Some("call of duty modern warfare xbox one 2019. Condition is New. Game came as part of bundle and not wanted. Never playes. Dispatched with Royal Mail 1st Class Large Letter."),
      None,
      "Video Games & Consoles|Video Games",
      ItemPrice(BigDecimal.valueOf(30.00), "GBP"),
      "New",
      ItemImage("https://i.ebayimg.com/images/g/0kcAAOSw~5ReGFCQ/s-l1600.jpg"),
      ItemSeller("168.robinhood", Some(100), Some(150)),
      Seq(
        ItemProperty("Game Name", "Call of Duty: Modern Warfare"),
        ItemProperty("Release Year", "2019"),
        ItemProperty("Platform", "Microsoft Xbox One"),
        ItemProperty("Genre", "Action"),
      ),
      Seq("FIXED_PRICE"),
      "https://www.ebay.co.uk/itm/call-of-duty-modern-warfare-xbox-one-2019-/333474293066",
      None,
      None,
      None,
      None
    )
}
