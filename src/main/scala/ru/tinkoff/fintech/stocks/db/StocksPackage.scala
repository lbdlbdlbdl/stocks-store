package ru.tinkoff.fintech.stocks.db

final case class StocksPackage(id: Option[Long],
                               userId: Long,
                               stockId: Long,
                               count: Int)
