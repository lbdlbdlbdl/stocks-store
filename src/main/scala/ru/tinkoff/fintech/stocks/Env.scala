package ru.tinkoff.fintech.stocks

import ru.tinkoff.fintech.stocks.services._

case class Env(userService: UserService,
               stocksService: StocksService,
               transactionService: TransactionService);
              