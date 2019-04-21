package ru.tinkoff.fintech.stocks.services

import java.time.LocalDate

import akka.actor.ActorSystem
import cats.data.{Reader, ReaderT}
import ru.tinkoff.fintech.stocks.Env
import ru.tinkoff.fintech.stocks.db.{Stock, StocksPackage}
import ru.tinkoff.fintech.stocks.exception.Exceptions._
import ru.tinkoff.fintech.stocks.http.JwtHelper
import ru.tinkoff.fintech.stocks.http.dtos.Responses
import ru.tinkoff.fintech.stocks.result.Result

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StocksService extends JwtHelper {

  /*
  def priceDelta(stock:Stock):Reader[Env, Double] = Reader { env =>
    val secondLastPrice = env.priceHistoryDao.find(stock.id).map { case _ :: p :: _ :: Nil => p }
  }
  */

  private def newStockBatch(stock: Future[Stock], count: Int): Result[Responses.StockBatch] = ReaderT { env =>
    for { //TODO: transaction
      s <- stock
      prices <- env.priceHistoryDao.find(s.id)
      secondLastPrice = prices match { case _ :: p :: _ :: Nil => p }
      deltaPrice = s.buyPrice - secondLastPrice.buyPrice
    } yield Responses.StockBatch(s.id, s.code, s.name, s.iconUrl, s.salePrice, deltaPrice, count)
  }

  def stockPackages2StockBatches(stocksPackages: List[StocksPackage]): Result[List[Responses.StockBatch]] = ReaderT { env =>
    Future.sequence(stocksPackages.map(sp => newStockBatch(env.stockDao.getStock(sp.stockId), sp.count).run(env)))
  }

  def stocksPage(searchStr: String, count: Int, itemId: Int): Result[Responses.StocksPage] = ReaderT { env =>
    env.logger.info(s"begin get stocks page, params: searchstr = $searchStr, count = $count, itemId = $itemId")
    for {
      stocks <- env.stockDao.getPagedQueryWithFind(searchStr, itemId, count + 1)
      lastId = stocks.last.id
    } yield Responses.StocksPage(lastId, itemId, stocks.take(count).map(s => s.as[Responses.Stock]).reverse)
  }

  private def date = LocalDate.now()

  def fromDate(range: String) = range match {
    case "day" => date.minusDays(1)
    case "week" => date.minusDays(7)
    case "month" => date.minusMonths(1)
    case "6months" => date.minusMonths(6)
    case "year" => date.minusYears(1)
    case "total" => LocalDate.ofEpochDay(1)
    case _ => throw ValidationException(s"incorrect range=$range")
  }

  //
  //  def compress(list: List[PricePackage]): Unit ={
  //    val step=list.length/100
  //
  //  }


  private def parse(date: String) =
    date.take(4).toInt * 10000 + date.slice(5, 7).toInt * 100 + date.slice(8, 10).toInt

  def stockPriceHistory(range: String, id: Long): Result[Responses.PriceHistory] = ReaderT { env =>
    env.logger.info(s"begin get price history per share id=$id during the period=$range")
    for {
      stockOption <- env.stockDao.getStockOption(id)
      stock = stockOption.getOrElse(throw NotFoundException(s"Stock not found id=$id."))

      dateFrom = fromDate(range)
      listHistory <- env.priceHistoryDao.find(id)
      prices = listHistory
        .filter(_.date.toLocalDate.isAfter(dateFrom))
        .map(pHis => Responses.PricePackage(pHis.date.toLocalDate, pHis.buyPrice))
    } yield Responses.PriceHistory(id, stock.code, stock.name, stock.iconUrl, dateFrom, date, prices)
  }
}


