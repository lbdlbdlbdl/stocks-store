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

case class Companion(user: User, pack: Option[StocksPackage], stock: Stock)

class TransactionService(implicit val userDao: UserDao,
                         val stockDao: StockDao,
                         val stocksPackageDao: StocksPackageDao,
                         val transactionHistoryDao: TransactionHistoryDao) {

  //достаем инфу о пользователе о его пакете на акцию и информацию о самой акции
  def companion(login: String, stockId: Long, amount: Int): Future[Companion] =
    for {
      maybeUser <- userDao.find(login)
      userInfo = maybeUser.getOrElse(throw NotFoundException("User not found."))
      maybeStock <- stockDao.getStockOption(stockId)
      stock = maybeStock.getOrElse(throw NotFoundException(s"Stock not found id=$stockId."))
      pack <- stocksPackageDao.findByStock(userInfo.id.get, stockId)
    } yield Companion(userInfo, pack, stock)


  private def timeNow = LocalDateTime.now()

  def buyStock(login: String, stockId: Long, amount: Int): Future[TransactionSuccess] =
    for {
      cmp <- companion(login: String, stockId: Long, amount: Int)
      buyStock <-
        if (cmp.user.balance < cmp.stock.buyPrice * amount) throw ValidationException("Insufficient funds in the account")
        else {
          val newBalance = cmp.user.balance - cmp.stock.buyPrice * amount
          //          env.logger.info(s"update user $login new balance $newBalance")
          userDao.updateBalance(login, newBalance)
        }
      addStockPackage <- cmp.pack match {
        case Some(value) => stocksPackageDao.updatePackage(stockId, value.count + amount)
        case None => stocksPackageDao.add(StocksPackage(None, cmp.user.id.get, stockId, amount))
      }
      history <- transactionHistoryDao.add(
        TransactionHistory(None, login, stockId, amount, cmp.stock.buyPrice * amount, timeNow, "buy"))
    } yield TransactionSuccess()


  def saleStock(login: String, stockId: Long, amount: Int): Future[TransactionSuccess] =
    for {
      cmp <- companion(login: String, stockId: Long, amount: Int)
      _ <- cmp.pack match {
        case Some(value) =>
          if (amount > value.count) throw ValidationException("Not enough shares in the account")
          else stocksPackageDao.updatePackage(stockId, value.count - amount
          ) andThen { case Success(value) =>
            val newBalance = cmp.user.balance + cmp.stock.salePrice * amount
            //            env.logger.info(s"update user $login new balanace $newBalance")
            userDao.updateBalance(login, newBalance)
          } andThen { case _ =>
            transactionHistoryDao.add(
              TransactionHistory(None, login, stockId, amount, cmp.stock.salePrice * amount, timeNow, "sell"))
          }
        case None => throw ValidationException("Not enough shares in the account")
      }
    } yield TransactionSuccess()


  def transactionHistory2Response(value: TransactionHistory): Future[TransactionHistoryResponse] =
    for {
      stock <- stockDao.getStock(value.stockId)
    } yield TransactionHistoryResponse(
      StockHistory(stock.id, stock.code, stock.name, stock.iconUrl),
      value.amount, value.totalPrice, value.date, value.`type`)


  def transactionHistoryPage(searchStr: String, count: Int, itemId: Int): Future[TransactionHistoryPage] = {
    //    env.logger.info(s"begin get trans. history page, params: searchstr = $searchStr, count = $count, itemId = $itemId")
    for {
      tHisesPage <- transactionHistoryDao.getPagedQueryWithFind(searchStr, itemId, count + 1)
      responses <- Future.sequence(tHisesPage.map(th => transactionHistory2Response(th)))
      tHisesPageLastId = tHisesPage.reverse.last.id
      tHisLasiId <- transactionHistoryDao.getLastId
      lastId = if (tHisesPageLastId.get == tHisLasiId.get) 0 else tHisesPageLastId.get // ? x: y doesnt work
    } yield TransactionHistoryPage(lastId, itemId, responses.take(count).reverse)
  }

}
