package ru.tinkoff.fintech.stocks.db

final case class Storage(id: Option[Long],
                         login: String,
                         idStock: Long,
                         count: Int)
