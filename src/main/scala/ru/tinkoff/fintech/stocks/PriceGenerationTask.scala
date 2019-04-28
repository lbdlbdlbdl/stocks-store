package ru.tinkoff.fintech.stocks

import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import ru.tinkoff.fintech.stocks.dao.{PriceHistoryDao, StockDao}
import ru.tinkoff.fintech.stocks.db.models.PriceHistory

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

//Со случайным интервалом сервер генерирует текущие цены для каждого элемента из списка акций.
class PriceGenerationTask(stockDao: StockDao,
                          priceHistoryDao: PriceHistoryDao)
                         (implicit val ec: ExecutionContext,
                          actorSystem: ActorSystem,
                          logger: LoggingAdapter) {

  //to config
  val random = new scala.util.Random

  def timeInterval = 1 + random.nextInt(15)

  def price = 50.0 + (2000.0 - 50.0) * random.nextDouble()

  def priceDifference = 20.00 + (61.00 - 20.00) * random.nextDouble() // difference between buy and sell price

  def updatePrices: Future[Unit] = {
    for {
      ids <- stockDao.idsList()
      update = ids
        .grouped(20)
        .foreach(group => group.foreach(id => {
          val sellPrice = price + priceDifference
          val buyPrice = sellPrice
          stockDao.updatePrices(id, buyPrice, sellPrice)
          priceHistoryDao.add(PriceHistory(None, id, LocalDateTime.now(), sellPrice, buyPrice))
        }))
    } yield ()
  }

  actorSystem.scheduler.schedule(initialDelay = 10.seconds, interval = timeInterval.minute) {
    logger.info("starting prices update")
    updatePrices.onComplete {
      case Success(_) => logger.info("prices successfully updated")
      case Failure(exception) => logger.error("error while prices updating: " + exception.getMessage)
    }
  }
}