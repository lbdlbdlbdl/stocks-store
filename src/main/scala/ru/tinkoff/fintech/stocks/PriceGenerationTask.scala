package ru.tinkoff.fintech.stocks

import akka.actor.ActorSystem
import ru.tinkoff.fintech.stocks.dao.StockDao

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

//Со случайным интервалом сервер генерирует текущие цены для каждого элемента из списка акций.
class PriceGenerationTask(implicit val actorSystem: ActorSystem,
                          implicit val stockDao: StockDao,
                          implicit val executionContext: ExecutionContext) {

  //to config
  val random = new scala.util.Random

  def timeInterval = 1 + random.nextInt(15)
  def price = 50.00 + (2000.00 - 50.00) * random.nextDouble()
  def priceDifference = 20.00 + (61.00 - 20.00) * random.nextDouble() // difference between buy and sell price

  actorSystem.scheduler.schedule(initialDelay = 10.seconds, interval = timeInterval.minute) {
    for {
      ids <- stockDao.idsList()
      update = ids.foreach(id => {
        val sellPrice = price
        val buyPrice = sellPrice - priceDifference
        stockDao.updatePrices(id, buyPrice, sellPrice)
      })
    } yield update
  }
}