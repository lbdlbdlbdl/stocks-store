package ru.tinkoff.fintech.stocks.http

object Responses {

  case class Token(authToken: String, refreshToken: String)

  case class AccountInfo(name: String, balance: Double, stocks: List[Stock])

  case class Stock(id: Long,
                   code: String,
                   name: String,
                   iconUrl: String = "icon.jpg",
                   price: Double,
                   priceDelta: Double,
                   count: Int)

}
