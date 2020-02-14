package clients.ebay.auth

import cats.effect.IO
import domain.ApiClientError._
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.http.MediaRange
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.mvc.Results
import play.api.routing.Router
import play.api.routing.sird._
import play.api.test.WsTestClient
import play.api.{BuiltInComponentsFromContext, Configuration}
import play.core.server.Server
import play.filters.HttpFiltersComponents

import scala.collection.immutable.ListMap

class EbayAuthClientSpec extends PlaySpec with ScalaFutures with MockitoSugar {
  import scala.concurrent.ExecutionContext.Implicits.global

  val ebayCredentials = Seq(Map("clientId" -> "id-1", "clientSecret" -> "secret-1"), Map("clientId" -> "id-2", "clientSecret" -> "secret-2"))
  val ebayConfig = Map("baseUri" -> "/ebay", "authPath" -> "/auth", "searchPath" -> "/search", "itemPath" -> "/item", "credentials" -> ebayCredentials)
  val config: Configuration = Configuration("ebay" -> ebayConfig)

  "EbayAuthClient" should {

    "make post request to obtain auth token" in {
      withEbayAuthClient(200, "ebay/auth-success-response.json", "Basic aWQtMTpzZWNyZXQtMQ==") { ebayAuthClient =>
        val accessToken = ebayAuthClient.accessToken()

        whenReady(accessToken.unsafeToFuture()) { token =>
          token must be ("KTeE7V9J5VTzdfKpn/nnrkj4+nbtl/fDD92Vctbbalh37c1X3fvEt7u7/uLZ93emB1uu/i5eOz3o8MfJuV7288dzu48BEAAA==")
        }
      }
    }

    "return existing token if it is valid" in {
      val (wsClientMock, wsRequestMock) = mockedWSClient()
      val ebayAuthClient = new EbayAuthClient(config, wsClientMock)
      ebayAuthClient.authToken = IO.pure(Right(EbayAuthToken("test-token", 7200)))

      val accessToken = ebayAuthClient.accessToken()

      whenReady(accessToken.unsafeToFuture()) { token =>
        token must be ("test-token")
        verify(wsRequestMock, times(2)).addHttpHeaders(any)
        verifyNoMoreInteractions(wsRequestMock)
      }
    }

    "authenticate with different account when switched" in {
      withEbayAuthClient(200, "ebay/auth-success-response.json", "Basic aWQtMjpzZWNyZXQtMg==") { ebayAuthClient =>
        ebayAuthClient.switchAccount()
        val accessToken = ebayAuthClient.accessToken()

        whenReady(accessToken.unsafeToFuture()) { token =>
          token must be ("KTeE7V9J5VTzdfKpn/nnrkj4+nbtl/fDD92Vctbbalh37c1X3fvEt7u7/uLZ93emB1uu/i5eOz3o8MfJuV7288dzu48BEAAA==")
          ebayAuthClient.currentAccountIndex must be (1)
        }
      }
    }

    "make another request when original token has expired" in {
      withEbayAuthClient(200, "ebay/auth-success-response.json", "Basic aWQtMTpzZWNyZXQtMQ==") { ebayAuthClient =>
        ebayAuthClient.authToken = IO.pure(Right(EbayAuthToken("test-token", 0)))
        val accessToken = ebayAuthClient.accessToken()

        whenReady(accessToken.unsafeToFuture()) { token =>
          token must be ("KTeE7V9J5VTzdfKpn/nnrkj4+nbtl/fDD92Vctbbalh37c1X3fvEt7u7/uLZ93emB1uu/i5eOz3o8MfJuV7288dzu48BEAAA==")
        }
      }
    }

    "handle errors from ebay" in {
      withEbayAuthClient(400, "ebay/auth-error-response.json", "Basic aWQtMTpzZWNyZXQtMQ==") { ebayAuthClient =>
        val accessToken = ebayAuthClient.accessToken()

        whenReady(accessToken.unsafeToFuture()) { token =>
          token must be (HttpError(400, "error authenticating with ebay: unsupported_grant_type-grant type in request is not supported by the authorization server"))
        }
      }
    }
  }

  def withEbayAuthClient[T](status: Int, responseFile: String, expectedAuthHeader: String)(block: EbayAuthClient => T): T = {
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

  def mockedWSClient(): (WSClient, WSRequest) = {
    val client = mock[WSClient]
    val request = mock[WSRequest]
    when(client.url(*)).thenReturn(request)
    when(request.addHttpHeaders(*)).thenReturn(request)
    (client, request)
  }
}
