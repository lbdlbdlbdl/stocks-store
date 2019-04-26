package ru.tinkoff.fintech.stocks.db

import java.nio.charset.StandardCharsets
import java.time.LocalDateTime

package object models {

  final case class User(id: Option[Long],
                        login: String,
                        passwordHash: String,
                        salt: String,
                        iconUrl: String = "icon.jpg",
                        balance: Double)

  object User {

    def dummySalt = scala.util.Random.nextString(128) //scala.util.Random.alphanumeric.take(128).mkString

    def dummyHash(str: String): String = {
      import java.security.MessageDigest
      val digest = MessageDigest.getInstance("SHA-256")
      digest.reset()
      val bytes = digest.digest(str.getBytes(StandardCharsets.UTF_8))
      bytes.map("%02x".format(_)).mkString
    }
  }

  final case class Stock(id: Long,
                         name: String,
                         code: String,
                         iconUrl: String,
                         salePrice: Double,
                         buyPrice: Double)

  final case class StocksPackage(id: Option[Long],
                                 userId: Long,
                                 stockId: Long,
                                 count: Int)

  final case class PriceHistory(id: Option[Long],
                                stockId: Long,
                                date: LocalDateTime,
                                salePrice: Double,
                                buyPrice: Double)

  final case class TransactionHistory(id: Option[Long],
                                      login: String,
                                      stockId: Long,
                                      amount: Int,
                                      totalPrice: Double,
                                      date: LocalDateTime,
                                      `type`: String)
}
