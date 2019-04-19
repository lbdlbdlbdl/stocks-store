package ru.tinkoff.fintech.stocks.services

import java.time.LocalDate

import akka.actor.ActorSystem
import cats.data.{Reader, ReaderT}
import ru.tinkoff.fintech.stocks.db.{Stock, StocksPackage}
import ru.tinkoff.fintech.stocks.exception.Exceptions._
import ru.tinkoff.fintech.stocks.http.JwtHelper
import ru.tinkoff.fintech.stocks.http.dtos.Responses
import ru.tinkoff.fintech.stocks.result.Result

import scala.concurrent.ExecutionContext.Implicits.global

class StocksService extends JwtHelper {

  def stockResponseList(stocksList: List[Stock], accumStockRes: List[Responses.Stock] = Nil): List[Responses.Stock] = { //TORO
    stocksList match {
      case stock :: Nil => accumStockRes :+ stock.as[Responses.Stock]
      case stock :: tail => stockResponseList(tail, accumStockRes :+ stock.as[Responses.Stock])
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

  def date = LocalDate.now()

  def fromDate(range: String) = range match {
    case "day" => date.minusDays(1)
    case "week" => date.minusDays(7)
    case "month" => date.minusMonths(1)
    case "6months" => date.minusMonths(6)
    case "year" => date.minusYears(1)
    case "total" => LocalDate.ofEpochDay(1)
    case _ => throw ValidationException(s"incorrect range=$range")
  }

  def parse(date: String) =
    date.take(4).toInt * 10000 + date.slice(5, 7).toInt * 100 + date.slice(8, 10).toInt


  def stockPriceHistory(range: String, id: Long): Result[Responses.PriceHistory] = ReaderT { env =>
    //log.info(s"begin get price history per share id=$id during the period=$range")
    for {
      stockOption <- env.stockDao.getStockOption(id)
      stock = stockOption.getOrElse(throw NotFoundException(s"Stock not found id=$id."))
      dateFrom = fromDate(range).toString
      listHistory <- env.priceHistoryDao.find(id)
      prices = listHistory.filter(h => parse(h.date) < parse(dateFrom)).map(s => Responses.PricePackage(s.date, s.buyPrice))
    } yield Responses.PriceHistory(id, stock.code, stock.name, stock.iconUrl, dateFrom, date.toString, prices)
  }
}


