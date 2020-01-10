package exceptions

import java.io.IOException

import cats.data.EitherT
import play.api.http.Status
import play.api.libs.json.JsResultException

import scala.concurrent.Future

sealed abstract class ApiClientError(status: Int, message: String) extends Throwable

final case class HttpError(status: Int, message: String) extends ApiClientError(status, message)
final case class InternalError(message: String) extends ApiClientError(Status.INTERNAL_SERVER_ERROR, message)
final case class AuthError(message: String) extends ApiClientError(Status.UNAUTHORIZED, message)

object ApiClientError {
  type FutureErrorOr[A] = EitherT[Future, ApiClientError, A]

  def jsonParsingError(e: Throwable): ApiClientError =
    InternalError(s"error parsing json response: ${e.getMessage}")

  def recoverFromHttpCallFailure: PartialFunction[Throwable, ApiClientError] = {
    case parsingError: JsResultException =>
      InternalError(s"error parsing json: ${parsingError.getMessage}")
    case networkingError: IOException =>
      InternalError(s"connection error: ${networkingError.getMessage}")
    case error: Throwable =>
      InternalError(s"unexpected error during http call: ${error.getMessage}")
  }
}
