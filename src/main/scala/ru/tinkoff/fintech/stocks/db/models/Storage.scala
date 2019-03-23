package ru.tinkoff.fintech.stocks.db.models

final case class Storage(
                          id: Long,
                          idStocks: Long,
                          count: Int
                        )

object Storage {

}
