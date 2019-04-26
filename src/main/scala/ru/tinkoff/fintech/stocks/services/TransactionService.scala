package ru.tinkoff.fintech.stocks.services

import java.time.LocalDateTime

import ru.tinkoff.fintech.stocks.exception.Exceptions._
import ru.tinkoff.fintech.stocks.http.dtos.Responses._

import scala.concurrent.Future
import java.time.LocalDateTime

import ru.tinkoff.fintech.stocks.http.JwtHelper

import scala.concurrent.ExecutionContext.Implicits.global

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
