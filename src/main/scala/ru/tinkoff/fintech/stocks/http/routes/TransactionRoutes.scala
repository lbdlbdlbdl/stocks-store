package ru.tinkoff.fintech.stocks.http.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import cats.data.Reader
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import ru.tinkoff.fintech.stocks.Env
import ru.tinkoff.fintech.stocks.http._
import ru.tinkoff.fintech.stocks.http.dtos.Requests
import JwtHelper._
import akka.event.LoggingAdapter

import scala.concurrent.ExecutionContext

class TransactionRoutes(implicit val ec: ExecutionContext,
                        logger: LoggingAdapter) extends FailFastCirceSupport {

  val route = Reader[Env, server.Route] { env =>
    import io.circe.generic.auto._

    pathPrefix("api" / "transaction") {
      path("buy") {
        (post & authenticated) { claim =>
          entity(as[Requests.Transaction]) { buy =>
            val login = getLoginFromClaim(claim)
            complete {
              for {
                purchase <- env.transactionService.buyStocks(login, buy.stockId, buy.amount)
              } yield StatusCodes.OK -> purchase
            }
          }
        }
      } ~
        path("sell") {
          (post & authenticated) { claim =>
            entity(as[Requests.Transaction]) { sell =>
              val login = getLoginFromClaim(claim)
              complete {
                for {
                  sale <- env.transactionService.sellStocks(login, sell.stockId, sell.amount)
                } yield StatusCodes.OK -> sale
              }
            }
          }
        } ~
        path("history") {
          (get & authenticated & paginationParams) { (claim, params) =>
            logger.info(s"begin get transaction history page")
            val login = getLoginFromClaim(claim)
            complete {
              for {
                transactionHistoryPage <- env.transactionService.transactionHistoryPage(
                  login,
                  params.search.getOrElse(""),
                  params.count.getOrElse(10),
                  params.itemId.getOrElse(1))
              } yield StatusCodes.OK -> transactionHistoryPage
            }
          }
        }
    }
  }
}