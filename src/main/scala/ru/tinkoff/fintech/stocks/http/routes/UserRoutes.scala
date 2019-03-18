package ru.tinkoff.fintech.stocks.http.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import io.circe.syntax._
import io.getquill.{Escape, PostgresAsyncContext}
import ru.tinkoff.fintech.stocks.dao.UserDao
import ru.tinkoff.fintech.stocks.http._
import ru.tinkoff.fintech.stocks.services._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class UserRoutes(implicit val exctx: ExecutionContext,
                 implicit val qctx: PostgresAsyncContext[Escape]) extends JwtHelper with FailFastCirceSupport {

  val userDao = new UserDao()
  val userService = new UserService(userDao)

  val authRoutes = {

    pathPrefix("api" / "auth") {
      path("signup") {
        post {
          entity(as[Requests.UserRequest]) { user =>
            val res = userService.createUser(user.login, user.password)
            onComplete(res) {
              case Success(value) => ???
            }
          }
        }
      }
    } ~
      path("signin") {
        post {
          entity(as[Requests.UserRequest]) { user =>
            val res = userService.authenticate(user.login, user.password)
            onComplete(res) {
              case Success(value: Either[ErrorMessage, Responses.Token]) =>
                value match {
                  case Right(token) => complete(StatusCodes.OK, token.asJson)
                  case Left(error) => {
                    val code = StatusCodes.Unauthorized
                    complete(code, Error(code.intValue, error.message).asJson)
                  }
                }
              case Failure(cause) =>
                complete(StatusCodes.InternalServerError, s"Failed to authenticate bc $cause")
            }
          }
        }
      } ~
      path("refresh") {
        post {
          entity(as[Requests.RefreshToken]) { refreshToken =>
            ???
          }
        }
      }
  }
}
