package clients.ebay.browse

import cats.effect.IO
import clients.SttpClientSpec
import domain.ApiClientError._
import play.api.Configuration
import sttp.client.Response
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client.testing.SttpBackendStub
import sttp.model.{Method, StatusCode}

import scala.concurrent.duration._
import scala.language.postfixOps

class EbayBrowseClientSpec extends SttpClientSpec {

  val ebayCredentials = List(Map("clientId" -> "id-1", "clientSecret" -> "secret-1"), Map("clientId" -> "id-2", "clientSecret" -> "secret-2"))
  val ebayConfig = Map("baseUri" -> "http://ebay.com", "authPath" -> "/auth", "searchPath" -> "/search", "itemPath" -> "/item", "credentials" -> ebayCredentials)
  val config: Configuration = Configuration("ebay" -> ebayConfig)

  val accessToken = "access-token"
  val itemId = "item-id-1"
  val searchQueryParams = Map("q" -> "iphone")

  "EbaySearchClient" should {

    "make get request to search api" in {
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isGoingToWithSpecificContent(r, Method.GET, "ebay.com", List("buy", "browse", "v1", "item_summary", "search"), searchQueryParams) =>
            Response.ok(json("ebay/search-success-response.json"))
          case _ => throw new RuntimeException()
        }

      val ebaySearchClient = new EbayBrowseClient(config, sttpCatsBackend(testingBackend))
      val foundItems = ebaySearchClient.search(accessToken, searchQueryParams)

      whenReady(foundItems.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { items =>
        items.map(_.itemId) must be (List("item-1", "item-2", "item-3", "item-4", "item-5"))
      }
    }

    "return empty seq when nothing found" in {
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isGoingToWithSpecificContent(r, Method.GET, "ebay.com", List("buy", "browse", "v1", "item_summary", "search"), searchQueryParams) =>
            Response.ok(json("ebay/search-empty-response.json"))
          case _ => throw new RuntimeException()
        }

      val ebaySearchClient = new EbayBrowseClient(config, sttpCatsBackend(testingBackend))
      val foundItems = ebaySearchClient.search(accessToken, searchQueryParams)

      whenReady(foundItems.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { items =>
        items must be (Nil)
      }
    }

    "return autherror when token expired during search" in {
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isGoingToWithSpecificContent(r, Method.GET, "ebay.com", List("buy", "browse", "v1", "item_summary", "search"), searchQueryParams) =>
            Response(json("ebay/get-item-unauthorized-error-response.json"), StatusCode.Forbidden)
          case _ => throw new RuntimeException()
        }

      val ebaySearchClient = new EbayBrowseClient(config, sttpCatsBackend(testingBackend))
      val result = ebaySearchClient.search(accessToken, searchQueryParams)

      whenReady(result.attempt.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { error =>
        error must be (Left(AuthError("ebay account has expired: 403")))
      }
    }

    "make get request to obtain item details" in {
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isGoingToWithSpecificContent(r, Method.GET, "ebay.com", List("buy", "browse", "v1", "item", itemId)) =>
            Response.ok(json("ebay/get-item-1-success-response.json"))
          case _ => throw new RuntimeException()
        }

      val ebaySearchClient = new EbayBrowseClient(config, sttpCatsBackend(testingBackend))
      val itemResult = ebaySearchClient.getItem(accessToken, itemId)

      whenReady(itemResult.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { item =>
        item.map(_.itemId) must be (Some("v1|114059888671|0"))
      }
    }

    "make get request to obtain item details without aspects" in {
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isGoingToWithSpecificContent(r, Method.GET, "ebay.com", List("buy", "browse", "v1", "item", itemId)) =>
            Response.ok(json("ebay/get-item-3-no-aspects-success-response.json"))
          case _ => throw new RuntimeException()
        }

      val ebaySearchClient = new EbayBrowseClient(config, sttpCatsBackend(testingBackend))
      val itemResult = ebaySearchClient.getItem(accessToken, itemId)

      whenReady(itemResult.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { item =>
        item.map(_.localizedAspects) must be (Some(None))
      }
    }

    "return autherror when token expired" in {
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isGoingToWithSpecificContent(r, Method.GET, "ebay.com", List("buy", "browse", "v1", "item", itemId)) =>
            Response(json("ebay/get-item-unauthorized-error-response.json"), StatusCode.Forbidden)
          case _ => throw new RuntimeException()
        }

      val ebaySearchClient = new EbayBrowseClient(config, sttpCatsBackend(testingBackend))
      val result = ebaySearchClient.getItem(accessToken, itemId)

      whenReady(result.attempt.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { error =>
        error must be (Left(AuthError("ebay account has expired: 403")))
      }
    }

    "return None when 404" in {
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isGoingToWithSpecificContent(r, Method.GET, "ebay.com", List("buy", "browse", "v1", "item", itemId)) =>
            Response(json("ebay/get-item-notfound-error-response.json"), StatusCode.NotFound)
          case _ => throw new RuntimeException()
        }

      val ebaySearchClient = new EbayBrowseClient(config, sttpCatsBackend(testingBackend))
      val itemResult = ebaySearchClient.getItem(accessToken, itemId)

      whenReady(itemResult.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { items =>
        items must be (None)
      }
    }
  }
}
