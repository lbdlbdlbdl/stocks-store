package ru.tinkoff.fintech.stocks.http.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.getquill.{Escape, PostgresAsyncContext}

import ru.tinkoff.fintech.stocks.dao._
import ru.tinkoff.fintech.stocks.http._
import ru.tinkoff.fintech.stocks.services._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class UserRoutes(implicit val exctx: ExecutionContext,
                 implicit val qctx: PostgresAsyncContext[Escape]) extends FailFastCirceSupport with JwtHelper {

  val userDao = new UserDao()
  val storageDao = new StorageDao()
  val stockDao = new StockDao()
  val userService = new UserService(userDao, storageDao, stockDao)

  val authRoutes = {
    import io.circe.generic.auto._

    pathPrefix("api" / "auth") {
      path("signup") {
        post {
          entity(as[Requests.UserRequest]) { user =>
            val res = userService.createUser(user.login, user.password)
            onComplete(res) {
              case Success(tokens) => complete(StatusCodes.OK, tokens)
            }
          }
        }
      } ~
        path("signin") {
          post {
            entity(as[Requests.UserRequest]) { user =>
              val res = userService.authenticate(user.login, user.password)
              onComplete(res) {
                case Success(tokens) => complete(StatusCodes.OK, tokens)
              }
            }
          }
        } ~
        path("refresh") {
          authenticated { claim =>
            post {
              entity(as[Requests.RefreshToken]) { refreshToken =>
                val res = userService.refreshTokens(refreshToken.refreshToken)
                onComplete(res) {
                  case Success(tokens) => complete(StatusCodes.OK, tokens)
                }
              }
            }
          }
        }
    }
  }
}
