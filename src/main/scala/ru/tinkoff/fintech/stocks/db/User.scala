package ru.tinkoff.fintech.stocks.db

import java.nio.charset.StandardCharsets

final case class User(id: Option[Long],
                      login: String,
                      passwordHash: String,
                      salt: String,
                      iconUrl: String = "icon.jpg",
                      balance: Double)

object User {

  def dummySalt = scala.util.Random.nextString(128) //кодировочка тута съезжает

  def dummyHash(str: String): String = {
    import java.security.MessageDigest
    val digest = MessageDigest.getInstance("SHA-256")
    digest.reset()
    val bytes = digest.digest(str.getBytes(StandardCharsets.UTF_8))
    bytes.map("%02x".format(_)).mkString
  }
}