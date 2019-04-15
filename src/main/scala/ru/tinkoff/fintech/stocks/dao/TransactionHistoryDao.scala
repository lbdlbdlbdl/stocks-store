package ru.tinkoff.fintech.stocks.dao

import akka.actor.ActorSystem
import io.getquill.{Escape, PostgresAsyncContext}
import ru.tinkoff.fintech.stocks.db.{Stock, TransactionHistory}

import scala.concurrent.{ExecutionContext, Future}

class TransactionHistoryDao(implicit val context: PostgresAsyncContext[Escape],
                            implicit val exctx: ExecutionContext,
                            implicit val system: ActorSystem) {

  import context._

  def find(login: String): Future[List[TransactionHistory]] = {
    run(quote {
      query[TransactionHistory].filter(_.login == lift(login))
    })
  }

  def add(history: TransactionHistory): Future[Unit] = {
    run(quote {
      query[TransactionHistory].insert(lift(history)).returning(_.id)
    }).map(newId => history.copy(id = newId))
  }

  def getPagedQueryWithFind(searchedStr: String, offset: Int, querySize: Int): Future[List[TransactionHistory]] = {
    run(quote {
      query[TransactionHistory]
        .drop(lift(offset - 1))
        .rightJoin(query[Stock])
        .on(_.idStock == _.id)
        .filter(pair => pair._2.name like s"%${lift(searchedStr)}%")
        .take(lift(querySize))
//        .map(pair => pair._1.getOrElse(0))
    })
  }

}

