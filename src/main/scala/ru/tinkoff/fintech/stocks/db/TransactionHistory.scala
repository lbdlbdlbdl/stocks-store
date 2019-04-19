package ru.tinkoff.fintech.stocks.db

import akka.http.scaladsl.server.util.Tuple
import ru.tinkoff.fintech.stocks.http.dtos.Responses

final case class TransactionHistory(id: Option[Long],
                                    login: String,
                                    stockId: Long,
                                    amount: Int,
                                    totalPrice: Double,
                                    date: String,
                                    `type`: String) {

//  def as[T](implicit f: TransactionHistory => T) = f(this)
}

object TransactionHistory {

  implicit def toTrans(stockAndHistory: Tuple2[Stock, TransactionHistory]) = stockAndHistory match {
    case (stock, trHistory) =>
      Responses.TransactionHistory(stock.as[Responses.StockHistory], trHistory.amount, trHistory.totalPrice, trHistory.date, trHistory.`type`)
  }

  implicit def convert[B, A](l: List[A])(implicit f: A => B): List[B] = l map { a => a: B }
}

