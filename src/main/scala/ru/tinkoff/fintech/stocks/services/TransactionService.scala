package ru.tinkoff.fintech.stocks.services

import akka.actor.ActorSystem
import cats.data.{Reader, ReaderT}
import ru.tinkoff.fintech.stocks.db._
import ru.tinkoff.fintech.stocks.exception.Exceptions._
import ru.tinkoff.fintech.stocks.http.JwtHelper
import ru.tinkoff.fintech.stocks.http.dtos.Responses

import scala.concurrent.{ExecutionContext, Future}
import java.time.LocalDateTime

import ru.tinkoff.fintech.stocks.result.Result

import scala.concurrent.ExecutionContext.Implicits.global

case class Companion(user: User, packag: Option[StocksPackage], stock: Stock)

class TransactionService extends JwtHelper {

  //достаем инфу о пользователе о его пакете на акцию и информацию о самой акции
  def companion(login: String, stockId: Long, amount: Int): Result[Companion] = ReaderT { env =>
    for {
      user <- env.userDao.find(login)
      userInfo = user.getOrElse(throw new Exception("User not found."))
      stock <- env.stockDao.getStockOption(stockId)
      package_ <-
        if (stock.isDefined) env.stocksPackageDao.findByStock(userInfo.id.get, stockId)
        else throw NotFoundException(s"Stock not found id=$stockId.")
    } yield Companion(userInfo, package_, stock.get)
  }

  def timeNow: String = LocalDateTime.now().toString

  def buyStock(login: String, stockId: Long, amount: Int): Result[Responses.TransactionSuccess] = ReaderT { env =>
    for {
      companion <- companion(login: String, stockId: Long, amount: Int).run(env)
      buyStock <-
        if (companion.user.balance < companion.stock.buyPrice * amount) throw ValidationException("Insufficient funds in the account")
        else {
          val newBalance = companion.user.balance - companion.stock.buyPrice * amount
//          log.info(s"update user $login new balance $newBalance")
          env.userDao.updateBalance(login, newBalance)
        }
      addStockPackage <- companion.packag match {
        case Some(value) => env.stocksPackageDao.updatePackage(stockId, value.count + amount)
        case None => env.stocksPackageDao.add(StocksPackage(None, companion.user.id.get, stockId, amount))
      }
      history <- env.transactionHistoryDao.add(
        TransactionHistory(None, login, stockId, amount, companion.stock.buyPrice * amount, timeNow, "buy"))
    } yield Responses.TransactionSuccess()
  }

  def saleStock(login: String, stockId: Long, amount: Int): Result[Responses.TransactionSuccess] = ReaderT { env =>
    for {
      companion <- companion(login: String, stockId: Long, amount: Int).run(env)
      removeStockPackage <- companion.packag match {
        case Some(value) =>
          if (amount > value.count) throw ValidationException("Not enough shares in the account")
          else env.stocksPackageDao.updatePackage(stockId, value.count - amount)
        case None => throw ValidationException("Not enough shares in the account")
      }
      sellStock <- {
        val newBalance = companion.user.balance + companion.stock.salePrice * amount
        //        log.info(s"update user $login new balanace $newBalance")
        env.userDao.updateBalance(login, newBalance)
      }
      history <- env.transactionHistoryDao.add(
        TransactionHistory(None, login, stockId, amount, companion.stock.salePrice * amount, timeNow, "sell"))
    } yield Responses.TransactionSuccess()
  }


  def transformation(value: TransactionHistory): Result[Responses.TransactionHistory] = ReaderT { env =>
    for {
      stock <- env.stockDao.getStock(value.stockId)
      sample = Responses.StockHistory(stock.id, stock.code, stock.name, stock.iconUrl)
    } yield Responses.TransactionHistory(sample, value.amount, value.totalPrice, value.date, value.`type`)
  }

  /*
  def history(login: String): Future[List[Responses.TransactionHistory]] = {
    for {
      list <- transactionDao.find(login)
      responses <- Future.sequence(list.map(transformation))
    } yield responses
  }
  */

  def transactionHistoryPage(searchStr: String, count: Int, itemId: Int): Result[Responses.TransactionHistoryPage] = ReaderT{ env =>
//    log.info(s"begin get trans. history page, params: searchstr = $searchStr, count = $count, itemId = $itemId")
    for {
      tHises <- env.transactionHistoryDao.getPagedQueryWithFind(searchStr, itemId, count + 1)
      responses <- Future.sequence(tHises.map(transformation))
      lastId = tHises.last.id
    } yield Responses.TransactionHistoryPage(lastId.get, itemId, responses.take(count).reverse)
  }

}
