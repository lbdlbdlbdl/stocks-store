package ru.tinkoff.fintech.stocks.dao

import io.getquill.{Escape, PostgresAsyncContext}
import ru.tinkoff.fintech.stocks.db.models.User

import scala.concurrent.{ExecutionContext, Future}

class UserDao(implicit val context: PostgresAsyncContext[Escape],
              implicit val exctx: ExecutionContext) {

  import context._

  // поищем что-нибудь в БД
  def findUserByLogin(login: String): Future[Option[User]] = {
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
  def addUser(user: User): Future[User] = {
    run(quote {
      query[User].insert(lift(user)).returning(_.id)
    }).map(newId => user.copy(id = newId))
  }
}
