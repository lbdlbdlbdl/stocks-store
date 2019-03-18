package ru.tinkoff.fintech.stocks.http

object Responses {

  case class AuthData(login: String)

  case class RefreshToken(refreshToken: String)
}
