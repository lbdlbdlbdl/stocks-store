package ru.tinkoff.fintech.stocks.http

object Requests {

  case class UserRequest(login: String, password: String)

  case class Token(authToken: String, refreshToken: String)

}
