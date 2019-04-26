package ru.tinkoff.fintech.stocks.http.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import cats.data.Reader
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import ru.tinkoff.fintech.stocks.Env
import ru.tinkoff.fintech.stocks.http._
import ru.tinkoff.fintech.stocks.http.dtos.Requests

import scala.concurrent.ExecutionContext.Implicits.global

class TransactionRoutes extends FailFastCirceSupport with JwtHelper {

  val route = Reader[Env, server.Route] { env =>
    import io.circe.generic.auto._


    pathPrefix("api" / "transaction") {
      path("buy") {
        authenticated { claim =>
          post {
            entity(as[Requests.Transaction]) { buy =>
              val login = getLoginFromClaim(claim)
              // logger.info(s"begin transaction buying: Stock ${buy.stockId}, amount ${buy.amount} ")
              complete {
                for {
                  purchase <- env.transactionService.buyStock(login, buy.stockId, buy.amount).run(env)
                } yield StatusCodes.OK -> purchase
              }
            }
          }
        }
      } ~
        path("sell") {
          authenticated { claim =>
            post {
              entity(as[Requests.Transaction]) { sell =>
                val login = getLoginFromClaim(claim)
                //logger.info(s"begin transaction selling: Stock ${sell.stockId}, amount ${sell.amount} ")
                complete {
                  for {
                    sale <- env.transactionService.saleStock(login, sell.stockId, sell.amount).run(env)
                  } yield StatusCodes.OK -> sale
                }
              }
            }
          }
        } ~
        path("history") {
          authenticated { claim =>
            get {
              parameters(
                "search".?,
                "count".as[Int] ?,
                "itemId".as[Int] ?
              ).as(Requests.PageParameters) { params =>
                //logger.info(s"begin get transaction history page")
                val login = getLoginFromClaim(claim)
                complete {
                  for {
                    transactionHistoryPage <- env.transactionService.transactionHistoryPage(
                      login,
                      params.search.getOrElse(""),
                      params.count.getOrElse(10),
                      params.itemId.getOrElse(1)).run(env)
                  } yield StatusCodes.OK -> transactionHistoryPage
                }
              }
            }
          }
        }
    }
  }
}
