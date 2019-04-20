package ru.tinkoff.fintech.stocks.db

import ru.tinkoff.fintech.stocks.http.dtos.Responses

final case class Stock(id: Long,
                       name: String,
                       code: String,
                       iconUrl: String = "icon.jpg",
                       salePrice: Double,
                       buyPrice: Double) {

  def as[T](implicit f: Stock => T) = f(this)
}

object Stock {

  implicit def toStockResponse: Stock => Responses.Stock =
    (stock: Stock) => Responses.Stock(stock.id, stock.code, stock.name, stock.iconUrl, stock.salePrice, 0.0)

  implicit def toStockHistoryResponse: Stock => Responses.StockHistory =
    (stock: Stock) => Responses.StockHistory(stock.id, stock.code, stock.name, stock.iconUrl)

}