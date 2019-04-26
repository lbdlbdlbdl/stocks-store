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

  def transactionHistoryPage(searchStr: String, count: Int, itemId: Int): Result[Responses.TransactionHistoryPage] = ReaderT { env =>
    env.logger.info(s"begin get trans. history page, params: searchstr = $searchStr, count = $count, itemId = $itemId")
    for {
      tHises <- env.transactionHistoryDao.getPagedQueryWithFind(searchStr, itemId, count + 1)
      responses <- Future.sequence(tHises.map(th => transactionHistory2Response(th).run(env)))
      lastId = tHises.last.id
    } yield Responses.TransactionHistoryPage(lastId.get, itemId, responses.take(count).reverse)
  }

}
