package ru.tinkoff.fintech.stocks.http

object Responses {

  case class Token(accessToken: String, refreshToken: String)

  case class AccountInfo(name: String, balance: Double, stocks: List[StockBatch])

  case class StockBatch(id: Long,
                        code: String,
                        name: String,
                        iconUrl: String = "icon.jpg",
                        price: Double,
                        priceDelta: Double,
                        count: Int)

  case class StocksPage(nextItemId: Long, prevItemId: Long, items: List[Stock])

  case class Stock(id: Long,
                    code: String,
                    name: String,
                    iconUrl: String = "icon.jpg",
                    price: Double,
                    priceDelta: Double)

  case class HistoryTransaction(stock:StockHistory,
                                 amount:Int,
                                 totalPrice:Double,
                                 date:String,
                                 `type`:String)

  case class StockHistory(id: Long,
                           code: String,
                           name: String,
                           iconUrl: String = "icon.jpg")


  case class TransactionSuccess(status: String = "success")

}
