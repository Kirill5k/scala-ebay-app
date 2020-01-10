package ebay.search

import cats.data.EitherT
import cats.implicits._
import exceptions.{ApiClientError, HttpError}
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.http.MediaRange
import play.api.mvc.Results
import play.api.routing.Router
import play.api.routing.sird._
import play.api.test.WsTestClient
import play.api.{BuiltInComponentsFromContext, Configuration}
import play.core.server.Server
import play.filters.HttpFiltersComponents

import scala.collection.immutable.ListMap
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class EbaySearchClientSpec extends PlaySpec with ScalaFutures with MockitoSugar {
  import scala.concurrent.ExecutionContext.Implicits.global

  val ebayCredentials = Seq(Map("clientId" -> "id-1", "clientSecret" -> "secret-1"), Map("clientId" -> "id-2", "clientSecret" -> "secret-2"))
  val ebayConfig = Map("baseUri" -> "/ebay", "authPath" -> "/auth", "searchPath" -> "/search", "itemPath" -> "/item", "credentials" -> ebayCredentials)
  val config: Configuration = Configuration("ebay" -> ebayConfig)

  val accessToken = "access-token"
  val itemId = "item-id-1"

  "EbaySearchClient" should {

    "make get request to obtain item details" in {
      withEbaySearchClient(200, "ebay/get-item-1-success-response.json") { ebaySearchClient =>
        val item = ebaySearchClient.getItem(accessToken, itemId)

        whenReady(item.value, timeout(10 seconds), interval(500 millis)) { foundItem =>
          foundItem.map(_.title) must be (Right("Samsung Galaxy S10 128gb UNLOCKED Prism Blue"))
        }
      }
    }
  }

  def withEbaySearchClient[T](status: Int, responseFile: String)(block: EbaySearchClient => T): T = {
    Server.withApplicationFromContext() { context =>
      new BuiltInComponentsFromContext(context) with HttpFiltersComponents {
        override def router: Router = Router.from {
          case GET(p"/ebay/item/$id") =>
            Action { req =>
              id must be ("item-id-1")
              req.contentType must be (Some("application/json"))
              req.acceptedTypes must be (MediaRange.parse("application/json"))
              req.headers.get("Authorization") must be (Some("Bearer access-token"))
              req.headers.get("X-EBAY-C-MARKETPLACE-ID") must be (Some("EBAY_GB"))
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
