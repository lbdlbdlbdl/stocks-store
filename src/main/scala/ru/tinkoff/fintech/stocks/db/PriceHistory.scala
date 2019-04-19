package ru.tinkoff.fintech.stocks.db

final case class PriceHistory(id: Option[Long],
                              stockId: Long,
                              date: String,
                              salePrice: Double,
                              buyPrice: Double)
