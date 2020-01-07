package clients.ebay

import configs.{CexConfig, EbayConfig}
import exceptions.ApiClientError
import javax.inject.Inject
import play.api.http.Status
import play.api.libs.json.JsValue
import play.api.{Configuration, Logger}
import play.api.libs.ws.{WSAuthScheme, WSClient}

import scala.concurrent.{ExecutionContext, Future}

class EbayAuthClient @Inject() (config: Configuration, client: WSClient)(implicit ex: ExecutionContext) {
  private val logger: Logger = Logger(getClass)

  private val ebayConfig = config.get[EbayConfig]("ebay")

  private val authRequest = client
    .url(s"${ebayConfig.baseUri}${ebayConfig.searchPath}")
    .addHttpHeaders("Accept" -> "application/json")
    .addHttpHeaders("Content-Type" -> "application/x-www-form-urlencoded")

  private val authRequestBody = Map("scope" -> Seq("https://api.ebay.com/oauth/api_scope"), "grant_type" -> Seq("client_credentials"))

  private var currentAccountIndex = 0

  def authenticate(): Future[Either[ApiClientError, EbayAuthToken]] = {
    val credentials = ebayConfig.credentials(currentAccountIndex)
    authRequest.withAuth(credentials.clientId, credentials.clientSecret, WSAuthScheme.BASIC)
      .post(authRequestBody)
      .map(res =>
        if (Status.isSuccessful(res.status)) (res.status, res.body[JsValue].as[EbayAuthSuccessResponse])
        else (res.status, res.body[JsValue].as[EbayAuthErrorResponse])
      )
      .map {
        case (_, EbayAuthSuccessResponse(token, expiresIn, _)) => Right(EbayAuthToken(token, expiresIn))
        case (status, EbayAuthErrorResponse(error, description)) => Left(ApiClientError(status, s"error authenticating with ebay: $error-$description"))
      }
      .recover(ApiClientError.recoverFromHttpCallFailure.andThen(Left(_)))
  }
}
