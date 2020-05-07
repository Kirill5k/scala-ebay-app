package clients.ebay.auth

import cats.effect.IO
import clients.SttpClientSpec
import domain.ApiClientError._
import play.api.Configuration
import sttp.client
import sttp.client.Response
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client.testing.SttpBackendStub
import sttp.model.{MediaType, Method, StatusCode}

import scala.concurrent.duration._
import scala.language.postfixOps

class EbayAuthClientSpec extends SttpClientSpec {

  val ebayCredentials = List(Map("clientId" -> "id-1", "clientSecret" -> "secret-1"), Map("clientId" -> "id-2", "clientSecret" -> "secret-2"))
  val ebayConfig = Map("baseUri" -> "http://ebay.com", "authPath" -> "/auth", "searchPath" -> "/search", "itemPath" -> "/item", "credentials" -> ebayCredentials)
  val config: Configuration = Configuration("ebay" -> ebayConfig)

  "EbayAuthClient" should {

    "make post request to obtain auth token" in {
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isAuthRequest(r) =>
            Response.ok(json("ebay/auth-success-response.json"))
          case _ => throw new RuntimeException()
        }

      val ebayAuthClient = new EbayAuthClient(config, sttpCatsBackend(testingBackend))
      val accessToken = ebayAuthClient.accessToken()

      whenReady(accessToken.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { token =>
        token must be ("KTeE7V9J5VTzdfKpn/nnrkj4+nbtl/fDD92Vctbbalh37c1X3fvEt7u7/uLZ93emB1uu/i5eOz3o8MfJuV7288dzu48BEAAA==")
      }
    }

    "return existing token if it is valid" in {
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case _ => throw new RuntimeException()
        }

      val ebayAuthClient = new EbayAuthClient(config, sttpCatsBackend(testingBackend))
      ebayAuthClient.authToken = IO.pure(Right(EbayAuthToken("test-token", 7200)))

      val accessToken = ebayAuthClient.accessToken()

      whenReady(accessToken.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { token =>
        token must be ("test-token")
      }
    }

    "authenticate with different account when switched" in {
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isAuthRequest(r) =>
            Response.ok(json("ebay/auth-success-response.json"))
          case _ => throw new RuntimeException()
        }

      val ebayAuthClient = new EbayAuthClient(config, sttpCatsBackend(testingBackend))
      ebayAuthClient.switchAccount()
      val accessToken = ebayAuthClient.accessToken()

      whenReady(accessToken.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { token =>
        token must be ("KTeE7V9J5VTzdfKpn/nnrkj4+nbtl/fDD92Vctbbalh37c1X3fvEt7u7/uLZ93emB1uu/i5eOz3o8MfJuV7288dzu48BEAAA==")
        ebayAuthClient.currentAccountIndex must be (1)
      }
    }

    "make another request when original token has expired" in {
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isAuthRequest(r) =>
            Response.ok(json("ebay/auth-success-response.json"))
          case _ => throw new RuntimeException()
        }

      val ebayAuthClient = new EbayAuthClient(config, sttpCatsBackend(testingBackend))
      ebayAuthClient.authToken = IO.pure(Right(EbayAuthToken("test-token", 0)))
      val accessToken = ebayAuthClient.accessToken()

      whenReady(accessToken.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { token =>
        token must be ("KTeE7V9J5VTzdfKpn/nnrkj4+nbtl/fDD92Vctbbalh37c1X3fvEt7u7/uLZ93emB1uu/i5eOz3o8MfJuV7288dzu48BEAAA==")
      }
    }

    "handle errors from ebay" in {
      val testingBackend: SttpBackendStub[IO, Nothing] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isAuthRequest(r) =>
            Response(json("ebay/auth-error-response.json"), StatusCode.BadRequest)
          case _ => throw new RuntimeException()
        }

      val ebayAuthClient = new EbayAuthClient(config, sttpCatsBackend(testingBackend))
      val accessToken = ebayAuthClient.accessToken()

      whenReady(accessToken.attempt.unsafeToFuture(), timeout(6 seconds), interval(100 millis)) { token =>
        token must be (Left(HttpError(400, "error authenticating with ebay: unsupported_grant_type: grant type in request is not supported by the authorization server")))
      }
    }

    def isAuthRequest(req: client.Request[_, _]): Boolean =
      isGoingToWithSpecificContent(req, Method.POST, "ebay.com", List("identity", "v1", "oauth2", "token"), contentType = MediaType.ApplicationXWwwFormUrlencoded)
  }
}
