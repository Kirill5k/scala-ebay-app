package clients.ebay

import cats.effect.IO
import clients.ebay.auth.EbayAuthClient
import clients.ebay.browse.EbayBrowseClient
import clients.ebay.browse.EbayBrowseResponse._
import common.errors.ApiClientError.{AuthError, HttpError}
import domain.ItemDetails.Game
import domain.{ItemDetails, SearchQuery}
import org.mockito.ArgumentMatchersSugar
import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.AsyncMockitoSugar
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec

class EbaySearchClientSpec extends AsyncWordSpec with Matchers with AsyncMockitoSugar with ArgumentMatchersSugar {
  val accessToken = "access-token"
  val searchQuery = SearchQuery("xbox")

  "An EbaySearchClient" should {

    "search for video games" in {
      val searchParamsCaptor = ArgCaptor[Map[String, String]]
      val (authClient, browseClient) = mockEbayClients
      val videoGameSearchClient = new EbaySearchClient(authClient, browseClient)

      when(browseClient.search(any, any)).thenReturn(IO.pure(List()))

      val itemsResponse = videoGameSearchClient.findItemsListedInLastMinutes[ItemDetails.Game](searchQuery, 15)

      itemsResponse.compile.toList.unsafeToFuture().map { items =>
        verify(authClient, times(1)).accessToken
        verify(browseClient, times(1)).search(eqTo(accessToken), searchParamsCaptor)
        searchParamsCaptor.values.map(_("q")) must be (List("xbox"))
        searchParamsCaptor.value("limit") must be ("200")
        searchParamsCaptor.value("category_ids") must be ("139973")
        searchParamsCaptor.value("filter") must startWith ("conditionIds:%7B1000|1500|2000|2500|3000|4000|5000%7D,itemLocationCountry:GB,deliveryCountry:GB,price:[0..90],priceCurrency:GBP,itemLocationCountry:GB,buyingOptions:%7BFIXED_PRICE%7D,itemStartDate:[")
        items must be (List())
      }
    }

    "switch ebay account on autherror" in {
      val (authClient, browseClient) = mockEbayClients
      val videoGameSearchClient = new EbaySearchClient(authClient, browseClient)

      when(authClient.accessToken).thenReturn(IO.pure(accessToken))
      when(browseClient.search(any, any)).thenReturn(IO.raiseError(AuthError("Too many requests")))

      val itemsResponse = videoGameSearchClient.findItemsListedInLastMinutes[ItemDetails.Game](searchQuery, 15)

      itemsResponse.compile.toList.unsafeToFuture().map { error =>
        videoGameSearchClient.itemsIds.isEmpty must be (true)
        verify(authClient).accessToken
        verify(authClient).switchAccount()
        verify(browseClient).search(eqTo(accessToken), anyMap[String, String])
        verify(browseClient, never).getItem(any[String], any[String])
        error must be (List())
      }
    }

    "return api client error on failure" in {
      val (authClient, browseClient) = mockEbayClients
      val videoGameSearchClient = new EbaySearchClient(authClient, browseClient)

      doReturn(IO.raiseError(HttpError(400, "Bad request")))
        .when(browseClient).search(any, any)

      val itemsResponse = videoGameSearchClient.findItemsListedInLastMinutes[ItemDetails.Game](searchQuery, 15)

      itemsResponse.compile.toList.unsafeToFuture().map { error =>
        videoGameSearchClient.itemsIds.isEmpty must be (true)
        verify(authClient, times(1)).accessToken
        verify(authClient, never).switchAccount()
        verify(browseClient, times(1)).search(eqTo(accessToken), anyMap[String, String])
        verify(browseClient, never).getItem(any, any)
        error must be (List())
      }
    }

    "filter out items with bad feedback" in {
      val (authClient, browseClient) = mockEbayClients
      val videoGameSearchClient = new EbaySearchClient(authClient, browseClient)

      doReturn(IO.pure(List(ebayItemSummary("1", feedbackPercentage = 90), ebayItemSummary("1", feedbackScore = 4))))
        .when(browseClient).search(any, any)

      val itemsResponse = videoGameSearchClient.findItemsListedInLastMinutes[ItemDetails.Game](searchQuery, 15)

      itemsResponse.compile.toList.unsafeToFuture().map { items =>
        videoGameSearchClient.itemsIds.isEmpty must be (true)
        verify(authClient, times(1)).accessToken
        verify(browseClient, times(1)).search(eqTo(accessToken), anyMap[String, String])
        verify(browseClient, never).getItem(any, any)
        items must be (List())
      }
    }

    "filter out items with bad names" in {
      val (authClient, browseClient) = mockEbayClients
      val videoGameSearchClient = new EbaySearchClient(authClient, browseClient)

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
        ebayItemSummary("18", name = """fifa 2020 million 100 point XBOX"""),
        ebayItemSummary("19", name = """animal crossing dinosaur recipe card"""),
        ebayItemSummary("20", name = """fallout 76 5000 caps"""),
        ebayItemSummary("21", name = """borderlands 4 promotional copy"""),
        ebayItemSummary("22", name = """Shiny 6IV Go Park Level 1 Timid Trace Gardevoir Sword/Shield Switch Master Ball""")
      )))
        .when(browseClient).search(any, any)

      val itemsResponse = videoGameSearchClient.findItemsListedInLastMinutes[ItemDetails.Game](searchQuery, 15)

      itemsResponse.compile.toList.unsafeToFuture().map { items =>
        videoGameSearchClient.itemsIds.isEmpty must be (true)
        items must be (List())
      }
    }

    "get item details for each item id" in {
      val (authClient, browseClient) = mockEbayClients
      val videoGameSearchClient = new EbaySearchClient(authClient, browseClient)

      when(browseClient.search(any, any)).thenReturn(IO.pure(ebayItemSummaries("item-1")))
      when(browseClient.getItem(accessToken, "item-1")).thenReturn(IO.pure(Some(ebayItem.copy(itemId = "item-1"))))

      val itemsResponse = videoGameSearchClient.findItemsListedInLastMinutes[ItemDetails.Game](searchQuery, 15)

      itemsResponse.compile.toList.unsafeToFuture().map { items =>
        videoGameSearchClient.itemsIds.containsKey("item-1") must be (true)
        verify(authClient, times(2)).accessToken
        verify(browseClient, times(1)).search(eqTo(accessToken), anyMap[String, String])
        verify(browseClient, times(1)).getItem(eqTo(accessToken), any)
        items.map(_.itemDetails) must be (List(Game(Some("call of duty modern warfare"), Some("XBOX ONE"), Some("2019"), Some("Action"))))
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
