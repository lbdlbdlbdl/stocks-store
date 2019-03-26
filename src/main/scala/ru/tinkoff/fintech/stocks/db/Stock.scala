package ru.tinkoff.fintech.stocks.db

final case class Stock(id: Long,
                       code: String,
                       name: String,
                       iconUrl: String = "icon.jpg",
                       sale: Double,
                       buy: Double)
