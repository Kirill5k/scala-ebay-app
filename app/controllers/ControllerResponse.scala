package controllers

sealed trait ControllerResponse

object ControllerResponse {
  final case class ErrorResponse(message: String) extends ControllerResponse
}