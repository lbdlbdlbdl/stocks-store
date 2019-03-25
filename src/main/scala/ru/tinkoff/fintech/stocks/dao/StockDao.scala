package ru.tinkoff.fintech.stocks.dao

import io.getquill.{Escape, PostgresAsyncContext}
import ru.tinkoff.fintech.stocks.db.models.StockBd

import scala.concurrent.{ExecutionContext, Future}

class StockDao(implicit val context: PostgresAsyncContext[Escape],
              implicit val exctx: ExecutionContext) {

  import context._

  //найдем описание акции
  def infoStock(id: Long): Future[StockBd] = {
    run(quote {
      query[StockBd].filter(_.id == lift(id)).take(1)
    }).map(_.head)
    }
}
