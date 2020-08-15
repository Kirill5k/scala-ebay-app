package clients.cex

import cats.effect.IO
import clients.SttpClientSpec
import common.errors.ApiClientError.{HttpError, JsonParsingError}
import domain._
import sttp.client
import sttp.client.Response
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client.testing.SttpBackendStub
import sttp.model.{Method, StatusCode}

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

      val result = cexClient.getCurrentStock[ItemDetails.Generic](query)

      result.unsafeToFuture().map(res => res must be (List(
        ResellableItem(
          ItemDetails.Generic("Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/A"),
          ListingDetails("https://uk.webuy.com/product-detail/?id=SLAPAPPMP16101SA", "Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/A", Some("Laptops - Apple Mac"), None, None, "USED / A", res(0).listingDetails.datePosted, "CEX", Map()),
          Price(2, 1950.0),
          Some(ResellPrice(BigDecimal(1131.0), BigDecimal(1365.0)))
        ),
        ResellableItem(
          ItemDetails.Generic("Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/B"),
          ListingDetails("https://uk.webuy.com/product-detail/?id=SLAPAPPMP16101SB", "Apple MacBook Pro 16,1/i7-9750H/16GB/512GB SSD/5300M 4GB/16\"/Silver/B", Some("Laptops - Apple Mac"), None, None, "USED / B", res(1).listingDetails.datePosted, "CEX", Map()),
          Price(1, 1800.0),
          Some(ResellPrice(BigDecimal(1044.0), BigDecimal(1260.0)))
        ),
        ResellableItem(
          ItemDetails.Generic("Apple MacBook Pro 16,1/i9-9880H/16GB/1TB SSD/5500M 4GB/16\"/Space Grey/A"),
          ListingDetails("https://uk.webuy.com/product-detail/?id=SLAPAPPMP16146SGA", "Apple MacBook Pro 16,1/i9-9880H/16GB/1TB SSD/5500M 4GB/16\"/Space Grey/A", Some("Laptops - Apple Mac"), None, None, "USED / A", res(2).listingDetails.datePosted, "CEX", Map()),
          Price(1, 2200.0),
          Some(ResellPrice(BigDecimal(1276.0), BigDecimal(1540.0)))
        )
      )))
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

      result.unsafeToFuture().map { price =>
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

      result.unsafeToFuture().map { price =>
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

      result.unsafeToFuture().map { price =>
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

      result.attempt.unsafeToFuture().map { price =>
        cexClient.searchResultsCache.isEmpty must be(true)
        price must be(Left(JsonParsingError("error parsing json: DecodingFailure(C[A], List(DownField(boxes), DownField(data), DownField(response)))")))
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

      result.attempt.unsafeToFuture().map { price =>
        cexClient.searchResultsCache.isEmpty must be(true)
        price must be(Left(HttpError(400, "error sending request to cex: 400")))
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

      result.unsafeToFuture().map { price =>
        price must be(None)
        cexClient.searchResultsCache.isEmpty must be(true)
      }
    }

    def isQueryRequest(req: client.Request[_, _], params: Map[String, String]): Boolean =
      isGoingTo(req, Method.GET, "cex.com", List("v3", "boxes"), params)
  }
}
