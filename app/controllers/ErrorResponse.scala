package controllers

sealed trait ErrorResponse

object ErrorResponse {
  final case class GeneralErrorResponse(message: String) extends ErrorResponse
}