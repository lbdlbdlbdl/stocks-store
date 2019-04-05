package ru.tinkoff.fintech.stocks.http

import ru.tinkoff.fintech.stocks.db.{Stock, StocksPackage, User}

case class Companion (
                     user:User,
                     packag:Option[StocksPackage],
                     stock:Stock
                     )
