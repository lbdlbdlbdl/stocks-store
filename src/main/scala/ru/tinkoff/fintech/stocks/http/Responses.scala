package ru.tinkoff.fintech.stocks.http
import ru.tinkoff.fintech.stocks.db.models.Stock

object Responses {

  case class Token(authToken: String, refreshToken: String)

  case class UserInfo(
                        name:String,
                        balance:Double,
                        stocks:List[Stock]
                        )

}
