package ru.tinkoff.fintech.stocks.db

final case class Stock(id: Long,
                       name: String,
                       code: String,
                       iconUrl: String = "icon.jpg",
                       salePrice: Double,
                       buyPrice: Double)