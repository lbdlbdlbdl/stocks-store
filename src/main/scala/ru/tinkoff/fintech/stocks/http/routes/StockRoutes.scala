package ru.tinkoff.fintech.stocks.http.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.getquill.{Escape, PostgresAsyncContext}
import ru.tinkoff.fintech.stocks.dao._
import ru.tinkoff.fintech.stocks.http._
import ru.tinkoff.fintech.stocks.services._
import pdi.jwt.JwtClaim
import cats.syntax.either._
import io.circe._
import io.circe.parser._
import ru.tinkoff.fintech.stocks.http.dtos.Requests
import ru.tinkoff.fintech.stocks.http.dtos.Requests.RangeHistory

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class StockRoutes(implicit val exctx: ExecutionContext,
                  implicit val qctx: PostgresAsyncContext[Escape],
                  implicit val system: ActorSystem) extends FailFastCirceSupport {

  val stockPackageDao = new StocksPackageDao()
  val stockDao = new StockDao()
  val priceHistoryDao = new  PriceHistoryDao()
  val stocksService = new StocksService(stockPackageDao, stockDao, priceHistoryDao)

  import akka.event.Logging

  val logger = Logging(system, getClass)

  val stocksRoutes = {
    import io.circe.generic.auto._

    pathPrefix("api" / "stocks") {
      path(IntNumber /"history"){(id)=>
        get {
          parameters(
            "range".?
          ).as(RangeHistory)  { params =>
            val res = stocksService.stocksHistory(params.range.getOrElse("week"), id)
            onComplete(res) {
              case Success(historyPrice) => complete(StatusCodes.OK, historyPrice)
            }
          }
        }
      }
      get {
        parameters(
          "search".?,
          "count".as[Int] ?,
          "itemId".as[Int] ?
        ).as(Requests.PageParameters) { params =>
          val res = stocksService.stocksPage(
            params.search.getOrElse(""),
            params.count.getOrElse(10),
            params.itemId.getOrElse(1))
          onComplete(res) {
            case Success(stocksPage) => complete(StatusCodes.OK, stocksPage)
          }
        }
      }
    }
  }
}


