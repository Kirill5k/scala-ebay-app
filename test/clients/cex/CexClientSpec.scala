package clients.cex

import cats.effect.IO
import clients.SttpClientSpec
import common.errors.ApiClientError.{HttpError, JsonParsingError}
import domain.ItemDetails.GenericItemDetails
import domain.PurchasableItem.GenericPurchasableItem
import domain.{PurchasableItemBuilder, PurchasePrice, ResellPrice, SearchQuery, VideoGameBuilder}
import sttp.client
import sttp.client.Response
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client.testing.SttpBackendStub
import sttp.model.{Method, StatusCode}

import scala.concurrent.duration._
import scala.language.postfixOps

class CexClientSpec extends SttpClientSpec {

  "CexClient" should {

    "get current stock" in {
      val query = SearchQuery("macbook pro 16,1")
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isQueryRequest(r, Map("q" -> query.value, "inStock" -> "1", "inStockOnline" -> "1")) =>
            Response.ok(json("cex/search-macbook-success-response.json"))
          case _ => throw new RuntimeException()
        }

      val cexClient = new CexClient(sttpCatsBackend(testingBackend))

      val result = cexClient.getCurrentStock(query)

      whenReady(result.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { items =>
        items must be (List(
          PurchasableItemBuilder.generic("Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/A", 2, 1950.0),
          PurchasableItemBuilder.generic("Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/B", 1, 1800.0),
          PurchasableItemBuilder.generic("Apple MacBook Pro 16,1/i9-9880H/16GB/1TB SSD/5500M 4GB/16\"/Space Grey/A", 1, 2200.0)
        ))
      }
    }

    "find minimal resell price and store it in cache" in {
      val query = SearchQuery("super mario 3 XBOX ONE")
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isQueryRequest(r, Map("q" -> query.value)) =>
            Response.ok(json("cex/search-iphone-success-response.json"))
          case _ => throw new RuntimeException()
        }

      val cexClient = new CexClient(sttpCatsBackend(testingBackend))

      val result = cexClient.findResellPrice(query)

      whenReady(result.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { price =>
        val expectedPrice = Some(ResellPrice(BigDecimal.valueOf(108), BigDecimal.valueOf(153)))
        price must be(expectedPrice)
        cexClient.searchResultsCache.get(query) must be(expectedPrice)
      }
    }

    "return resell price from cache" in {
      val query = SearchQuery("super mario 3 XBOX ONE")
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case _ => throw new RuntimeException()
        }

      val cexClient = new CexClient(sttpCatsBackend(testingBackend))
      cexClient.searchResultsCache.put(query, Some(ResellPrice(BigDecimal.valueOf(108), BigDecimal.valueOf(153))))

      val result = cexClient.findResellPrice(query)

      whenReady(result.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { price =>
        val expectedPrice = Some(ResellPrice(BigDecimal.valueOf(108), BigDecimal.valueOf(153)))
        price must be(expectedPrice)
        cexClient.searchResultsCache.get(query) must be(expectedPrice)
      }
    }

    "return none when no results" in {
      val query = SearchQuery("super mario 3 XBOX ONE")
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isQueryRequest(r, Map("q" -> query.value)) =>
            Response.ok(json("cex/search-noresults-response.json"))
          case _ => throw new RuntimeException()
        }

      val cexClient = new CexClient(sttpCatsBackend(testingBackend))

      val result = cexClient.findResellPrice(query)

      whenReady(result.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { price =>
        price must be(None)
        cexClient.searchResultsCache.containsKey(query) must be(false)
      }
    }

    "return internal error when failed to parse json" in {
      val query = SearchQuery("super mario 3 XBOX ONE")
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isQueryRequest(r, Map("q" -> query.value)) =>
            Response.ok(json("cex/search-unexpected-response.json"))
          case _ => throw new RuntimeException()
        }

      val cexClient = new CexClient(sttpCatsBackend(testingBackend))

      val result = cexClient.findResellPrice(query)

      whenReady(result.attempt.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { price =>
        price must be(Left(JsonParsingError("error parsing json: DecodingFailure(C[A], List(DownField(boxes), DownField(data), DownField(response)))")))
        cexClient.searchResultsCache.isEmpty must be(true)
      }
    }

    "return http error when not success" in {
      val query = SearchQuery("super mario 3 XBOX ONE")
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isQueryRequest(r, Map("q" -> query.value)) =>
            Response(json("cex/search-error-response.json"), StatusCode.BadRequest)
          case _ => throw new RuntimeException()
        }

      val cexClient = new CexClient(sttpCatsBackend(testingBackend))

      val result = cexClient.findResellPrice(query)

      whenReady(result.attempt.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { price =>
        price must be(Left(HttpError(400, "error sending request to cex: 400")))
        cexClient.searchResultsCache.isEmpty must be(true)
      }
    }

    "return none when 429 returned" in {
      val query = SearchQuery("super mario 3 XBOX ONE")
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isQueryRequest(r, Map("q" -> query.value)) =>
            Response(json("cex/search-error-response.json"), StatusCode.TooManyRequests)
          case _ => throw new RuntimeException()
        }

      val cexClient = new CexClient(sttpCatsBackend(testingBackend))

      val result = cexClient.findResellPrice(query)

      whenReady(result.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { price =>
        price must be(None)
        cexClient.searchResultsCache.isEmpty must be(true)
      }
    }

    def isQueryRequest(req: client.Request[_, _], params: Map[String, String]): Boolean =
      isGoingTo(req, Method.GET, "cex.com", List("v3", "boxes"), params)
  }
}
