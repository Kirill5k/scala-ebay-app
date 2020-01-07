package exceptions

import java.io.IOException

import play.api.http.Status
import play.api.libs.json.JsResultException

case class ApiClientError(status: Int, message: String) extends Throwable


object ApiClientError {
  def recoverFromHttpCallFailure: PartialFunction[Throwable, ApiClientError] = {
    case parsingError: JsResultException =>
      ApiClientError(Status.INTERNAL_SERVER_ERROR, s"error parsing json: ${parsingError.getMessage}")
    case networkingError: IOException =>
      ApiClientError(Status.INTERNAL_SERVER_ERROR, s"connection error: ${networkingError.getMessage}")
    case error: Throwable =>
      ApiClientError(Status.INTERNAL_SERVER_ERROR, s"unexpected error during http call: ${error.getMessage}")
  }
}
