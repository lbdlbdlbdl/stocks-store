package ru.tinkoff.fintech.stocks.services

import akka.actor.ActorSystem
import ru.tinkoff.fintech.stocks.dao.{StockDao, StocksPackageDao, UserDao}
import ru.tinkoff.fintech.stocks.db._
import ru.tinkoff.fintech.stocks.http.JwtHelper

import scala.concurrent.{ExecutionContext, Future}

class TransactionService(
                          val stocksPackageDao: StocksPackageDao,
                          val stockDao: StockDao,
                          val userDao: UserDao)
                        (implicit val exctx: ExecutionContext,
                         implicit val system: ActorSystem) extends JwtHelper {

  import akka.event.Logging

  val log = Logging.getLogger(system, this)

  //переделать с использованием Cats!!!
  def purchaseStock(login: String, idStock: Long, amount: Int): Future[Unit] = {
    for {
      user <- userDao.find(login)
      userInfo = user match {
        case Some(value) => value
        case None => throw new Exception("User not found.")
      }
      package_ <- stocksPackageDao.findByStock(userInfo.id.get, idStock)
      stock <- stockDao.getStock(idStock)
      buyStock <-
        if (userInfo.balance < stock.buyPrice * amount) throw new Exception("Insufficient funds in the account") // изменить код ошибки
        else {
          val newBalance = userInfo.balance - stock.buyPrice * amount
          log.info(s"update user $login new balanace $newBalance")
          userDao.updateBalance(login, newBalance)
        }
      addPackageStock <- package_ match {
        case Some(value) =>stocksPackageDao.updatePackage(idStock,value.count+amount)
        case None => stocksPackageDao.add(StocksPackage(None, userInfo.id.get, idStock, amount))
      }
    }yield ()

  }
}
