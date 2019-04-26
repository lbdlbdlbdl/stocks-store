package ru.tinkoff.fintech.stocks.services

import java.time.LocalDateTime

import ru.tinkoff.fintech.stocks.exception.Exceptions._
import ru.tinkoff.fintech.stocks.http.dtos.Responses._

import scala.concurrent.Future
import java.time.LocalDateTime

import ru.tinkoff.fintech.stocks.http.JwtHelper

import scala.concurrent.ExecutionContext.Implicits.global
import ru.tinkoff.fintech.stocks.dao.{StockDao, StocksPackageDao, TransactionHistoryDao, UserDao}
import ru.tinkoff.fintech.stocks.db.models._

import scala.util.Success

case class Companion(user: User, bag: Option[StocksPackage], stock: Stock)

class TransactionService(implicit val userDao: UserDao,
                         val stockDao: StockDao,
                         val stocksPackageDao: StocksPackageDao,
                         val transactionHistoryDao: TransactionHistoryDao) {

  //достаем инфу о пользователе о его пакете на акцию и информацию о самой акции
  def companion(login: String, stockId: Long, amount: Int): Future[Companion] = {
  if (amount < 1) throw ValidationException("Amount must be more than 0.")
    for {
      maybeUser <- userDao.find(login)
      userInfo = maybeUser.getOrElse(throw NotFoundException("User not found."))
      maybeStock <- stockDao.getStockOption(stockId)
      stock = maybeStock.getOrElse(throw NotFoundException(s"Stock not found id=$stockId."))
      bag <- stocksPackageDao.findByStock(userInfo.id.get, stockId)
    } yield Companion(userInfo, bag, stock)

    }

  private def timeNow = LocalDateTime.now()

  def transaction(act: String, login: String, stockId: Long, amount: Int): Future[TransactionSuccess] = {
    for {
      cmp <- companion(login: String, stockId: Long, amount: Int).run(env)
      price = cmp.stock.buyPrice * amount
      _ <- act match {
        case "buy" => env.transactionDao.transactionBuy(cmp.stock.id, cmp.user.id.get, price, amount)
        case "sell" => env.transactionDao.transactionSell(cmp.stock.id, cmp.user.id.get, price, amount)
      }
      _ <- env.transactionHistoryDao.add(
        TransactionHistory(None, login, stockId, amount, price, timeNow, act))
    } yield Responses.TransactionSuccess()
  }

  def transactionHistory2Response(value: TransactionHistory): Result[Responses.TransactionHistory] = ReaderT { env =>
    for {
      stock <- env.stockDao.getStock(value.stockId)
    } yield Responses.TransactionHistory(stock.as[Responses.StockHistory], value.amount, value.totalPrice, value.date, value.`type`)
  }

  def transactionHistoryPage(login: String, searchStr: String, count: Int, itemId: Int): Result[Responses.TransactionHistoryPage] = ReaderT { env =>
    env.logger.info(s"begin get trans. history page, params: searchstr = $searchStr, count = $count, itemId = $itemId")
    for {
      historiesPage <- env.transactionHistoryDao.getPagedQueryWithFind(login, searchStr, itemId - 1, count + 1)
      historiesSize <- env.transactionHistoryDao.getLastId

      historyPageLastId = historiesPage.last.id.get
      lastId = if (historyPageLastId == historiesSize) 0 else historyPageLastId // ? x: y doesnt work

      responses <-
        Future.sequence(historiesPage.take(count).map(th => transactionHistory2Response(th).run(env)))

    } yield Responses.TransactionHistoryPage(lastId, itemId, responses)
  }

}
