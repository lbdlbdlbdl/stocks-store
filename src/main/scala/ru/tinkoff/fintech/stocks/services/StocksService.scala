package ru.tinkoff.fintech.stocks.services

import java.time.LocalDate

import akka.actor.ActorSystem
import ru.tinkoff.fintech.stocks.dao.{PriceHistoryDao, StockDao, StocksPackageDao}
import ru.tinkoff.fintech.stocks.db.Stock
import ru.tinkoff.fintech.stocks.http.Exceptions.{NotFoundException, ValidationException}
import ru.tinkoff.fintech.stocks.http.JwtHelper
import ru.tinkoff.fintech.stocks.http.dtos.Responses
import ru.tinkoff.fintech.stocks.http.dtos.Responses.PricePackage

import scala.concurrent.{ExecutionContext, Future}

class StocksService(val stocksPackageDao: StocksPackageDao,
                    val stockDao: StockDao,
                    val priceHistoryDao: PriceHistoryDao)
                   (implicit val exctx: ExecutionContext,
                    implicit val system: ActorSystem) extends JwtHelper {

  import akka.event.Logging

  val log = Logging.getLogger(system, this)

  private def newStockResponse(stock: Stock): Responses.Stock = //TORO
    Responses.Stock(stock.id, stock.code, stock.name, "icon.jpg", stock.salePrice, 0.0)

  def stockResponseList(stocksList: List[Stock], accumStockRes: List[Responses.Stock] = Nil): List[Responses.Stock] = { //TORO
    stocksList match {
      case stock :: Nil => accumStockRes :+ newStockResponse(stock)
      case stock :: tail => stockResponseList(tail, accumStockRes :+ newStockResponse(stock))
      case _ => Nil
    }
  }

  def stocksPage(searchStr: String, count: Int, itemId: Int): Future[Responses.StocksPage] = {
    log.info(s"begin get stocks page, params: searchstr = $searchStr, count = $count, itemId = $itemId")
    for {
      stocks <- stockDao.getPagedQueryWithFind(searchStr, itemId, count + 1)
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
//
//  def compress(list: List[PricePackage]): Unit ={
//    val step=list.length/100
//
//  }

  def stocksHistory(range: String, id: Long): Future[Responses.PriceHistory] = {
    log.info(s"begin get price history per share id=$id during the period=$range")
    for {
      stockOption <- stockDao.getStockOption(id)
      stock = stockOption.getOrElse(throw NotFoundException(s"Stock not found id=$id."))
      dateFrom=fromDate(range)
      listHistory <- priceHistoryDao.find(id)
      prices=listHistory.filter(_.date.toLocalDate.isAfter(dateFrom)).map(s=>Responses.PricePackage(s.date.toLocalDate,s.buyPrice))
    } yield Responses.PriceHistory(id,stock.code,stock.name,stock.iconUrl,dateFrom,date,prices)
  }
}


