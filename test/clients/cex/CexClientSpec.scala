package clients.cex

import cats.effect.IO
import clients.SttpClientSpec
import domain.ApiClientError._
import domain.{ResellPrice, VideoGameBuilder}
import play.api.Configuration
import sttp.client
import sttp.client.Response
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client.testing.SttpBackendStub
import sttp.model.{Method, StatusCode}

import scala.concurrent.duration._
import scala.language.postfixOps

class CexClientSpec extends SttpClientSpec {

  val cexConfig             = Map("baseUri"       -> "http://cex.com", "searchPath" -> "/search")
  val config: Configuration = Configuration("cex" -> cexConfig)

  val queryString = "super mario 3 XBOX ONE"

  "CexClient" should {
    val gameDetails = VideoGameBuilder.build("super mario 3", platform = "XBOX ONE").itemDetails

    "find minimal resell price and store it in cache" in {
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isPriceQueryRequest(r) =>
            Response.ok(json("cex/search-success-response.json"))
          case _ => throw new RuntimeException()
        }

      val cexClient = new CexClient(config, sttpCatsBackend(testingBackend))

      val result = cexClient.findResellPrice(gameDetails)

      whenReady(result.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { price =>
        val expectedPrice = Some(ResellPrice(BigDecimal.valueOf(108), BigDecimal.valueOf(153)))
        price must be(expectedPrice)
        cexClient.searchResultsCache.get(queryString) must be(expectedPrice)
      }
    }

    "return none when no results" in {
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isPriceQueryRequest(r) =>
            Response.ok(json("cex/search-noresults-response.json"))
          case _ => throw new RuntimeException()
        }

      val cexClient = new CexClient(config, sttpCatsBackend(testingBackend))

      val result = cexClient.findResellPrice(gameDetails)

      whenReady(result.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { price =>
        price must be(None)
        cexClient.searchResultsCache.containsKey(queryString) must be(false)
      }
    }

    "return none when not enough details" in {
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isPriceQueryRequest(r) =>
            Response.ok(json("cex/search-noresults-response.json"))
          case _ => throw new RuntimeException()
        }

      val cexClient = new CexClient(config, sttpCatsBackend(testingBackend))

      val result = cexClient.findResellPrice(gameDetails.copy(name = None))

      whenReady(result.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { price =>
        price must be(None)
        cexClient.searchResultsCache.isEmpty must be(true)
      }
    }

    "return internal error when failed to parse json" in {
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isPriceQueryRequest(r) =>
            Response.ok(json("cex/search-unexpected-response.json"))
          case _ => throw new RuntimeException()
        }

      val cexClient = new CexClient(config, sttpCatsBackend(testingBackend))

      val result = cexClient.findResellPrice(gameDetails)

      whenReady(result.attempt.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { price =>
        price must be(Left(JsonParsingError("error parsing json: DecodingFailure(C[A], List(DownField(boxes), DownField(data), DownField(response)))")))
        cexClient.searchResultsCache.isEmpty must be(true)
      }
    }

    "return http error when not success" in {
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isPriceQueryRequest(r) =>
            Response(json("cex/search-error-response.json"), StatusCode.BadRequest)
          case _ => throw new RuntimeException()
        }

      val cexClient = new CexClient(config, sttpCatsBackend(testingBackend))

      val result = cexClient.findResellPrice(gameDetails)

      whenReady(result.attempt.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { price =>
        price must be(Left(HttpError(400, "error sending request to cex: 400")))
        cexClient.searchResultsCache.isEmpty must be(true)
      }
    }

    "return none when 429 returned" in {
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isPriceQueryRequest(r) =>
            Response(json("cex/search-error-response.json"), StatusCode.TooManyRequests)
          case _ => throw new RuntimeException()
        }

      val cexClient = new CexClient(config, sttpCatsBackend(testingBackend))

      val result = cexClient.findResellPrice(gameDetails)

      whenReady(result.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { price =>
        price must be(None)
        cexClient.searchResultsCache.isEmpty must be(true)
      }
    }

    def isPriceQueryRequest(req: client.Request[_, _]): Boolean =
      isGoingTo(req, Method.GET, "cex.com", List("v3", "boxes"), Map("q" -> queryString))
  }
}
