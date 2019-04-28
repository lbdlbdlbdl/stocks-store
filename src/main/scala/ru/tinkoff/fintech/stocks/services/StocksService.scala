package ru.tinkoff.fintech.stocks.services

import java.time.LocalDate

import akka.event.LoggingAdapter
import cats.data.OptionT
import cats.instances.future._
import ru.tinkoff.fintech.stocks.exception.Exceptions._
import ru.tinkoff.fintech.stocks.http.dtos.Responses._

import scala.concurrent.{ExecutionContext, Future}
import ru.tinkoff.fintech.stocks.dao.{PriceHistoryDao, StockDao}
import ru.tinkoff.fintech.stocks.db.models._

class StocksService(stockDao: StockDao,
                    priceHistoryDao: PriceHistoryDao)
                   (implicit val ec: ExecutionContext,
                    logger: LoggingAdapter) {

  def stockPackages2StockBatches(stocksPackages: List[StocksPackage]): Future[List[StockBatch]] =
    Future.sequence(stocksPackages.map(sp => newStockBatch(stockDao.getStock(sp.stockId), sp.count)))

  def stock2StockResponse(stock: Stock): Future[StockResponse] = {
    priceDelta(stock).map(d => StockResponse(stock.id, stock.code, stock.name, stock.iconUrl, stock.buyPrice, d))
  }

  def priceDelta(stock: Stock): Future[Double] = {
    for {
      prices <- priceHistoryDao.find(stock.id) //TODO: map match
      deltaPrice = prices match {
        case _ :: p :: _ => stock.buyPrice - p.buyPrice
        case _ => 0.0
      }
    } yield deltaPrice
  }

  private def newStockBatch(stock: Future[Stock], count: Int): Future[StockBatch] =
    for {
      s <- stock
      deltaPrice <- priceDelta(s)
    } yield StockBatch(s.id, s.code, s.name, s.iconUrl, s.salePrice, deltaPrice, count)

  def stocksPage(searchStr: String, count: Int, itemId: Int): Future[StocksPage] = {
    logger.info(s"begin get stocks page, params: searchstr = $searchStr, count = $count, itemId = $itemId")
    for {
      stocksPage <- stockDao.getPagedQueryWithFind(searchStr, itemId - 1, count + 1)
      stocksSize <- stockDao.getLastId

      stocksPageLastId = stocksPage.last.id
      lastId = if (stocksPageLastId == stocksSize) 0 else stocksPageLastId // ? x: y doesnt work

      stocksRes <- Future.sequence(stocksPage.take(count).map(s => stock2StockResponse(s)))
    } yield StocksPage(lastId, itemId, stocksRes)
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
    logger.info(s"begin get price history per share id=$id during the period=$range")
    for {
      stock <- OptionT(stockDao.getStockOption(id)).getOrElse(throw NotFoundException(s"Stock not found id=$id."))

      dateFrom = fromDate(range)
      listHistory <- priceHistoryDao.find(id)
      prices = compress(listHistory
        .filter(_.date.toLocalDate.isAfter(dateFrom))
        .map(pHis => PricePackage(pHis.date.toLocalDate, pHis.buyPrice)))
    } yield PriceHistoryResponse(id, stock.code, stock.name, stock.iconUrl, dateFrom, date, prices)
  }
}


