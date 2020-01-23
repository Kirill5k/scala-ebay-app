package clients.ebay

import cats.data.EitherT
import cats.implicits._
import clients.ebay.auth.EbayAuthClient
import clients.ebay.browse.EbayBrowseClient
import clients.ebay.browse.EbayBrowseResponse.{EbayItem, EbayItemSummary, ItemImage, ItemPrice, ItemProperty, ItemSeller, ItemShippingOption, ShippingCost}
import domain.ApiClientError
import domain.ApiClientError.{AuthError, FutureErrorOr, HttpError}
import domain.ItemDetails.GameDetails
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

    "filter out items with bad feedback" in {
      val (authClient, browseClient) = mockEbayClients
      val videoGameSearchClient = new VideoGameSearchClient(authClient, browseClient)

      doReturn(successResponse(Seq(ebayItemSummary("1", feedbackPercentage = 90), ebayItemSummary("1", feedbackScore = 4))))
        .doReturn(successResponse(Seq()))
        .doReturn(successResponse(Seq()))
        .when(browseClient).search(any, any)

      val response = videoGameSearchClient.getItemsListedInLastMinutes(15)

      whenReady(response.value, timeout(10 seconds), interval(500 millis)) { items =>
        items must be (Right(Seq()))
        verify(authClient, times(3)).accessToken
        verify(browseClient, times(3)).search(eqTo(accessToken), anyMap[String, String])
        verify(browseClient, never).getItem(any, any)
      }
    }

    "filter out items with bad names" in {
      val (authClient, browseClient) = mockEbayClients
      val videoGameSearchClient = new VideoGameSearchClient(authClient, browseClient)

      doReturn(successResponse(Seq(
        ebayItemSummary("1", name = "fallout 4 disc only"),
        ebayItemSummary("2", name = "fallout 76 blah blah blah blah blah"),
        ebayItemSummary("3", name = "call of duty digital code"),
        ebayItemSummary("4", name = "lego worlds read description")
      )))
        .doReturn(successResponse(Seq()))
        .doReturn(successResponse(Seq()))
        .when(browseClient).search(any, any)

      val response = videoGameSearchClient.getItemsListedInLastMinutes(15)

      whenReady(response.value, timeout(10 seconds), interval(500 millis)) { items =>
        items must be (Right(Seq()))
        verify(authClient, times(3)).accessToken
        verify(browseClient, times(3)).search(eqTo(accessToken), anyMap[String, String])
        verify(browseClient, never).getItem(any, any)
      }
    }

    "get item details for each item id" in {
      val (authClient, browseClient) = mockEbayClients
      val videoGameSearchClient = new VideoGameSearchClient(authClient, browseClient)

      doReturn(successResponse(ebayItemSummaries("item-1")))
        .doReturn(successResponse(ebayItemSummaries("item-2")))
        .doReturn(successResponse(ebayItemSummaries("item-3")))
        .when(browseClient).search(any, any)

      doReturn(successResponse(None)).when(browseClient).getItem(accessToken, "item-1")
      doReturn(successResponse(None)).when(browseClient).getItem(accessToken, "item-2")
      doReturn(successResponse(Some(ebayItem))).when(browseClient).getItem(accessToken, "item-3")

      val response = videoGameSearchClient.getItemsListedInLastMinutes(15)

      whenReady(response.value, timeout(10 seconds), interval(500 millis)) { items =>
        val listings = items.getOrElse(throw new RuntimeException())
        listings must have size (1)
        listings.head._1 must be (GameDetails(Some("Call of Duty Modern Warfare"), Some("XBOX ONE"), Some("2019"), Some("Action")))
        verify(authClient, times(6)).accessToken
        verify(browseClient, times(3)).search(eqTo(accessToken), anyMap[String, String])
        verify(browseClient, times(3)).getItem(eqTo(accessToken), any)
        videoGameSearchClient
      }
    }
  }

  def mockEbayClients: (EbayAuthClient, EbayBrowseClient) = {
    val authClient = mock[EbayAuthClient]
    val browseClient = mock[EbayBrowseClient]
    when(authClient.accessToken).thenReturn(successResponse(accessToken))
    (authClient, browseClient)
  }

  def successResponse[A](response: A): FutureErrorOr[A] = {
    EitherT.right[ApiClientError](Future(response))
  }

  def errorResponse[A](error: ApiClientError): FutureErrorOr[A] = {
    EitherT.left[A](Future(error))
  }

  def ebayItemSummaries(ids: String*): Seq[EbayItemSummary] = {
    ids.map(ebayItemSummary(_))
  }

  def ebayItemSummary(id: String, name: String = "ebay item", feedbackScore: Int = 150, feedbackPercentage: Int = 150) =
    EbayItemSummary(id, name, ItemPrice(BigDecimal.valueOf(30.00), "GBP"), ItemSeller("168.robinhood", Some(feedbackPercentage), Some(feedbackScore)))

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
      None,
      Seq(ItemShippingOption("Royal Mail 1st class", ShippingCost(BigDecimal.valueOf(4.99), "GBR")))
    )
}
