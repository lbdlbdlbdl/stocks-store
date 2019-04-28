package ru.tinkoff.fintech.stocks.http.routes

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import cats.data.Reader
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe._
import io.circe.parser._
import ru.tinkoff.fintech.stocks.http._
import ru.tinkoff.fintech.stocks.Env
import ru.tinkoff.fintech.stocks.http.dtos.Requests.RangeHistory
import ru.tinkoff.fintech.stocks.services._

import scala.concurrent.ExecutionContext

class StockRoutes(implicit val ec: ExecutionContext,
                  logger: LoggingAdapter) extends FailFastCirceSupport {

  val route = Reader[Env, server.Route] { env =>
    import io.circe.generic.auto._

    pathPrefix("api" / "stocks") {
      get {
        (path(IntNumber / "history") & parameters("range".?).as(RangeHistory)) { (stockId, params) =>
          complete {
            for {
              priceHistory <- env.stocksService.stockPriceHistory(params.range.getOrElse("week"), stockId)
            } yield StatusCodes.OK -> priceHistory
          }
        }
      } ~
        paginationParams { params =>
          complete {
            for {
              stocksPage <- env.stocksService
                .stocksPage(
                  params.search.getOrElse(""),
                  params.count.getOrElse(10),
                  params.itemId.getOrElse(1))
            } yield StatusCodes.OK -> stocksPage
          }
        }
    }
  }
}


