package ru.tinkoff.fintech.stocks.services

import ru.tinkoff.fintech.stocks.exception.Exceptions._
import ru.tinkoff.fintech.stocks.http.dtos.Responses._

import scala.concurrent.{ExecutionContext, Future}
import java.time.LocalDateTime

import akka.event.LoggingAdapter
import cats.data.OptionT
import cats.instances.future._

import ru.tinkoff.fintech.stocks.dao._
import ru.tinkoff.fintech.stocks.db.models._

case class Companion(user: User, bag: Option[StocksPackage], stock: Stock)

class TransactionService(userDao: UserDao,
                         stockDao: StockDao,
                         stocksPackageDao: StocksPackageDao,
                         transactionDao: TransactionDao,
                         transactionHistoryDao: TransactionHistoryDao)
                        (implicit val ec: ExecutionContext,
                         logger: LoggingAdapter) {

  //достаем инфу о пользователе о его пакете на акцию и информацию о самой акции
  def companion(login: String, stockId: Long, amount: Int): Future[Companion] = {
    if (amount < 1) throw ValidationException("Amount must be more than 0.")
    for {
      user <- OptionT(userDao.find(login)).getOrElse(throw NotFoundException("User not found."))
      stock <- OptionT(stockDao.getStockOption(stockId)).getOrElse(throw NotFoundException(s"Stock not found id=$stockId."))
      bag <- stocksPackageDao.findByStock(user.id.get, stockId)
    } yield Companion(user, bag, stock)
  }

  private def timeNow = LocalDateTime.now()

  def buyStocks(implicit login: String, stockId: Long, amount: Int): Future[TransactionSuccess] =
    transaction("buy")

  def sellStocks(implicit login: String, stockId: Long, amount: Int): Future[TransactionSuccess] =
    transaction("sell")

  private def transaction(act: String)(implicit login: String, stockId: Long, amount: Int): Future[TransactionSuccess] = {
    for {
      cmp <- companion(login: String, stockId: Long, amount: Int)
      price = cmp.stock.buyPrice * amount
      _ <- act match {
        case "buy" => transactionDao.transactionBuy(cmp.stock.id, cmp.user.id.get, price, amount)
        case "sell" => transactionDao.transactionSell(cmp.stock.id, cmp.user.id.get, price, amount)
      }
      _ <- transactionHistoryDao.add(TransactionHistory(None, login, stockId, amount, price, timeNow, act))
    } yield TransactionSuccess()
  }

  def transactionHistory2Response(value: TransactionHistory): Future[TransactionHistoryResponse] =
    for {
      stock <- stockDao.getStock(value.stockId)
    } yield TransactionHistoryResponse(
      StockHistory(stock.id, stock.code, stock.name, stock.iconUrl),
      value.amount, value.totalPrice, value.date, value.`type`)

  def transactionHistoryPage(login: String, searchStr: String, count: Int, itemId: Int): Future[TransactionHistoryPage] = {
    logger.info(s"begin get trans. history page, params: searchstr = $searchStr, count = $count, itemId = $itemId")
    for {
      historiesPage <- transactionHistoryDao.getPagedQueryWithFind(login, searchStr, itemId - 1, count + 1)
      historiesSize <- transactionHistoryDao.getLastId

      lastId = historiesPage match {
        case Nil => 1
        case _ =>
          val historyPageLastId = historiesPage.last.id.get
          if (historyPageLastId == historiesSize.get) 0 else historyPageLastId
      }
      responses <- historiesPage match {
        case Nil => Future.successful(Nil)
        case _ => Future.sequence(historiesPage.take(count).map(th => transactionHistory2Response(th)))
      }
    } yield TransactionHistoryPage(lastId, itemId, responses)
  }

}
