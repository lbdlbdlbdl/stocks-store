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

class AccountRoutes(implicit val exctx: ExecutionContext,
                    implicit val qctx: PostgresAsyncContext[Escape]) extends FailFastCirceSupport with JwtHelper {

  val userDao = new UserDao()
  val storageDao = new StorageDao()
  val stockDao = new StockDao()
  val userService = new UserService(userDao, storageDao, stockDao)

  val accountRoutes = {
    import io.circe.generic.auto._

    pathPrefix("api" / "account") {
      path("info") {
        authenticated { claim =>
          get {
            val login = claim.content
            val res = userService.accountInfo(login)
            onComplete(res) {
              case Success(accountInfo) => complete(StatusCodes.OK, accountInfo)
            }
          }
        }
      }
    }
  }
}