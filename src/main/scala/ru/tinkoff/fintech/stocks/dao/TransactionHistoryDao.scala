package ru.tinkoff.fintech.stocks.dao

import ru.tinkoff.fintech.stocks.db.models.{Stock, TransactionHistory}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class TransactionHistoryDao{

  import quillContext._

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
    run(quote{
      for {
        tHistories <- query[TransactionHistory]
          .drop(lift(offset - 1))
          .take(lift(querySize))
        stocks <- query[Stock]
          .join(_.id == tHistories.stockId)
          .filter(s => s.name like lift(searchedStr))//s"%${lift(searchedStr)}%")
      } yield tHistories
    })
  }

}

