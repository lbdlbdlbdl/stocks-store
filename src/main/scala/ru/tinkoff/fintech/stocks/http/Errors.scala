package ru.tinkoff.fintech.stocks.http

case class Error(httpCode: Int, description: String)

case class ErrorMessage(message: String)

object Errors {

  val InternalServerError = Error(500, "Internal Server Error")

}
