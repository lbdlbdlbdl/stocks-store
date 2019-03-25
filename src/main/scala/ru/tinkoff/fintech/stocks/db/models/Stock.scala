package ru.tinkoff.fintech.stocks.db.models

final case class StockBd(
                        id: Long,
                        code: String,
                        name: String,
                        iconUrl: Option[String],
                        sale: Double,
                        buy: Double
                      )

final case class Stock(
                        id: Long,
                        code: String,
                        name: String,
                        iconUrl: Option[String],
                        price: Double,
                        priceDelta: Double,
                        count: Int
                      )




