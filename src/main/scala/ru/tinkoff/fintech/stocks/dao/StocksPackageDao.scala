package ru.tinkoff.fintech.stocks.dao

import ru.tinkoff.fintech.stocks.db.models.StocksPackage

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class StocksPackageDao{

  import quillContext._

  def find(userId: Long): Future[List[StocksPackage]] = {
    run(quote {
      query[StocksPackage].filter(_.userId == lift(userId))
    })
  }

  def findByStock(userId: Long, stockId: Long): Future[Option[StocksPackage]] = {
    run(quote {
      query[StocksPackage].filter(x => x.userId == lift(userId) && x.stockId == lift(stockId)).take(1)
    }).map(_.headOption)
  }

  def add(stocksPack: StocksPackage): Future[StocksPackage] = {
    run(quote {
      query[StocksPackage].insert(lift(stocksPack)).returning(_.id)
    }).map(newId => stocksPack.copy(id = newId))
  }
}