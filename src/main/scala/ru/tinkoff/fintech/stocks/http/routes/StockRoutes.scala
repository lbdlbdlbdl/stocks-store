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
import io.circe._, io.circe.parser._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class StockRoutes(implicit val exctx: ExecutionContext,
                  implicit val qctx: PostgresAsyncContext[Escape],
                  implicit val system: ActorSystem) extends    FailFastCirceSupport with JwtHelper {

  val stockPackageDao = new StocksPackageDao()
  val stockDao = new StockDao()
  val stocksService = new StocksService(stockPackageDao, stockDao)

  import akka.event.Logging

  val logger = Logging(system, getClass)

  val stocksRoutes = {
    import io.circe.generic.auto._

    pathPrefix("api" / "stocks") {
      get {
        parameter(
          "search".as[Option[String]],
          "count".as[Option[Int]],
          "itemId".as[Option[Int]]
        ) { (search, count, itemId) => {
          search match {
            case Some(value) => ??? //stockDao.findStrInName(value) // go to stocksDao.find(value)
            case None => ???
          }
          //          val res = stocksService.accountInfo(login)
          //          onComplete(res) {
          //            case Success(accountInfo) => complete(StatusCodes.OK, accountInfo)
          //          }
        }
        }
      }
    }
  }
}
