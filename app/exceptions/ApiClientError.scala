package exceptions

import play.api.http.Status

case class ApiClientError(status: Int, message: String) extends Throwable
