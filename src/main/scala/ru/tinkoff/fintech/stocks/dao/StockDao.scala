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
  def getStockOption(id: Long): Future[Option[Stock]] = {
    run(quote {
      query[Stock].filter(_.id == lift(id)).take(1)
    }).map(_.headOption)
  }
  def findStrInName(str: String): Future[List[Stock]] = {
    run(quote {
      query[Stock].filter(s => s.name like s"%${lift(str)}%")
    })
  }

  def getPagedQueryWithFind(searchedStr: String, offset: Int, querySize: Int): Future[List[Stock]] = {
    run(quote {
      query[Stock]
        .drop(lift(offset - 1))
        .filter(s => s.name like s"%${lift(searchedStr)}%")
        .take(lift(querySize))
    })
  }
}
