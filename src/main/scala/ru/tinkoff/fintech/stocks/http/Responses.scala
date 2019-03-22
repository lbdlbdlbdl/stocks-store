package ru.tinkoff.fintech.stocks.http

object Responses {

  case class Token(accessToken: String, refreshToken: String)

}
