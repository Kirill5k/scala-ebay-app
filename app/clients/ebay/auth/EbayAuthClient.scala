package clients.ebay.auth

import cats.effect.IO
import cats.effect.concurrent.Ref
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
import sttp.model.{HeaderNames, MediaType, StatusCode}

@Singleton
private[ebay] class EbayAuthClient @Inject()(catsSttpBackendResource: SttpBackendResource[IO]) {
  import EbayAuthClient._

  private val log: Logger = Logger(getClass)
  private val ebayConfig  = AppConfig.load().ebay

  private val expiredToken: IO[Ref[IO, Either[ApiClientError, EbayAuthToken]]] =
    Ref.of[IO, Either[ApiClientError, EbayAuthToken]](Left(AuthError("authentication with ebay is required")))

  private[auth] var currentAccountIndex: Int = 0
  private[auth] var authTokenRef             = expiredToken

  def accessToken(): IO[String] =
    for {
      tokenRef <- authTokenRef
      token    <- tokenRef.get
      validToken <- if (token.exists(_.isValid)) IO.fromEither(token)
                    else authenticate().flatMap(t => tokenRef.set(t) *> IO.fromEither(t))
    } yield validToken.token

  def switchAccount(): Unit = {
    log.warn("switching ebay account")
    currentAccountIndex = if (currentAccountIndex + 1 < ebayConfig.credentials.length) currentAccountIndex + 1 else 0
    authTokenRef = expiredToken
  }

  private def authenticate(): IO[Either[ApiClientError, EbayAuthToken]] =
    catsSttpBackendResource.get.use { implicit b =>
      val credentials = ebayConfig.credentials(currentAccountIndex)
      basicRequest
        .header(HeaderNames.Accept, MediaType.ApplicationJson.toString())
        .contentType(MediaType.ApplicationXWwwFormUrlencoded)
        .auth
        .basic(credentials.clientId.trim, credentials.clientSecret.trim)
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
                (if (r.code == StatusCode.TooManyRequests) IO(switchAccount()) *> authenticate()
                 else IO.pure(Left(ApiClientError.HttpError(r.code.code, s"error authenticating with ebay: $message"))))
          }
        }
    }
}

object EbayAuthClient {
  final case class EbayAuthSuccessResponse(access_token: String, expires_in: Long, token_type: String)

  final case class EbayAuthErrorResponse(error: String, error_description: String)
}
