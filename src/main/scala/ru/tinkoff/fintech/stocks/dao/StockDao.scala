package ru.tinkoff.fintech.stocks.dao

import io.getquill.{Escape, PostgresAsyncContext}
import ru.tinkoff.fintech.stocks.db.Stock

import scala.concurrent.{ExecutionContext, Future}

class StockDao(implicit val context: PostgresAsyncContext[Escape],
              implicit val exctx: ExecutionContext) {

  import context._

  //найдем описание акции
  def getStock(id: Long): Future[Stock] = {
    run(quote {
      query[Stock].filter(_.id == lift(id)).take(1)
    }).map(_.head)
    }
}
