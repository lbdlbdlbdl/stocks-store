package ru.tinkoff.fintech.stocks.http.dtos

object Requests {

  case class UserRequest(login: String, password: String)

  case class AuthData(login: String)

  case class RefreshToken(refreshToken: String)

  case class Transaction(stockId: Long, amount: Int) {
    require(amount >= 0, "amount mast have positive value")
  }

  case class PageParameters(search: Option[String], count: Option[Int], itemId: Option[Int])

  //    require(3 <= search.get.length && search.get.length <= 100, "search string length must be between 3 and 100")
  //    require(1 <= count.get && count.get <= 50, "count in query must be between 1 and 50")
  //    require(1 <= itemId.get, "itemId must be minimum 1")

  case class RangeHistory(range: Option[String])

}
