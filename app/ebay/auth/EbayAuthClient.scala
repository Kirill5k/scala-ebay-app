package ebay.auth

import cats.data.EitherT
import cats.implicits._
import ebay.EbayConfig
import exceptions.ApiClientError.FutureErrorOr
import exceptions.{ApiClientError, AuthError, HttpError}
import javax.inject.Inject
import play.api.http.{HeaderNames, Status}
import play.api.libs.ws.{WSAuthScheme, WSClient}
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext

class EbayAuthClient @Inject() (config: Configuration, client: WSClient)(implicit ex: ExecutionContext) {
  private val logger: Logger = Logger(getClass)

  private val ebayConfig = config.get[EbayConfig]("ebay")

  private val authRequest = client
    .url(s"${ebayConfig.baseUri}${ebayConfig.authPath}")
    .addHttpHeaders(HeaderNames.ACCEPT -> "application/json")
    .addHttpHeaders(HeaderNames.CONTENT_TYPE -> "application/x-www-form-urlencoded")

  private val authRequestBody = Map("scope" -> Seq("https://api.ebay.com/oauth/api_scope"), "grant_type" -> Seq("client_credentials"))

  private[auth] var currentAccountIndex: Int = 0
  private[auth] var authToken: FutureErrorOr[EbayAuthToken] = EitherT.leftT(AuthError("authentication with ebay is required"))

  def accessToken(): FutureErrorOr[String] = {
    authToken = authToken
      .ensure(AuthError("ebay token has expired"))(_.isValid)
      .orElse(authenticate())
    authToken.map(_.token)
  }

  def switchAccount(): Unit = {
    logger.warn("switching ebay account")
    currentAccountIndex = if (currentAccountIndex+1 < ebayConfig.credentials.length) currentAccountIndex + 1 else 0
  }

  private def authenticate(): FutureErrorOr[EbayAuthToken] = {
    val credentials = ebayConfig.credentials(currentAccountIndex)
    logger.info(s"authenticated with ebay account ${credentials.clientId}")
    val authResponse = authRequest
      .withAuth(credentials.clientId, credentials.clientSecret, WSAuthScheme.BASIC)
      .post(authRequestBody)
      .map(res =>
        if (Status.isSuccessful(res.status))
          res.body[Either[ApiClientError, EbayAuthSuccessResponse]]
            .map(s => EbayAuthToken(s.access_token, s.expires_in))
        else
          res.body[Either[ApiClientError, EbayAuthErrorResponse]]
            .flatMap(e => HttpError(res.status, s"error authenticating with ebay: ${e.error}-${e.error_description}").asLeft)
      )
      .recover(ApiClientError.recoverFromHttpCallFailure.andThen(_.asLeft))
    EitherT(authResponse)
  }
}
