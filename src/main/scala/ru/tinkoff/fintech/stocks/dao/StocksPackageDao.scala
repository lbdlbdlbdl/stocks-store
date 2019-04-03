package ru.tinkoff.fintech.stocks.dao

import io.getquill.{Escape, PostgresAsyncContext}
import ru.tinkoff.fintech.stocks.db.{StocksPackage, User}

import scala.concurrent.{ExecutionContext, Future}

/*
  Примечание:
    Пакет(контекст)- набор из акции (Stock) и её количества
    У User может быть пакетов не больше чем весь ассортимент акций
    1 пакет - 1 акция для User
*/

class StocksPackageDao(implicit val context: PostgresAsyncContext[Escape],
                       implicit val exctx: ExecutionContext) {

  import context._

  //найдем пакет акции
  def find(userId: Long): Future[List[StocksPackage]] = {
    run(quote {
      query[StocksPackage].filter(_.userId == lift(userId))
    })
  }

  def add(stocksPack: StocksPackage): Future[StocksPackage] = {
    run(quote {
      query[StocksPackage].insert(lift(stocksPack)).returning(_.id)
    }).map(newId => stocksPack.copy(id = newId))
  }

//  def add(stocksPack: StocksPackage): Future[StocksPackage] = {
//    run(quote {
//      query[StocksPackage].insert(lift(stocksPack)).returning(sp => (sp.userId, sp.stockId))
//    }).map(t => stocksPack.copy(userId = t._1, stockId = t._2))
//  }

}
