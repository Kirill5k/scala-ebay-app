package clients.ebay.auth

import cats.effect.IO
import cats.implicits._
import common.config.AppConfig
import common.resources.SttpBackendResource
import domain.ApiClientError
import domain.ApiClientError._
import io.circe.generic.auto._
import io.circe.parser._
import javax.inject._
import play.api.Logger
import sttp.client._
import sttp.client.circe._
import sttp.model.{HeaderNames, MediaType}

@Singleton
private[ebay] class EbayAuthClient @Inject()(catsSttpBackendResource: SttpBackendResource[IO]) {
  import EbayAuthClient._

  private val log: Logger = Logger(getClass)
  private val ebayConfig = AppConfig.load().ebay

  private val expiredToken: IO[Left[AuthError, Nothing]] = IO.pure(Left(AuthError("authentication with ebay is required")))

  private[auth] var currentAccountIndex: Int = 0
  private[auth] var authToken: IO[Either[ApiClientError, EbayAuthToken]] = expiredToken

  def accessToken(): IO[String] = {
    authToken = for {
      currentToken <- authToken
      validToken <- if (currentToken.exists(_.isValid)) IO.pure(currentToken) else authenticate()
    } yield validToken
    authToken.flatMap(_.fold(IO.raiseError, IO.pure)).map(_.token)
  }

  def switchAccount(): Unit = {
    currentAccountIndex = if (currentAccountIndex + 1 < ebayConfig.credentials.length) currentAccountIndex + 1 else 0
    authToken = expiredToken
  }

  private def authenticate(): IO[Either[ApiClientError, EbayAuthToken]] =
    catsSttpBackendResource.get.use { implicit b =>
      val credentials = ebayConfig.credentials(currentAccountIndex)
      basicRequest
        .header(HeaderNames.Accept, MediaType.ApplicationJson.toString())
        .contentType(MediaType.ApplicationXWwwFormUrlencoded)
        .auth.basic(credentials.clientId, credentials.clientSecret)
        .post(uri"${ebayConfig.baseUri}/identity/v1/oauth2/token")
        .body(Map("scope" -> "https://api.ebay.com/oauth/api_scope", "grant_type" -> "client_credentials"))
        .response(asJson[EbayAuthSuccessResponse])
        .send()
        .flatMap { r =>
          r.body match {
            case Right(token) =>
              IO.pure(Right(EbayAuthToken(token.access_token, token.expires_in)))
            case Left(error) =>
              val message = decode[EbayAuthErrorResponse](error.body)
                .fold(_ => error.body, e => s"${e.error}: ${e.error_description}")
              IO(log.error(s"error authenticating with ebay ${r.code}: $message")) *>
                IO.pure(Left(ApiClientError.HttpError(r.code.code, s"error authenticating with ebay: ${message}")))
          }
        }
    }
}

object EbayAuthClient {
  final case class EbayAuthSuccessResponse(access_token: String, expires_in: Long, token_type: String)
  final case class EbayAuthErrorResponse(error: String, error_description: String)
}
