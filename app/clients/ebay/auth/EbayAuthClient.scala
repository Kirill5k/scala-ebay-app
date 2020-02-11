package clients.ebay.auth

import cats.implicits._
import cats.effect.{ContextShift, IO}
import clients.ebay.EbayConfig
import javax.inject._
import play.api.http.{HeaderNames, Status}
import play.api.libs.ws.{WSAuthScheme, WSClient}
import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}
import EbayAuthResponse._
import domain.ApiClientError
import domain.ApiClientError._

@Singleton
private[ebay] class EbayAuthClient @Inject()(config: Configuration, client: WSClient)(implicit ex: ExecutionContext) {
  private implicit val cs: ContextShift[IO] = IO.contextShift(ex)

  private val ebayConfig = config.get[EbayConfig]("ebay")

  private val authRequest = client
    .url(s"${ebayConfig.baseUri}${ebayConfig.authPath}")
    .addHttpHeaders(HeaderNames.ACCEPT -> "application/json")
    .addHttpHeaders(HeaderNames.CONTENT_TYPE -> "application/x-www-form-urlencoded")

  private val authRequestBody = Map("scope" -> Seq("https://api.ebay.com/oauth/api_scope"), "grant_type" -> Seq("client_credentials"))

  private[auth] var currentAccountIndex: Int = 0
  private[auth] var authToken: IO[Either[ApiClientError, EbayAuthToken]] = IO.pure(Left(AuthError("authentication with ebay is required")))

  def accessToken(): IO[String] = {
    authToken = for {
      currentToken <- authToken
      validToken <- if (currentToken.exists(_.isValid)) IO.pure(currentToken) else IO.fromFuture(IO.pure(authenticate()))
    } yield validToken
    authToken.flatMap(_.fold(IO.raiseError, IO.pure)).map(_.token)
  }

  def switchAccount(): Unit = {
    currentAccountIndex = if (currentAccountIndex + 1 < ebayConfig.credentials.length) currentAccountIndex + 1 else 0
  }

  private def authenticate(): Future[Either[ApiClientError, EbayAuthToken]] = {
    val credentials = ebayConfig.credentials(currentAccountIndex)
    authRequest
      .withAuth(credentials.clientId, credentials.clientSecret, WSAuthScheme.BASIC)
      .post(authRequestBody)
      .map { res =>
        if (Status.isSuccessful(res.status))
          res.body[Either[ApiClientError, EbayAuthSuccessResponse]].map(s => EbayAuthToken(s.access_token, s.expires_in))
        else
          res.body[Either[ApiClientError, EbayAuthErrorResponse]].flatMap(toApiClientError(res.status))
      }
      .recover(ApiClientError.recoverFromHttpCallFailure.andThen(_.asLeft))
  }

  private def toApiClientError[A](status: Int)(authError: EbayAuthErrorResponse): Either[ApiClientError, A] =
    HttpError(status, s"error authenticating with ebay: ${authError.error}-${authError.error_description}").asLeft
}
