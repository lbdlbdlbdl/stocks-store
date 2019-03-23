package ru.tinkoff.fintech.stocks.dao

import io.getquill.{Escape, PostgresAsyncContext}
import ru.tinkoff.fintech.stocks.db.models.Stock

import scala.concurrent.{ExecutionContext, Future}

class UserStock(implicit val context: PostgresAsyncContext[Escape],
              implicit val exctx: ExecutionContext) {

  import context._

  //найдем описание акции
  def findInStorage(id: Long): Future[Option[Stock]] = {
    run(quote {
      query[Stock].filter(_.id == lift(id)).take(1)
    }).map(_.headOption)
  }

}
