package clients.ebay.search

import domain.ApiClientError._
import domain.ListingDetails
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.http.MediaRange
import play.api.mvc.{AnyContent, Request, Results}
import play.api.routing.Router
import play.api.routing.sird._
import play.api.test.WsTestClient
import play.api.{BuiltInComponentsFromContext, Configuration}
import play.core.server.Server
import play.filters.HttpFiltersComponents

import scala.concurrent.duration._
import scala.language.postfixOps

class EbaySearchClientSpec extends PlaySpec with ScalaFutures with MockitoSugar {

  import scala.concurrent.ExecutionContext.Implicits.global

  val ebayCredentials = Seq(Map("clientId" -> "id-1", "clientSecret" -> "secret-1"), Map("clientId" -> "id-2", "clientSecret" -> "secret-2"))
  val ebayConfig = Map("baseUri" -> "/ebay", "authPath" -> "/auth", "searchPath" -> "/search", "itemPath" -> "/item", "credentials" -> ebayCredentials)
  val config: Configuration = Configuration("ebay" -> ebayConfig)

  val accessToken = "access-token"
  val itemId = "item-id-1"
  val searchQueryParams = Map("q" -> "iphone")

  "EbaySearchClient" should {

    "make get request to search api" in {
      withEbaySearchClient(200, "ebay/search-success-response.json") { ebaySearchClient =>
        val item = ebaySearchClient.search(accessToken, searchQueryParams)

        whenReady(item.value, timeout(10 seconds), interval(500 millis)) { foundItems =>
          foundItems.map(_.map(_.itemId)) must be(Right(Seq("item-1", "item-2", "item-3", "item-4", "item-5")))
        }
      }
    }

    "return empty seq when nothing found" in {
      withEbaySearchClient(200, "ebay/search-empty-response.json") { ebaySearchClient =>
        val item = ebaySearchClient.search(accessToken, searchQueryParams)

        whenReady(item.value, timeout(10 seconds), interval(500 millis)) { foundItems =>
          foundItems.map(_.map(_.itemId)) must be(Right(Seq()))
        }
      }
    }

    "make get request to obtain item details" in {
      withEbaySearchClient(200, "ebay/get-item-1-success-response.json") { ebaySearchClient =>
        val item = ebaySearchClient.getItem(accessToken, itemId)

        whenReady(item.value, timeout(10 seconds), interval(500 millis)) { foundItem =>
          val item: ListingDetails = foundItem.getOrElse(throw new RuntimeException())
          item must have (
            Symbol("url") ("https://www.ebay.co.uk/itm/Samsung-Galaxy-S10-128gb-UNLOCKED-Prism-Blue-/114059888671"),
            Symbol("title") ("Samsung Galaxy S10 128gb UNLOCKED Prism Blue"),
            Symbol("description") (Some("Pictures to follow.")),
            Symbol("image") ("https://i.ebayimg.com/images/g/yOMAAOSw~5ReGEH2/s-l1600.jpg"),
            Symbol("buyingOptions") (Seq("FIXED_PRICE", "BEST_OFFER")),
            Symbol("sellerName") ("jb-liquidation3"),
            Symbol("price") (BigDecimal.valueOf(425.00)),
            Symbol("condition") ("Used"),
            Symbol("dateEnded") (None),
            Symbol("properties") (Map("Colour" -> "Blue", "RAM" -> "8 GB", "Brand" -> "Samsung", "Network" -> "Unlocked", "Model" -> "Samsung Galaxy S10", "Storage Capacity" -> "128 GB"))
          )
        }
      }
    }

    "return autherror when token expired" in {
      withEbaySearchClient(403, "ebay/get-item-unauthorized-error-response.json") { ebaySearchClient =>
        val item = ebaySearchClient.getItem(accessToken, itemId)

        whenReady(item.value, timeout(10 seconds), interval(500 millis)) { foundItem =>
          foundItem must be(Left(AuthError("ebay account has expired: 403")))
        }
      }
    }

    "return httperror when error" in {
      withEbaySearchClient(404, "ebay/get-item-notfound-error-response.json") { ebaySearchClient =>
        val item = ebaySearchClient.getItem(accessToken, itemId)

        whenReady(item.value, timeout(10 seconds), interval(500 millis)) { foundItem =>
          foundItem must be(Left(HttpError(404, "error sending request to ebay search api: The specified item Id was not found.")))
        }
      }
    }
  }

  def withEbaySearchClient[T](status: Int, responseFile: String)(block: EbaySearchClient => T): T = {
    def assertHeaders(request: Request[AnyContent]): Unit = {
      request.contentType must be(Some("application/json"))
      request.acceptedTypes must be(MediaRange.parse("application/json"))
      request.headers.get("Authorization") must be(Some("Bearer access-token"))
      request.headers.get("X-EBAY-C-MARKETPLACE-ID") must be(Some("EBAY_GB"))
    }

    Server.withApplicationFromContext() { context =>
      new BuiltInComponentsFromContext(context) with HttpFiltersComponents {
        override def router: Router = Router.from {
          case GET(p"/ebay/item/$id") =>
            Action { req =>
              id must be("item-id-1")
              assertHeaders(req)
              Results.Status(status).sendResource(responseFile)(executionContext, fileMimeTypes)
            }
          case GET(p"/ebay/search" ? q"q=$query") =>
            Action { req =>
              query must be("iphone")
              assertHeaders(req)
              Results.Status(status).sendResource(responseFile)(executionContext, fileMimeTypes)
            }
        }
      }.application
    } { implicit port =>
      WsTestClient.withClient { client =>
        block(new EbaySearchClient(config, client))
      }
    }
  }
}
