package ru.tinkoff.fintech.stocks.http.routes

import pdi.jwt._
//{Jwt, JwtAlgorithm, JwtHeader, JwtClaim, JwtOptions}
import akka.http.scaladsl.model.StatusCodes
import io.getquill.{Escape, PostgresAsyncContext}

import scala.concurrent.ExecutionContext

import ru.tinkoff.fintech.stocks.dao.UserDao

class UserRoutes(implicit val exctx: ExecutionContext,
                 implicit val qctx: PostgresAsyncContext[Escape]) {

  val dao = new UserDao()

  val helloRoutes = {

    import akka.http.scaladsl.server.Directives._

    path("hello" / Segment) {
      s => complete(s"Hello, ${s.capitalize}!") //вот тут дерриктива матчится у нас
    } ~ pathEndOrSingleSlash {
      redirect("hello/world", StatusCodes.TemporaryRedirect)
    }
  }

  val userRoutes = {

    import akka.http.scaladsl.server.Directives._
    import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
    import io.circe.generic.auto._

    pathPrefix("user") {
      path(Segment) {
        login =>
          onSuccess(dao.findUserByLogin(login)) {
            case Some(user) =>
              complete(user) // wow, so secure! very safe
            case None =>
              complete(StatusCodes.NotFound, s"User $login not found")
          }
      } ~ pathEndOrSingleSlash {
        onSuccess(dao.listOfLogins()) {
          logins =>
            complete(logins)
        }
      }
    }
  }

  val authRoutes = {

    import akka.http.scaladsl.server.Directives._

    pathPrefix("api" / "auth") {
      path("signup") {
        complete("Регистрация пользователя скоро будет создана -_-...")
      } ~
        path("signin") {
          complete("Нет пользователей - нет авторизации - нет проблем..")
        } ~
        path("refresh") {
          complete("Зайдите попозже ^_^")
        }
    }
  }
}
