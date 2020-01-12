package domain

import java.io.IOException

import cats.data.EitherT
import play.api.libs.json.JsResultException

import scala.concurrent.Future

sealed trait ApiClientError {
  def message: String
}

object ApiClientError {
  type FutureErrorOr[A] = EitherT[Future, ApiClientError, A]

  final case class HttpError(status: Int, message: String) extends ApiClientError
  final case class InternalError(message: String) extends ApiClientError
  final case class AuthError(message: String) extends ApiClientError
  final case class JsonParsingError(message: String) extends ApiClientError

  def recoverFromHttpCallFailure: PartialFunction[Throwable, ApiClientError] = {
    case parsingError: JsResultException =>
      JsonParsingError(s"error parsing json: ${parsingError.getMessage}")
    case networkingError: IOException =>
      InternalError(s"connection error: ${networkingError.getMessage}")
    case error: Throwable =>
      InternalError(s"unexpected error during http call: ${error.getMessage}")
  }
}
