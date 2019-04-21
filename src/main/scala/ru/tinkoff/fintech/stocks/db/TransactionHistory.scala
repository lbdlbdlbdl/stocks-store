package ru.tinkoff.fintech.stocks.db

import java.time.LocalDateTime

final case class TransactionHistory(id: Option[Long],
                                    login: String,
                                    stockId: Long,
                                    amount: Int,
                                    totalPrice: Double,
                                    date: LocalDateTime,
                                    `type`: String)
