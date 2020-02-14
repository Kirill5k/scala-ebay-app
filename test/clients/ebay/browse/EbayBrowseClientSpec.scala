package clients.ebay.browse

import domain.ApiClientError._
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

class EbayBrowseClientSpec extends PlaySpec with ScalaFutures with MockitoSugar {

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
        val foundItems = ebaySearchClient.search(accessToken, searchQueryParams)

        whenReady(foundItems.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { items =>
          items.map(_.itemId) must be (Seq("item-1", "item-2", "item-3", "item-4", "item-5"))
        }
      }
    }

    "return empty seq when nothing found" in {
      withEbaySearchClient(200, "ebay/search-empty-response.json") { ebaySearchClient =>
        val foundItems = ebaySearchClient.search(accessToken, searchQueryParams)

        whenReady(foundItems.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { items =>
          items must be (Seq())
        }
      }
    }

    "return autherror when token expired during search" in {
      withEbaySearchClient(403, "ebay/get-item-unauthorized-error-response.json") { ebaySearchClient =>
        val result = ebaySearchClient.search(accessToken, searchQueryParams)

        whenReady(result.attempt.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { error =>
          error must be (Left(AuthError("ebay account has expired: 403")))
        }
      }
    }

    "make get request to obtain item details" in {
      withEbaySearchClient(200, "ebay/get-item-1-success-response.json") { ebaySearchClient =>
        val itemResult = ebaySearchClient.getItem(accessToken, itemId)


        whenReady(itemResult.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { item =>
          item.map(_.itemId) must be (Some("v1|114059888671|0"))
        }
      }
    }

    "return autherror when token expired" in {
      withEbaySearchClient(403, "ebay/get-item-unauthorized-error-response.json") { ebaySearchClient =>
        val result = ebaySearchClient.getItem(accessToken, itemId)

        whenReady(result.attempt.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { error =>
          error must be (Left(AuthError("ebay account has expired: 403")))
        }
      }
    }

    "return None when 404" in {
      withEbaySearchClient(404, "ebay/get-item-notfound-error-response.json") { ebaySearchClient =>
        val itemResult = ebaySearchClient.getItem(accessToken, itemId)

        whenReady(itemResult.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { items =>
          items must be (None)
        }
      }
    }
  }

  def withEbaySearchClient[T](status: Int, responseFile: String)(block: EbayBrowseClient => T): T = {
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
        block(new EbayBrowseClient(config, client))
      }
    }
  }
}
