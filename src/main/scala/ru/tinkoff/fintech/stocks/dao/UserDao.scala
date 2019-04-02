package ru.tinkoff.fintech.stocks.dao

import io.getquill.{Escape, PostgresAsyncContext}
import ru.tinkoff.fintech.stocks.db.User

import scala.concurrent.{ExecutionContext, Future}

class UserDao(implicit val context: PostgresAsyncContext[Escape],
              implicit val exctx: ExecutionContext) {

  import context._

  // поищем что-нибудь в БД
  def find(login: String): Future[Option[User]] = {
    run(quote {
      query[User].filter(_.login == lift(login)).take(1)
    }).map(_.headOption)
  }

  // списочек логинов
  def listOfLogins(): Future[List[String]] = {
    run(quote {
      query[User].map(_.login)
    })
  }

  // или добавим что-то новое
  def add(user: User): Future[User] = {
    run(quote {
      query[User].insert(lift(user)).returning(_.id)
    }).map(newId => user.copy(id = newId))
  }

//  def update(id: Long, minusBalance: Double): Future[User] = {
//    run(quote {
//      query[User].filter(_.id == lift(id)).update(u => u.balance -> (u.balance - lift(minusBalance)))
//    })
//  }

}
