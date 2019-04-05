package ru.tinkoff.fintech.stocks.services

import akka.actor.ActorSystem
import ru.tinkoff.fintech.stocks.dao.{StockDao, StocksPackageDao, UserDao}
import ru.tinkoff.fintech.stocks.db._
import ru.tinkoff.fintech.stocks.http.Exceptions._
import ru.tinkoff.fintech.stocks.http.JwtHelper
import ru.tinkoff.fintech.stocks.http.Responses

import scala.concurrent.{ExecutionContext, Future}
import java.time.LocalDateTime

import ru.tinkoff.fintech.stocks.http.Responses.HistoryTransaction

case class Companion (user:User, packag:Option[StocksPackage], stock:Stock)

class TransactionService(
                          val stocksPackageDao: StocksPackageDao,
                          val stockDao: StockDao,
                          val userDao: UserDao)
                        (implicit val exctx: ExecutionContext,
                         implicit val system: ActorSystem) extends JwtHelper {

  import akka.event.Logging

  val log = Logging.getLogger(system, this)

  //переделать с использованием Cats!!!

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
    } yield ()

  }

  def saleStock(login: String, idStock: Long, amount: Int): Future[Unit] = {
    for {
      companion <- companion(login: String, idStock: Long, amount: Int)
      removePackageStock <- companion.packag match {
        case Some(value) =>
          if(amount>value.count) throw new Exception("Not enough shares in the account")
          else stocksPackageDao.updatePackage(idStock, value.count - amount)
        case None => throw new Exception("Not enough shares in the account")
      }
      sellStock <- {
        val newBalance = companion.user.balance + companion.stock.buyPrice * amount
        log.info(s"update user $login new balanace $newBalance")
        userDao.updateBalance(login, newBalance)
      }

    } yield ()


  }

  def history(login: String): Future[HistoryTransaction] = {
    for {


    } yield HistoryTransaction()


  }
}
