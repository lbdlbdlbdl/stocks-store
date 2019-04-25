package ru.tinkoff.fintech.stocks.db

import java.time.LocalDateTime

final case class PriceHistory(id: Option[Long],
                              stockId: Long,
                              date: LocalDateTime,
                              salePrice: Double,
                              buyPrice: Double)

