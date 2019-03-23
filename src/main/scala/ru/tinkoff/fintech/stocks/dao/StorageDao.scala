package ru.tinkoff.fintech.stocks.dao

import io.getquill.{Escape, PostgresAsyncContext}
import ru.tinkoff.fintech.stocks.db.models.Storage

import scala.concurrent.{ExecutionContext, Future}

/*
  Примечание:
    Пакет(контекст)- набор из акции (Stock) и её количества
    У User может быть пакетов не больше чем весь ассортимент акций
    1 пакет - 1 акция для User
*/

class StorageDao(implicit val context: PostgresAsyncContext[Escape],
              implicit val exctx: ExecutionContext) {

  import context._

  //найдем пакет акции
  def findById(id: Long): Future[List[Storage]] = {
    run(quote {
      query[Storage].filter(_.id == lift(id))
    })
  }

}
