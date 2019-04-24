package ru.tinkoff.fintech.stocks.dao

import akka.actor.ActorSystem
import io.getquill.{Escape, PostgresAsyncContext}
import ru.tinkoff.fintech.stocks.db.StocksPackage

import scala.concurrent.{ExecutionContext, Future}

class StocksPackageDao(implicit val context: PostgresAsyncContext[Escape],
                       implicit val exctx: ExecutionContext,
                       implicit val system: ActorSystem) {

  import akka.event.Logging
  import context._

  val log = Logging.getLogger(system, this)

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
    log.info("start add stockspackage")
    run(quote {
      query[StocksPackage].insert(lift(stocksPack)).returning(_.id)
    }).map(newId => stocksPack.copy(id = newId))
  }

  def updatePackage(idUser:Long,id: Long, newCount:Int): Future[Long] = {
      log.info(s"update package id=$id new count=$newCount")
      run(quote {
        query[StocksPackage].filter(env=>env.stockId == lift(id) && env.userId==lift(idUser)).update(_.count -> lift(newCount))
      })
  }
}
