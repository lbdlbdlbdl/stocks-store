package ru.tinkoff.fintech.stocks.services

import akka.actor.ActorSystem
import ru.tinkoff.fintech.stocks.dao.{StockDao, StocksPackageDao, TransactionHistoryDao, UserDao}
import ru.tinkoff.fintech.stocks.db._
import ru.tinkoff.fintech.stocks.http.Exceptions._
import ru.tinkoff.fintech.stocks.http.JwtHelper
import ru.tinkoff.fintech.stocks.http.dtos.Responses

import scala.concurrent.{ExecutionContext, Future}
import java.time.LocalDateTime

case class Companion(user: User, packag: Option[StocksPackage], stock: Stock)

class TransactionService(val stocksPackageDao: StocksPackageDao,
                         val stockDao: StockDao,
                         val userDao: UserDao,
                         val transactionDao: TransactionHistoryDao)
                        (implicit val exctx: ExecutionContext,
                         implicit val system: ActorSystem) extends JwtHelper {

  import akka.event.Logging

  val log = Logging.getLogger(system, this)

  //достаем инфу о пользователе о его пакете на акцию и информацию о самой акции
  def companion(login: String, stockId: Long, amount: Int): Future[Companion] =
    for {
      user <- userDao.find(login)
      userInfo = user.getOrElse(throw new Exception("User not found."))
      stock <- stockDao.getStockOption(stockId)
      package_ <-
        if (stock.isDefined) stocksPackageDao.findByStock(userInfo.id.get, stockId)
        else throw NotFoundException(s"Stock not found id=$stockId.")
    } yield Companion(userInfo, package_, stock.get)


  def timeNow: String = LocalDateTime.now().toString

  def buyStock(login: String, stockId: Long, amount: Int): Future[Responses.TransactionSuccess] = {
    for {
      companion <- companion(login: String, stockId: Long, amount: Int)
      buyStock <-
        if (companion.user.balance < companion.stock.buyPrice * amount) throw ValidationException("Insufficient funds in the account")
        else {
          val newBalance = companion.user.balance - companion.stock.buyPrice * amount
          log.info(s"update user $login new balance $newBalance")
          userDao.updateBalance(login, newBalance)
        }
      addStockPackage <- companion.packag match {
        case Some(value) => stocksPackageDao.updatePackage(stockId, value.count + amount)
        case None => stocksPackageDao.add(StocksPackage(None, companion.user.id.get, stockId, amount))
      }
      history <- transactionDao.add(
        TransactionHistory(None, login, stockId, amount, companion.stock.buyPrice * amount, timeNow, "buy"))
    } yield Responses.TransactionSuccess()
  }

  def saleStock(login: String, stockId: Long, amount: Int): Future[Responses.TransactionSuccess] =
    for {
      companion <- companion(login: String, stockId: Long, amount: Int)
      removeStockPackage <- companion.packag match {
        case Some(value) =>
          if (amount > value.count) throw ValidationException("Not enough shares in the account")
          else stocksPackageDao.updatePackage(stockId, value.count - amount)
        case None => throw ValidationException("Not enough shares in the account")
      }
      sellStock <- {
        val newBalance = companion.user.balance + companion.stock.salePrice * amount
        log.info(s"update user $login new balanace $newBalance")
        userDao.updateBalance(login, newBalance)
      }
      history <- transactionDao.add(
        TransactionHistory(None, login, stockId, amount, companion.stock.salePrice * amount, timeNow, "sell"))

    } yield Responses.TransactionSuccess()


  def transformation(value: TransactionHistory): Future[Responses.TransactionHistory] = {
    for {
      stock <- stockDao.getStock(value.stockId)
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

  def transactionHistoryPage(searchStr: String, count: Int, itemId: Int): Future[Responses.TransactionHistoryPage] = {
    log.info(s"begin get trans. history page, params: searchstr = $searchStr, count = $count, itemId = $itemId")
    for {
      tHises <- transactionDao.getPagedQueryWithFind(searchStr, itemId, count + 1)
      responses <- Future.sequence(tHises.map(transformation))
      lastId = tHises.last.id
    } yield Responses.TransactionHistoryPage(lastId.get, itemId, responses.take(count).reverse)
  }

}
