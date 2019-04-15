package ru.tinkoff.fintech.stocks.db

final case class TransactionHistory(id: Option[Long],
                                    login: String,
                                    idStock: Long,
                                    amount: Int,
                                    totalPrice: Double,
                                    date: String,
                                    `type`: String)


