package common

import java.io.IOException

import play.api.libs.json.JsResultException
import sttp.client.DeserializationError

object errors {
  sealed trait ApiClientError extends Throwable {
    def message: String
    override def getMessage: String = message
  }

  object ApiClientError {
    final case class HttpError(status: Int, message: String) extends ApiClientError
    final case class InternalError(message: String) extends ApiClientError
    final case class AuthError(message: String) extends ApiClientError
    final case class JsonParsingError(message: String) extends ApiClientError
    final case class DbError(message: String) extends ApiClientError
    final case class NotEnoughDetailsError(message: String) extends ApiClientError
  }
}
