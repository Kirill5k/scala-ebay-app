package domain

import java.io.IOException

import cats.effect.IO
import play.api.libs.json.JsResultException

sealed trait ApiClientError extends Throwable

object ApiClientError {
  type IOErrorOr[A] = IO[Either[ApiClientError, A]]

  final case class HttpError(status: Int, message: String) extends ApiClientError
  final case class InternalError(message: String) extends ApiClientError
  final case class AuthError(message: String) extends ApiClientError
  final case class JsonParsingError(message: String) extends ApiClientError
  final case class DbError(message: String) extends ApiClientError
  final case class NotEnoughDetailsError(message: String) extends ApiClientError

  def recoverFromHttpCallFailure: PartialFunction[Throwable, ApiClientError] = {
    case parsingError: JsResultException =>
      JsonParsingError(s"error parsing json: ${parsingError.getMessage}")
    case networkingError: IOException =>
      InternalError(s"connection error: ${networkingError.getMessage}")
    case error: Throwable =>
      InternalError(s"unexpected error during http call: ${error.getMessage}")
  }

  def recoverFromDbError: PartialFunction[Throwable, ApiClientError] = {
    case error: Throwable => DbError(s"error during db operation: ${error.getMessage}")
  }
}
