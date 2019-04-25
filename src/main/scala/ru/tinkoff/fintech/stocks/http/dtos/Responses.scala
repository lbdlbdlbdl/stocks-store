package ru.tinkoff.fintech.stocks.http.dtos

import java.time.{LocalDate, LocalDateTime}

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

  case class StockResponse(id: Long,
                           code: String,
                           name: String,
                           iconUrl: String,
                           price: Double,
                           priceDelta: Double)

  case class StocksPage(nextItemId: Long, prevItemId: Long, items: List[StockResponse])

  case class StockHistory(id: Long,
                          code: String,
                          name: String,
                          iconUrl: String)

  case class TransactionHistoryResponse(stock: StockHistory,
                                amount: Int,
                                totalPrice: Double,
                                date: LocalDateTime,
                                `type`: String)

  case class TransactionHistoryPage(nextItemId: Long, prevItemId: Long, items: List[TransactionHistoryResponse])

  case class TransactionSuccess(status: String = "success")

  case class PriceHistoryResponse(id: Long,
                          code: String,
                          name: String,
                          iconUrl: String,
                          from: LocalDate,
                          to: LocalDate,
                          history: List[PricePackage])

  case class PricePackage(date: LocalDate, price: Double)

}
