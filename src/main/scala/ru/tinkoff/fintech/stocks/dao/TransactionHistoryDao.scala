package ru.tinkoff.fintech.stocks.dao

import ru.tinkoff.fintech.stocks.db.models.{Stock, TransactionHistory}

import scala.concurrent.{ExecutionContext, Future}

class TransactionHistoryDao(implicit val ec: ExecutionContext) {

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

  def getLastId: Future[Option[Long]] = {
    run(quote {
      query[TransactionHistory].map(s => s.id).max
    }).map(_.head)
  }

  def getPagedQueryWithFind(login: String, searchedStr: String, offset: Int, querySize: Int): Future[List[TransactionHistory]] = {
    run(quote{
      for {
        tHistories <- query[TransactionHistory]
          .filter(t => t.login like s"%${lift(login)}%")
          .sortBy(t => t.id)(Ord.desc)
          .drop(lift(offset))
          .take(lift(querySize))
        stocks <- query[Stock]
          .join(_.id == tHistories.stockId)
          .filter(t => t.name like s"%${lift(searchedStr)}%")
      } yield tHistories
    })
  }

}

