package ru.tinkoff.fintech.stocks.http

object Requests {

  case class UserRequest(login: String, password: String)

  case class AuthData(login: String)

  case class RefreshToken(refreshToken: String)

  case class Transaction(stockId: Long, amount: Int)
}
