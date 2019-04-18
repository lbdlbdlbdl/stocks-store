package ru.tinkoff.fintech.stocks.services

import akka.actor.ActorSystem
import cats.data.{Reader, ReaderT}
import ru.tinkoff.fintech.stocks.db.{Stock, StocksPackage}
import ru.tinkoff.fintech.stocks.http.JwtHelper
import ru.tinkoff.fintech.stocks.http.dtos.Responses
import ru.tinkoff.fintech.stocks.result.Result

import scala.concurrent.ExecutionContext.Implicits.global


import scala.concurrent.{ExecutionContext, Future}

class StocksService extends JwtHelper {

  private def newStockResponse(stock: Stock): Responses.Stock = //TORO
    Responses.Stock(stock.id, stock.code, stock.name, "icon.jpg", stock.salePrice, 0.0)

  def stockResponseList(stocksList: List[Stock], accumStockRes: List[Responses.Stock] = Nil): List[Responses.Stock] = { //TORO
    stocksList match {
      case stock :: Nil => accumStockRes :+ newStockResponse(stock)
      case stock :: tail => stockResponseList(tail, accumStockRes :+ newStockResponse(stock))
      case _ => Nil
    }
  }

  def stocksPage(searchStr: String, count: Int, itemId: Int): Result[Responses.StocksPage] = ReaderT { env =>
//    log.info(s"begin get stocks page, params: searchstr = $searchStr, count = $count, itemId = $itemId")
    for {
      stocks <- env.stockDao.getPagedQueryWithFind(searchStr, itemId, count + 1)
      lastId = stocks.last.id
    } yield Responses.StocksPage(lastId, itemId, stockResponseList(stocks.take(count)))
  }

}


