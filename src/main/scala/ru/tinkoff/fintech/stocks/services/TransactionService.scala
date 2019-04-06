package ru.tinkoff.fintech.stocks.services

import akka.actor.ActorSystem
import ru.tinkoff.fintech.stocks.dao.{StockDao, StocksPackageDao, TransactionHistoryDao, UserDao}
import ru.tinkoff.fintech.stocks.db._
import ru.tinkoff.fintech.stocks.http.Exceptions._
import ru.tinkoff.fintech.stocks.http.JwtHelper
import ru.tinkoff.fintech.stocks.http.Responses.{HistoryTransaction, StockHistory}

import scala.concurrent.{ExecutionContext, Future}
import java.time.LocalDateTime

case class Companion(user: User, packag: Option[StocksPackage], stock: Stock)

class TransactionService(
                          val stocksPackageDao: StocksPackageDao,
                          val stockDao: StockDao,
                          val userDao: UserDao,
                          val transactionDao: TransactionHistoryDao)
                        (implicit val exctx: ExecutionContext,
                         implicit val system: ActorSystem) extends JwtHelper {

  import akka.event.Logging

  val log = Logging.getLogger(system, this)

  //переделать с использованием Cats!!!

  //достаем инфу о пользователе о его пакете на акцию и информацию о самой акции
  def companion(login: String, idStock: Long, amount: Int): Future[Companion] = {
    if (amount <= 0) throw ValidationException(s"Invalid value amount=$amount")
    for {
      user <- userDao.find(login)
      userInfo = user match {
        case Some(value) => value
        case None => throw new Exception("User not found.")
      }
      package_ <- stocksPackageDao.findByStock(userInfo.id.get, idStock)
      stock <- stockDao.getStock(idStock)
    } yield Companion(userInfo, package_, stock)

  }

  def timeNow:String=LocalDateTime.now().toString

  def buyStock(login: String, idStock: Long, amount: Int): Future[Unit] = {
    for {
      companion <- companion(login: String, idStock: Long, amount: Int)
      buyStock <-
        if (companion.user.balance < companion.stock.buyPrice * amount) throw new Exception("Insufficient funds in the account")
        else {
          val newBalance = companion.user.balance - companion.stock.buyPrice * amount
          log.info(s"update user $login new balanace $newBalance")
          userDao.updateBalance(login, newBalance)
        }
      addPackageStock <- companion.packag match {
        case Some(value) => stocksPackageDao.updatePackage(idStock, value.count + amount)
        case None => stocksPackageDao.add(StocksPackage(None, companion.user.id.get, idStock, amount))
      }
      history<- transactionDao.add(TransactionHistory
      (None,login,idStock,amount,companion.stock.buyPrice * amount,timeNow,"buy"))
    } yield ()

  }

  def saleStock(login: String, idStock: Long, amount: Int): Future[Unit] = {
    for {
      companion <- companion(login: String, idStock: Long, amount: Int)
      removePackageStock <- companion.packag match {
        case Some(value) =>
          if (amount > value.count) throw new Exception("Not enough shares in the account")
          else stocksPackageDao.updatePackage(idStock, value.count - amount)
        case None => throw new Exception("Not enough shares in the account")
      }
      sellStock <- {
        val newBalance = companion.user.balance + companion.stock.salePrice * amount
        log.info(s"update user $login new balanace $newBalance")
        userDao.updateBalance(login, newBalance)
      }
      history<- transactionDao.add(TransactionHistory
      (None,login,idStock,amount,companion.stock.salePrice * amount,timeNow,"sell"))

    } yield ()


  }
  def transformation(value:TransactionHistory): Future[HistoryTransaction] ={
    for{
      stock<-stockDao.getStock(value.idStock)
      sample =StockHistory(stock.id,stock.code,stock.name)
    }yield HistoryTransaction(sample,value.amount,value.totalPrice,value.date,value.`type`)
  }

  def history(login: String): Future[List[HistoryTransaction]] = {
    for {
      list <- transactionDao.find(login)
      responses<-Future.sequence(list.map(transformation))
    } yield responses
  }
}
