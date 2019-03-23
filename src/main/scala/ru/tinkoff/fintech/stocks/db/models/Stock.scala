package ru.tinkoff.fintech.stocks.db.models

final case class Stock(
                        id: Long,
                        name: String,
                        sale: Double,
                        buy: Double,
                        code: String
                      )

object Stock {

}


