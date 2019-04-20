package ru.tinkoff.fintech.stocks

import akka.event.LoggingAdapter
import ru.tinkoff.fintech.stocks.dao._
import ru.tinkoff.fintech.stocks.services._


case class Env(logger: LoggingAdapter,
               userService: UserService,
               stocksService: StocksService,
               transactionService: TransactionService,

               userDao: UserDao,
               stockDao: StockDao,
               stocksPackageDao: StocksPackageDao,
               transactionHistoryDao: TransactionHistoryDao,
               priceHistoryDao: PriceHistoryDao);
