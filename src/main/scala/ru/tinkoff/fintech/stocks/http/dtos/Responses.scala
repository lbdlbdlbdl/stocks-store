package ru.tinkoff.fintech.stocks.http.dtos

object Responses {

  case class Token(accessToken: String, refreshToken: String)

  case class AccountInfo(name: String, balance: Double, stocks: List[StockBatch])

  case class StockBatch(id: Long,
                        code: String,
                        name: String,
                        iconUrl: String,
                        price: Double,
                        priceDelta: Double,
                        count: Int)

  case class Stock(id: Long,
                   code: String,
                   name: String,
                   iconUrl: String,
                   price: Double,
                   priceDelta: Double)

  case class StocksPage(nextItemId: Long, prevItemId: Long, items: List[Stock])

  case class StockHistory(id: Long,
                          code: String,
                          name: String,
                          iconUrl: String)

  case class TransactionHistory(stock: StockHistory,
                                amount: Int,
                                totalPrice: Double,
                                date: String,
                                `type`: String)

  case class TransactionHistoryPage(nextItemId: Long, prevItemId: Long, items: List[TransactionHistory])

  case class TransactionSuccess(status: String = "success")

  case class PriceHistory(
                           id: Long,
                           code: String,
                           name: String,
                           iconUrl: String,
                           from: String,
                           to: String,
                           history: List[PricePackage])

  case class PricePackage(date: String, price: Double)

}
