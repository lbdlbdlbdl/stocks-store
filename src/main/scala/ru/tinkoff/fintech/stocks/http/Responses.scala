package ru.tinkoff.fintech.stocks.http

object Responses {

  case class Token(authToken: String, refreshToken: String)

}
