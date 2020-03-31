package clients.ebay

import cats.effect.IO
import clients.ebay.auth.EbayAuthClient
import clients.ebay.browse.EbayBrowseClient
import clients.ebay.browse.EbayBrowseResponse.{EbayItem, EbayItemSummary, ItemImage, ItemPrice, ItemProperty, ItemSeller, ItemShippingOption, ShippingCost}
import domain.ApiClientError.{AuthError, HttpError}
import domain.ItemDetails.GameDetails
import org.mockito.captor.ArgCaptor
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec

import scala.concurrent.duration._
import scala.language.postfixOps

class VideoGameEbayClientSpec extends PlaySpec with ScalaFutures with MockitoSugar with ArgumentMatchersSugar {
  val accessToken = "access-token"

  "VideoGameSearchClient" should {

    "search for ps4, xbox one and switch games" in {
      val searchParamsCaptor = ArgCaptor[Map[String, String]]
      val (authClient, browseClient) = mockEbayClients
      val videoGameSearchClient = new VideoGameEbayClient(authClient, browseClient)

      when(browseClient.search(any, any)).thenReturn(IO.pure(List()))

      val itemsResponse = videoGameSearchClient.getItemsListedInLastMinutes(15)

      whenReady(itemsResponse.compile.toList.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { items =>
        items must be (List())
        verify(authClient, times(3)).accessToken
        verify(browseClient, times(3)).search(eqTo(accessToken), searchParamsCaptor)
        searchParamsCaptor.values.map(_("q")) must contain allOf ("PS4", "XBOX ONE", "SWITCH")
        searchParamsCaptor.value("limit") must be ("200")
        searchParamsCaptor.value("category_ids") must be ("139973")
        searchParamsCaptor.value("filter") must startWith ("conditionIds:%7B1000|1500|2000|2500|3000|4000|5000%7D,itemLocationCountry:GB,deliveryCountry:GB,price:[0..100],priceCurrency:GBP,itemLocationCountry:GB,buyingOptions:%7BFIXED_PRICE%7D,itemStartDate:[")
      }
    }

    "switch ebay account on autherror" in {
      val (authClient, browseClient) = mockEbayClients
      val videoGameSearchClient = new VideoGameEbayClient(authClient, browseClient)

      when(authClient.accessToken).thenReturn(IO.pure(accessToken))
      when(browseClient.getItem(any, any)).thenReturn(IO.pure(None))

      doReturn(IO.pure(List(ebayItemSummary("1"))))
        .doReturn(IO.pure(List()))
        .doReturn(IO.raiseError(AuthError("Too many requests")))
        .when(browseClient).search(any, any)

      val itemsResponse = videoGameSearchClient.getItemsListedInLastMinutes(15)

      whenReady(itemsResponse.compile.toList.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { error =>
        error must be (List())
        videoGameSearchClient.itemsIds.isEmpty must be (true)
        verify(authClient, times(4)).accessToken
        verify(authClient, times(1)).switchAccount
        verify(browseClient, times(3)).search(eqTo(accessToken), anyMap[String, String])
        verify(browseClient).getItem(accessToken, "1")
      }
    }

    "return api client error on failure" in {
      val (authClient, browseClient) = mockEbayClients
      val videoGameSearchClient = new VideoGameEbayClient(authClient, browseClient)

      doReturn(IO.raiseError(HttpError(400, "Bad request")))
        .doReturn(IO.pure(ebayItemSummaries("item-1", "item-2")))
        .doReturn(IO.pure(ebayItemSummaries("item-3", "item-4")))
        .when(browseClient).search(any, any)

      val itemsResponse = videoGameSearchClient.getItemsListedInLastMinutes(15)

      whenReady(itemsResponse.compile.toList.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { error =>
        error must be (List())
        videoGameSearchClient.itemsIds.isEmpty must be (true)
        verify(authClient, times(1)).accessToken
        verify(authClient, never).switchAccount
        verify(browseClient, times(1)).search(eqTo(accessToken), anyMap[String, String])
        verify(browseClient, never).getItem(any, any)
      }
    }

    "filter out items with bad feedback" in {
      val (authClient, browseClient) = mockEbayClients
      val videoGameSearchClient = new VideoGameEbayClient(authClient, browseClient)

      doReturn(IO.pure(List(ebayItemSummary("1", feedbackPercentage = 90), ebayItemSummary("1", feedbackScore = 4))))
        .doReturn(IO.pure(List()))
        .doReturn(IO.pure(List()))
        .when(browseClient).search(any, any)

      val itemsResponse = videoGameSearchClient.getItemsListedInLastMinutes(15)

      whenReady(itemsResponse.compile.toList.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { items =>
        items must be (List())
        videoGameSearchClient.itemsIds.isEmpty must be (true)
        verify(authClient, times(3)).accessToken
        verify(browseClient, times(3)).search(eqTo(accessToken), anyMap[String, String])
        verify(browseClient, never).getItem(any, any)
      }
    }

    "filter out items with bad names" in {
      val (authClient, browseClient) = mockEbayClients
      val videoGameSearchClient = new VideoGameEbayClient(authClient, browseClient)

      doReturn(IO.pure(List(
        ebayItemSummary("1", name = "fallout 4 disc only"),
        ebayItemSummary("2", name = "fallout 76 blah blah blah blah blah"),
        ebayItemSummary("3", name = "call of duty digital code"),
        ebayItemSummary("4", name = "lego worlds read description"),
        ebayItemSummary("5", name = """Borderlands 3 "Spooling Recursion" X2 Godroll Moze Splash Damage (Xbox One)"""),
        ebayItemSummary("6", name = """Borderlands 3 - (ps4) (new maliwan takedown pistol) Moonfire (Anointed) x3 (op)"""),
        ebayItemSummary("7", name = """Borderlands 3 - (ps4) S3RV 80's Execute (anointed 50% cyro ase )(new takedown)"""),
        ebayItemSummary("8", name = """Borderlands 3 -(ps4) Redistributor(anointed 100% dam ase x 6 pack)(new takedown)"""),
        ebayItemSummary("9", name = """Borderlands 3 “Teething St4kbot” SMGdmg/+5GRENADE/JWD (Xbox One)"""),
        ebayItemSummary("10", name = """Borderlands 3 “Teething St4kbot” SMGdmg/+5GRENADE/JWD (Xbox One)"""),
        ebayItemSummary("11", name = """call of duty pre-order bonus"""),
        ebayItemSummary("12", name = """All Shiny Max IV Battle Ready Eeveelutions Pokemon Sword Shield Nintendo Switch"""),
        ebayItemSummary("13", name = """Call of Duty WW2 no case XBOX 360"""),
        ebayItemSummary("14", name = """Call of Duty WW2 digital code XBOX"""),
        ebayItemSummary("15", name = """Call of Duty WW2 with carry bag XBOX"""),
        ebayItemSummary("16", name = """xbox game pass XBOX"""),
        ebayItemSummary("17", name = """xbox gamepass XBOX"""),
        ebayItemSummary("18", name = """Shiny 6IV Go Park Level 1 Timid Trace Gardevoir Sword/Shield Switch Master Ball""")
      )))
        .doReturn(IO.pure(List()))
        .doReturn(IO.pure(List()))
        .when(browseClient).search(any, any)

      val itemsResponse = videoGameSearchClient.getItemsListedInLastMinutes(15)

      whenReady(itemsResponse.compile.toList.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { items =>
        items must be (List())
        videoGameSearchClient.itemsIds.isEmpty must be (true)
      }
    }

    "get item details for each item id" in {
      val (authClient, browseClient) = mockEbayClients
      val videoGameSearchClient = new VideoGameEbayClient(authClient, browseClient)

      doReturn(IO.pure(ebayItemSummaries("item-1")))
        .doReturn(IO.pure(ebayItemSummaries("item-2")))
        .doReturn(IO.pure(ebayItemSummaries("item-3")))
        .when(browseClient).search(any, any)

      doReturn(IO.pure(None)).when(browseClient).getItem(accessToken, "item-1")
      doReturn(IO.pure(None)).when(browseClient).getItem(accessToken, "item-2")
      doReturn(IO.pure(Some(ebayItem.copy(itemId = "item-3")))).when(browseClient).getItem(accessToken, "item-3")

      val itemsResponse = videoGameSearchClient.getItemsListedInLastMinutes(15)

      whenReady(itemsResponse.compile.toList.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { items =>
        items.map(_._1) must be (List(GameDetails(Some("Call of Duty Modern Warfare"), Some("XBOX ONE"), Some("2019"), Some("Action"))))
        videoGameSearchClient.itemsIds.containsKey("item-3") must be (true)
        verify(authClient, times(6)).accessToken
        verify(browseClient, times(3)).search(eqTo(accessToken), anyMap[String, String])
        verify(browseClient, times(3)).getItem(eqTo(accessToken), any)
      }
    }
  }

  def mockEbayClients: (EbayAuthClient, EbayBrowseClient) = {
    val authClient = mock[EbayAuthClient]
    val browseClient = mock[EbayBrowseClient]
    when(authClient.accessToken).thenReturn(IO.pure(accessToken))
    (authClient, browseClient)
  }

  def ebayItemSummaries(ids: String*): Seq[EbayItemSummary] = {
    ids.map(ebayItemSummary(_))
  }

  def ebayItemSummary(id: String, name: String = "ebay item", feedbackScore: Int = 150, feedbackPercentage: Int = 150) =
    EbayItemSummary(id, name, Some(ItemPrice(BigDecimal.valueOf(30.00), "GBP")), ItemSeller(Some("168.robinhood"), Some(feedbackPercentage), Some(feedbackScore)))

  def ebayItem: EbayItem =
    EbayItem(
      "item-1",
      "call of duty modern warfare xbox one 2019",
      Some("call of duty modern warfare xbox one 2019. Condition is New. Game came as part of bundle and not wanted. Never playes. Dispatched with Royal Mail 1st Class Large Letter."),
      None,
      "Video Games & Consoles|Video Games",
      ItemPrice(BigDecimal.valueOf(30.00), "GBP"),
      "New",
      Some(ItemImage("https://i.ebayimg.com/images/g/0kcAAOSw~5ReGFCQ/s-l1600.jpg")),
      ItemSeller(Some("168.robinhood"), Some(100), Some(150)),
      Some(List(
        ItemProperty("Game Name", "Call of Duty: Modern Warfare"),
        ItemProperty("Release Year", "2019"),
        ItemProperty("Platform", "Microsoft Xbox One"),
        ItemProperty("Genre", "Action"),
      )),
      List("FIXED_PRICE"),
      "https://www.ebay.co.uk/itm/call-of-duty-modern-warfare-xbox-one-2019-/333474293066",
      None,
      None,
      None,
      None,
      Some(List(ItemShippingOption("Royal Mail 1st class", ShippingCost(BigDecimal.valueOf(4.99), "GBR"))))
    )
}
