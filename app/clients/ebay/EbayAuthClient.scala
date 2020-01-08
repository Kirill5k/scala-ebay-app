package clients.ebay

import cats.data.EitherT
import cats.implicits._
import configs.EbayConfig
import exceptions.{ApiClientError, AuthError, HttpError}
import exceptions.ApiClientError.FutureErrorOr
import javax.inject.Inject
import play.api.http.Status
import play.api.libs.json.JsValue
import play.api.{Configuration, Logger}
import play.api.libs.ws.{WSAuthScheme, WSClient}

import scala.concurrent.ExecutionContext

class EbayAuthClient @Inject() (config: Configuration, client: WSClient)(implicit ex: ExecutionContext) {
  private val logger: Logger = Logger(getClass)

  private val ebayConfig = config.get[EbayConfig]("ebay")

  private val authRequest = client
    .url(s"${ebayConfig.baseUri}${ebayConfig.searchPath}")
    .addHttpHeaders("Accept" -> "application/json")
    .addHttpHeaders("Content-Type" -> "application/x-www-form-urlencoded")

  private val authRequestBody = Map("scope" -> Seq("https://api.ebay.com/oauth/api_scope"), "grant_type" -> Seq("client_credentials"))

  private var currentAccountIndex: Int = 0
  private var authToken: FutureErrorOr[EbayAuthToken] = EitherT.leftT(AuthError("authentication with ebay is required"))

  def accessToken(): FutureErrorOr[EbayAuthToken] = {
    val t = Right("hello")
    authToken = authToken
      .ensure(AuthError("ebay token has expired"))(_.isValid)
      .orElse(authenticate())
    authToken
  }

  def switchAccount(): Unit = {
    logger.warn("switching ebay account")
    currentAccountIndex = if (currentAccountIndex+1 < ebayConfig.credentials.length) currentAccountIndex + 1 else 0
  }

  private def authenticate(): FutureErrorOr[EbayAuthToken] = {
    val credentials = ebayConfig.credentials(currentAccountIndex)
    val authResponse = authRequest.withAuth(credentials.clientId, credentials.clientSecret, WSAuthScheme.BASIC)
      .post(authRequestBody)
      .map(res =>
        if (Status.isSuccessful(res.status)) (res.status, res.body[JsValue].as[EbayAuthSuccessResponse])
        else (res.status, res.body[JsValue].as[EbayAuthErrorResponse])
      )
      .map {
        case (_, EbayAuthSuccessResponse(token, expiresIn, _)) => Right(EbayAuthToken(token, expiresIn))
        case (status, EbayAuthErrorResponse(error, description)) => Left(HttpError(status, s"error authenticating with ebay: $error-$description"))
      }
      .recover(ApiClientError.recoverFromHttpCallFailure.andThen(Left(_)))
    EitherT(authResponse)
  }
}
