package clients.ebay

import cats.effect.IO
import clients.ebay.auth.EbayAuthClient
import clients.ebay.browse.EbayBrowseClient
import clients.ebay.browse.EbayBrowseResponse.{EbayItem, EbayItemSummary, ItemImage, ItemPrice, ItemProperty, ItemSeller, ItemShippingOption, ShippingCost}
import domain.ApiClientError.{AuthError, HttpError}
import domain.ItemDetails.GameDetails
import org.mockito.captor.ArgCaptor
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatestplus.play.PlaySpec


class VideoGameEbayClientSpec extends PlaySpec with AsyncIOSpec with MockitoSugar with ArgumentMatchersSugar {
  val accessToken = "access-token"

  "VideoGameSearchClient" should {

    "search for ps4, xbox one and switch games" in {
      val searchParamsCaptor = ArgCaptor[Map[String, String]]
      val (authClient, browseClient) = mockEbayClients
      val videoGameSearchClient = new VideoGameEbayClient(authClient, browseClient)

      when(browseClient.search(any, any)).thenReturn(IO.pure(Seq()))

      val itemsResponse = videoGameSearchClient.getItemsListedInLastMinutes(15)

      itemsResponse.compile.toList.asserting(_ must be (List()))

      verify(authClient, times(3)).accessToken
      verify(browseClient, times(3)).search(eqTo(accessToken), searchParamsCaptor)
      searchParamsCaptor.values.map(_("q")) must contain allOf ("PS4", "XBOX ONE", "SWITCH")
      searchParamsCaptor.value("limit") must be ("200")
      searchParamsCaptor.value("category_ids") must be ("139973")
      searchParamsCaptor.value("filter") must startWith ("conditionIds:%7B1000|1500|2000|2500|3000|4000|5000%7D,deliveryCountry:GB,price:[0..100],priceCurrency:GBP,itemLocationCountry:GB,buyingOptions:%7BFIXED_PRICE%7D,itemStartDate:[")
    }

    "switch ebay account on autherror" in {
      val (authClient, browseClient) = mockEbayClients
      val videoGameSearchClient = new VideoGameEbayClient(authClient, browseClient)

      when(authClient.accessToken).thenReturn(IO.pure(accessToken))
      when(browseClient.search(any, any)).thenReturn(IO.raiseError(AuthError("Too many requests")))

      val itemsResponse = videoGameSearchClient.getItemsListedInLastMinutes(15)

      itemsResponse.compile.toList.assertThrows[AuthError]

      verify(authClient, times(3)).accessToken
      verify(authClient, times(1)).switchAccount
      verify(browseClient, times(3)).search(eqTo(accessToken), anyMap[String, String])
    }

    "return api client error on failure" in {
      val (authClient, browseClient) = mockEbayClients
      val videoGameSearchClient = new VideoGameEbayClient(authClient, browseClient)

      doReturn(IO.raiseError(HttpError(400, "Bad request")))
        .doReturn(IO.pure(ebayItemSummaries("item-1", "item-2")))
        .doReturn(IO.pure(ebayItemSummaries("item-3", "item-4")))
        .when(browseClient).search(any, any)

      val itemsResponse = videoGameSearchClient.getItemsListedInLastMinutes(15)

      itemsResponse.compile.toList.assertThrows[HttpError]

      verify(authClient, times(3)).accessToken
      verify(browseClient, times(3)).search(eqTo(accessToken), anyMap[String, String])
      verify(browseClient, never).getItem(any, any)
    }

    "filter out items with bad feedback" in {
      val (authClient, browseClient) = mockEbayClients
      val videoGameSearchClient = new VideoGameEbayClient(authClient, browseClient)

      doReturn(IO.pure(Seq(ebayItemSummary("1", feedbackPercentage = 90), ebayItemSummary("1", feedbackScore = 4))))
        .doReturn(IO.pure(Seq()))
        .doReturn(IO.pure(Seq()))
        .when(browseClient).search(any, any)

      val itemsResponse = videoGameSearchClient.getItemsListedInLastMinutes(15)

      itemsResponse.compile.toList.asserting(_ must be (List()))

      verify(authClient, times(3)).accessToken
      verify(browseClient, times(3)).search(eqTo(accessToken), anyMap[String, String])
      verify(browseClient, never).getItem(any, any)
    }

    "filter out items with bad names" in {
      val (authClient, browseClient) = mockEbayClients
      val videoGameSearchClient = new VideoGameEbayClient(authClient, browseClient)

      doReturn(IO.pure(Seq(
        ebayItemSummary("1", name = "fallout 4 disc only"),
        ebayItemSummary("2", name = "fallout 76 blah blah blah blah blah"),
        ebayItemSummary("3", name = "call of duty digital code"),
        ebayItemSummary("4", name = "lego worlds read description"),
        ebayItemSummary("5", name = """Borderlands 3 "Spooling Recursion" X2 Godroll Moze Splash Damage (Xbox One)"""),
        ebayItemSummary("6", name = """Borderlands 3 - (ps4) (new maliwan takedown pistol) Moonfire (Anointed) x3 (op)"""),
        ebayItemSummary("7", name = """Borderlands 3 - (ps4) S3RV 80's Execute (anointed 50% cyro ase )(new takedown)"""),
        ebayItemSummary("8", name = """Borderlands 3 -(ps4) Redistributor(anointed 100% dam ase x 6 pack)(new takedown)"""),
        ebayItemSummary("9", name = """Borderlands 3 “Teething St4kbot” SMGdmg/+5GRENADE/JWD (Xbox One)"""),
        ebayItemSummary("10", name = """Borderlands 3 Rain Firestorm Grenade Anointed. 25% Damage 6 Seconds (Xbox1)"""),
      )))
        .doReturn(IO.pure(Seq()))
        .doReturn(IO.pure(Seq()))
        .when(browseClient).search(any, any)

      val itemsResponse = videoGameSearchClient.getItemsListedInLastMinutes(15)

      itemsResponse.compile.toList.asserting(_ must be (List()))
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
      doReturn(IO.pure(Some(ebayItem))).when(browseClient).getItem(accessToken, "item-3")

      val itemsResponse = videoGameSearchClient.getItemsListedInLastMinutes(15)

      itemsResponse.compile.toList.asserting(_.map(_._1) must be (List(GameDetails(Some("Call of Duty Modern Warfare"), Some("XBOX ONE"), Some("2019"), Some("Action")))))

      verify(authClient, times(6)).accessToken
      verify(browseClient, times(3)).search(eqTo(accessToken), anyMap[String, String])
      verify(browseClient, times(3)).getItem(eqTo(accessToken), any)
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
