package ebay.auth

import exceptions.HttpError
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
import scala.concurrent.duration._
import scala.language.postfixOps

class EbayAuthClientSpec extends PlaySpec with ScalaFutures {
  import scala.concurrent.ExecutionContext.Implicits.global

  val ebayCredentials = Seq(Map("clientId" -> "id-1", "clientSecret" -> "secret-1"), Map("clientId" -> "id-2", "clientSecret" -> "secret-2"))
  val ebayConfig = Map("baseUri" -> "/ebay", "authPath" -> "/auth", "searchPath" -> "/search", "itemPath" -> "/item", "credentials" -> ebayCredentials)
  val config: Configuration = Configuration("ebay" -> ebayConfig)

  "EbayAuthClient" should {

    "make post request to obtain auth token" in {
      withEbayAuthClient(200, "ebay/auth-success-response.json") { client =>
        val accessToken = client.accessToken()

        whenReady(accessToken.value, timeout(6 seconds), interval(500 millis)) { token =>
          token must be (Right("KTeE7V9J5VTzdfKpn/nnrkj4+nbtl/fDD92Vctbbalh37c1X3fvEt7u7/uLZ93emB1uu/i5eOz3o8MfJuV7288dzu48BEAAA=="))
        }
      }
    }
  }

  def withEbayAuthClient[T](status: Int, responseFile: String, expectedAuthHeader: String = "Basic aWQtMTpzZWNyZXQtMQ==")(block: EbayAuthClient => T): T = {
    Server.withApplicationFromContext() { context =>
      new BuiltInComponentsFromContext(context) with HttpFiltersComponents {
        override def router: Router = Router.from {
          case POST(p"/ebay/auth") =>
            Action { req =>
              req.contentType must be (Some("application/x-www-form-urlencoded"))
              req.acceptedTypes must be (MediaRange.parse("application/json"))
              req.headers.get("Authorization") must be (Some(expectedAuthHeader))
              req.body.asFormUrlEncoded must be (Some(ListMap("scope" -> Seq("https://api.ebay.com/oauth/api_scope"), "grant_type" -> Seq("client_credentials"))))
              Results.Status(status).sendResource(responseFile)(executionContext, fileMimeTypes)
            }
        }
      }.application
    } { implicit port =>
      WsTestClient.withClient { client =>
        block(new EbayAuthClient(config, client))
      }
    }
  }
}
