package ru.tinkoff.fintech.stocks.http.routes

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.getquill.{Escape, PostgresAsyncContext}
import ru.tinkoff.fintech.stocks.dao._
import ru.tinkoff.fintech.stocks.http._
import ru.tinkoff.fintech.stocks.services._

import scala.concurrent.ExecutionContext
import scala.util.Success

class AccountRoutes(implicit val exctx: ExecutionContext,
                    implicit val qctx: PostgresAsyncContext[Escape],
                    implicit val system: ActorSystem) extends FailFastCirceSupport with JwtHelper {

  val userDao = new UserDao()
  val storageDao = new StocksPackageDao()
  val stockDao = new StockDao()
  val userService = new UserService(userDao, storageDao, stockDao)

  val logger = Logging(system, getClass)

  val accountRoutes = {

    pathPrefix("api" / "account") {
      path("info") {
        authenticated { claim =>
          get {
            import io.circe.generic.auto._
            val login = getLoginFromClaim(claim)
            logger.info(s"login == $login")
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
