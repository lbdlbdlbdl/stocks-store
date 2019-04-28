package ru.tinkoff.fintech.stocks.dao

import ru.tinkoff.fintech.stocks.db.models.StocksPackage

import scala.concurrent.{ExecutionContext, Future}

class StocksPackageDao(implicit val ec: ExecutionContext) {

  import quillContext._

  def find(userId: Long, with0count: Boolean): Future[List[StocksPackage]] = {
    if (with0count) //TODO:
      run(quote {
        query[StocksPackage]
          .filter(_.userId == lift(userId))
      })
    else
      run(quote {
        query[StocksPackage]
          .filter(_.userId == lift(userId))
          .filter(_.count != 0)
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

  def updatePackage(id: Long, newCount: Int): Future[Unit] = {
    run(quote {
      query[StocksPackage].filter(_.id.forall(_ == lift(id))).update(_.count -> lift(newCount))
    }).map(_ => ())
  }

}
