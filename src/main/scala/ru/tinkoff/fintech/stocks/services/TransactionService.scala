package ru.tinkoff.fintech.stocks.services

import java.time.LocalDateTime

import cats.data.ReaderT
import ru.tinkoff.fintech.stocks.db._
import ru.tinkoff.fintech.stocks.exception.Exceptions._
import ru.tinkoff.fintech.stocks.http.JwtHelper
import ru.tinkoff.fintech.stocks.http.dtos.Responses
import ru.tinkoff.fintech.stocks.result.Result

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Companion(user: User, bag: Option[StocksPackage], stock: Stock)

class TransactionService extends JwtHelper {

  //достаем инфу о пользователе о его пакете на акцию и информацию о самой акции
  def companion(login: String, stockId: Long, amount: Int): Result[Companion] = ReaderT { env =>
    if (amount < 1) throw ValidationException("Amount must be more than 0.")
    for {
      maybeUser <- env.userDao.find(login)
      userInfo = maybeUser.getOrElse(throw NotFoundException("User not found."))
      maybeStock <- env.stockDao.getStockOption(stockId)
      stock = maybeStock.getOrElse(throw NotFoundException(s"Stock not found id=$stockId."))
      bag <- env.stocksPackageDao.findByStock(userInfo.id.get, stockId)
    } yield Companion(userInfo, bag, stock)
  }

  private def timeNow = LocalDateTime.now()

  def transaction(act: String, login: String, stockId: Long, amount: Int): Result[Responses.TransactionSuccess] = ReaderT { env =>
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

  //  def stocksPage(searchStr: String, count: Int, itemId: Int): Result[Responses.TransactionHistoryPage] = ReaderT { env =>
  //    env.logger.info(s"begin get stocks page, params: searchstr = $searchStr, count = $count, itemId = $itemId")
  //    for {
  //      stocksPage <- env.stockDao.getPagedQueryWithFind(searchStr, itemId - 1, count + 1)
  //      stocksSize <- env.stockDao.getLastId
  //
  //      stocksPageLastId = stocksPage.last.id
  //      lastId = if (stocksPageLastId == stocksSize) 0 else stocksPageLastId // ? x: y doesnt work
  //    } yield Responses.StocksPage(
  //      lastId, itemId,
  //      stocksPage.take(count).map(s => s.as[Responses.Stock]))
  //  }

}
