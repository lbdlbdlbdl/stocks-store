package ru.tinkoff.fintech.stocks.dao

import ru.tinkoff.fintech.stocks.db.models.PriceHistory

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class PriceHistoryDao {

  import quillContext._

  def find(idStock: Long): Future[List[PriceHistory]] = {
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

