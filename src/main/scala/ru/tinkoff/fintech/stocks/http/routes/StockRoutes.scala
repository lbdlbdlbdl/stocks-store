package ru.tinkoff.fintech.stocks.http.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import cats.data.Reader
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe._
import io.circe.parser._
import ru.tinkoff.fintech.stocks.Env
import ru.tinkoff.fintech.stocks.http.dtos.Requests

import scala.concurrent.ExecutionContext.Implicits.global

class StockRoutes extends FailFastCirceSupport {

  val route = Reader[Env, server.Route] { env =>
    import io.circe.generic.auto._

    pathPrefix("api" / "stocks") {
      get {
        parameters(
          "search".?,
          "count".as[Int] ?,
          "itemId".as[Int] ?
        ).as(Requests.PageParameters) { params =>
          complete {
            for {
              stocksPage <- env.stocksService
                .stocksPage(
                  params.search.getOrElse(""),
                  params.count.getOrElse(10),
                  params.itemId.getOrElse(1))
                .run(env)
            } yield StatusCodes.OK -> stocksPage
          }
        }
      }
    }
  }
}


