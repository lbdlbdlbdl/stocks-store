package ru.tinkoff.fintech.stocks.dao

import akka.actor.ActorSystem
import io.getquill.{Escape, PostgresAsyncContext}
import ru.tinkoff.fintech.stocks.db.PriceHistory

import scala.concurrent.{ExecutionContext, Future}

class PriceHistoryDao(implicit val context: PostgresAsyncContext[Escape],
                      implicit val exctx: ExecutionContext,
                      implicit val system: ActorSystem) {

  import context._

  def find(idStock:Long):Future[List[PriceHistory]] = {
    run(quote {
      query[PriceHistory].filter(_.stockId == lift(idStock))
    })
  }

  def add(history: PriceHistory): Future[Unit] = {
    run(quote {
      query[PriceHistory].insert(lift(history)).returning(_.id)
    }).map(newId => history.copy(id = newId))
  }


}

