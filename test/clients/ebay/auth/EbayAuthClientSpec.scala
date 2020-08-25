package clients.ebay.auth

import cats.effect.IO
import cats.effect.concurrent.Ref
import clients.SttpClientSpec
import common.errors.ApiClientError.HttpError
import sttp.client
import sttp.client.Response
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client.testing.SttpBackendStub
import sttp.model.{Header, HeaderNames, MediaType, Method, StatusCode}

class EbayAuthClientSpec extends SttpClientSpec {

  "EbayAuthClient" should {

    "make post request to obtain auth token" in {
      val testingBackend: SttpBackendStub[IO, Nothing, WebSocketHandler] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isAuthRequest(r) =>
            Response.ok(json("ebay/auth-success-response.json"))
          case _ => throw new RuntimeException()
        }

      val ebayAuthClient = new EbayAuthClient(sttpCatsBackend(testingBackend))
      val accessToken = ebayAuthClient.accessToken

      accessToken.unsafeToFuture().map { token =>
        token must be ("KTeE7V9J5VTzdfKpn/nnrkj4+nbtl/fDD92Vctbbalh37c1X3fvEt7u7/uLZ93emB1uu/i5eOz3o8MfJuV7288dzu48BEAAA==")
      }
    }

    "return existing token if it is valid" in {
      val testingBackend: SttpBackendStub[IO, Nothing, WebSocketHandler] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case _ => throw new RuntimeException()
        }

      val ebayAuthClient = new EbayAuthClient(sttpCatsBackend(testingBackend))
      ebayAuthClient.authTokenRef = Ref.of(Right(EbayAuthToken("test-token", 7200)))

      val accessToken = ebayAuthClient.accessToken

      accessToken.unsafeToFuture().map { token =>
        token must be ("test-token")
      }
    }

    "authenticate with different account when switched" in {
      val testingBackend: SttpBackendStub[IO, Nothing, WebSocketHandler] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isAuthRequest(r) =>
            Response.ok(json("ebay/auth-success-response.json"))
          case _ => throw new RuntimeException()
        }

      val ebayAuthClient = new EbayAuthClient(sttpCatsBackend(testingBackend))
      ebayAuthClient.switchAccount()
      val accessToken = ebayAuthClient.accessToken

      accessToken.unsafeToFuture().map { token =>
        token must be ("KTeE7V9J5VTzdfKpn/nnrkj4+nbtl/fDD92Vctbbalh37c1X3fvEt7u7/uLZ93emB1uu/i5eOz3o8MfJuV7288dzu48BEAAA==")
        ebayAuthClient.currentAccountIndex must be (1)
      }
    }

    "make another request when original token has expired" in {
      val testingBackend: SttpBackendStub[IO, Nothing, WebSocketHandler] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isAuthRequest(r) =>
            Response.ok(json("ebay/auth-success-response.json"))
          case _ => throw new RuntimeException()
        }

      val ebayAuthClient = new EbayAuthClient(sttpCatsBackend(testingBackend))
      ebayAuthClient.authTokenRef = Ref.of(Right(EbayAuthToken("test-token", 0)))
      val accessToken = ebayAuthClient.accessToken

      accessToken.unsafeToFuture().map { token =>
        token must be ("KTeE7V9J5VTzdfKpn/nnrkj4+nbtl/fDD92Vctbbalh37c1X3fvEt7u7/uLZ93emB1uu/i5eOz3o8MfJuV7288dzu48BEAAA==")
      }
    }

    "handle errors from ebay" in {
      val testingBackend: SttpBackendStub[IO, Nothing, WebSocketHandler] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isAuthRequest(r) =>
            Response(json("ebay/auth-error-response.json"), StatusCode.BadRequest)
          case _ => throw new RuntimeException()
        }

      val ebayAuthClient = new EbayAuthClient(sttpCatsBackend(testingBackend))
      val accessToken = ebayAuthClient.accessToken

      accessToken.attempt.unsafeToFuture().map { token =>
        token must be (Left(HttpError(400, "error authenticating with ebay: unsupported_grant_type: grant type in request is not supported by the authorization server")))
      }
    }

    "switch account when 429 received" in {
      val testingBackend: SttpBackendStub[IO, Nothing, WebSocketHandler] = AsyncHttpClientCatsBackend
        .stub[IO]
        .whenRequestMatchesPartial {
          case r if isAuthRequest(r) && r.headers.contains(Header(HeaderNames.Authorization, "Basic aWQtMTpzZWNyZXQtMQ==")) =>
            Response(json("ebay/auth-error-response.json"), StatusCode.TooManyRequests)
          case r if isAuthRequest(r) && r.headers.contains(Header(HeaderNames.Authorization, "Basic aWQtMjpzZWNyZXQtMg==")) =>
            Response.ok(json("ebay/auth-success-response.json"))
          case _ => throw new RuntimeException()
        }

      val ebayAuthClient = new EbayAuthClient(sttpCatsBackend(testingBackend))
      val accessToken = ebayAuthClient.accessToken

      accessToken.unsafeToFuture().map { token =>
        token must be ("KTeE7V9J5VTzdfKpn/nnrkj4+nbtl/fDD92Vctbbalh37c1X3fvEt7u7/uLZ93emB1uu/i5eOz3o8MfJuV7288dzu48BEAAA==")
      }
    }

    def isAuthRequest(req: client.Request[_, _]): Boolean =
      isGoingToWithSpecificContent(req, Method.POST, "ebay.com", List("identity", "v1", "oauth2", "token"), contentType = MediaType.ApplicationXWwwFormUrlencoded)
  }
}
