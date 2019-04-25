package ru.tinkoff.fintech.stocks.services

import java.time.LocalDate

import ru.tinkoff.fintech.stocks.exception.Exceptions._
import ru.tinkoff.fintech.stocks.http.dtos.Responses._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import ru.tinkoff.fintech.stocks.dao.{PriceHistoryDao, StockDao}
import ru.tinkoff.fintech.stocks.db.models._

class StocksService(implicit val priceHistoryDao: PriceHistoryDao,
                    val stockDao: StockDao) {

  /*
  def priceDelta(stock:Stock):Reader[Env, Double] = Reader { env =>
    val secondLastPrice = env.priceHistoryDao.find(stock.id).map { case _ :: p :: _ :: Nil => p }
  }
  */

  private def newStockBatch(stock: Future[Stock], count: Int): Future[StockBatch] =
    for { //TODO: transaction
      s <- stock
      prices <- priceHistoryDao.find(s.id)
      secondLastPrice = prices match {
        case _ :: p :: _ => p
      }
      deltaPrice = s.buyPrice - secondLastPrice.buyPrice
    } yield StockBatch(s.id, s.code, s.name, s.iconUrl, s.salePrice, deltaPrice, count)


  def stockPackages2StockBatches(stocksPackages: List[StocksPackage]): Future[List[StockBatch]] =
    Future.sequence(stocksPackages.map(sp => newStockBatch(stockDao.getStock(sp.stockId), sp.count)))


  def stocksPage(searchStr: String, count: Int, itemId: Int): Future[StocksPage] = {
    //    env.logger.info(s"begin get stocks page, params: searchstr = $searchStr, count = $count, itemId = $itemId")
    for {
      stocks <- stockDao.getPagedQueryWithFind(searchStr, itemId, count + 1)
      lastId = stocks.reverse.last.id
    } yield StocksPage(lastId, itemId,
      stocks.take(count).map(s => StockResponse(s.id, s.name, s.code, s.iconUrl, s.buyPrice, 0.0)).reverse)
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

  private def parse(date: String) =
    date.take(4).toInt * 10000 + date.slice(5, 7).toInt * 100 + date.slice(8, 10).toInt

  def compress(list: List[PricePackage]): List[PricePackage] = {
    val step = list.length / 20 + 1

    def averaged(t: List[PricePackage]): PricePackage = {
      val p = println(step)
      val price = t.map(_.price).sum / step
      val date = LocalDate.ofEpochDay(t.map(_.date.toEpochDay).sum / step)
      PricePackage(date, price)
    }

    def recTail(t: List[PricePackage], acc: List[PricePackage]): List[PricePackage] = t.length match {
      case v if v >= step => recTail(t.drop(step), acc :+ averaged(t.take(step)))
      case _ => acc ++ t
    }

    recTail(list, List.empty[PricePackage])

  }

  def stockPriceHistory(range: String, id: Long): Future[PriceHistoryResponse] = {
    //    env.logger.info(s"begin get price history per share id=$id during the period=$range")
    for {
      stockOption <- stockDao.getStockOption(id)
      stock = stockOption.getOrElse(throw NotFoundException(s"Stock not found id=$id."))

      dateFrom = fromDate(range)
      listHistory <- priceHistoryDao.find(id)
      prices = compress(listHistory
        .filter(_.date.toLocalDate.isAfter(dateFrom))
        .map(pHis => PricePackage(pHis.date.toLocalDate, pHis.buyPrice)))
    } yield PriceHistoryResponse(id, stock.code, stock.name, stock.iconUrl, dateFrom, date, prices)
  }
}


