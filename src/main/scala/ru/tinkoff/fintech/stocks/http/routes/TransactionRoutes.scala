package ru.tinkoff.fintech.stocks.http.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.getquill.{Escape, PostgresAsyncContext}
import ru.tinkoff.fintech.stocks.dao._
import ru.tinkoff.fintech.stocks.http._
import ru.tinkoff.fintech.stocks.services._

import scala.concurrent.ExecutionContext
import scala.util.Success

class TransactionRoutes(implicit val exctx: ExecutionContext,
                        implicit val qctx: PostgresAsyncContext[Escape],
                        implicit val system: ActorSystem) extends FailFastCirceSupport with JwtHelper {

  val stockPackageDao = new StocksPackageDao()
  val stockDao = new StockDao()
  val userDao = new UserDao()
  val transactionHistoryDao= new TransactionHistoryDao()
  val transactionService = new TransactionService(stockPackageDao, stockDao,userDao,transactionHistoryDao)

  import akka.event.Logging

  val logger = Logging(system, getClass)

  val stocksRoutes = {
    import io.circe.generic.auto._

    pathPrefix("api" / "transaction") {
      path("buy") {
        authenticated { claim =>
          post {
            entity(as[Requests.Transaction]) { buy =>
              val login = getLoginFromClaim(claim)
              logger.info(s"begin transaction buy: Stock ${buy.stockId}, amount ${buy.amount} ")
              val purchase = transactionService.buyStock(login,buy.stockId, buy.amount)
              onComplete(purchase) {
                case Success(value) => complete(StatusCodes.OK, value)
              }            }
          }
        }
      }~
      path("sell") {
        authenticated { claim =>
          post {
            entity(as[Requests.Transaction]) { sell =>
              val login = getLoginFromClaim(claim)
              logger.info(s"begin transaction sell: Stock ${sell.stockId}, amount ${sell.amount} ")
              val sale = transactionService.saleStock(login,sell.stockId, sell.amount)
              onComplete(sale) {
                case Success(value) => complete(StatusCodes.OK, value)
              }
            }
          }
        }
      }~
        path("history") {
          authenticated { claim =>
            get {
                val login = getLoginFromClaim(claim)
                val history = transactionService.history(login)
                onComplete(history) {
                  case Success(accountInfo) => complete(StatusCodes.OK, accountInfo)
                }
              }
          }
        }
    }
  }
}
