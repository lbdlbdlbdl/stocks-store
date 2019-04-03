package ru.tinkoff.fintech.stocks.services

import akka.actor.ActorSystem
import ru.tinkoff.fintech.stocks.dao.{StockDao, StocksPackageDao}
import ru.tinkoff.fintech.stocks.http.JwtHelper

import scala.concurrent.ExecutionContext

class StocksService(val stocksPackageDao: StocksPackageDao,
                    val stockDao: StockDao)
                   (implicit val exctx: ExecutionContext,
                   implicit val system: ActorSystem) extends JwtHelper {

  import akka.event.Logging
  val log = Logging.getLogger(system, this)

}


