package ru.tinkoff.fintech.stocks.http

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import com.typesafe.config.ConfigFactory
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import pdi.jwt._
import ru.tinkoff.fintech.stocks.exception.Exceptions._
import ru.tinkoff.fintech.stocks.http.dtos.{Requests, Responses}

/** *
  * Useful for gated routes and not only.
  */
trait JwtHelper {

  private val secretKey = ConfigFactory.load().getString("jwt.secretKey") // = "change-me-please"
  private val accessExpiration = ConfigFactory.load().getInt("jwt.token.access.expirationInSeconds")
  private val refreshExpiration = ConfigFactory.load().getInt("jwt.token.refresh.expirationInSeconds")
  private val algorithm = JwtAlgorithm.HS512

  private def generateClaim(authData: Requests.AuthData, expiration: Int): JwtClaim =
    JwtClaim(authData.asJson.toString())
      .expiresIn(expiration)
      .issuedNow

  def getClaim(token: String): JwtClaim =
    if (isValidToken(token)) {
      val claims = decodeToken(token).get
      claims
    } else throw UnauthorizedException("Invalid token.")

  def getLoginFromClaim(claim: JwtClaim): String = {
    val loginDoc: Json = parse(claim.content).getOrElse(Json.Null)
    val cursor: HCursor = loginDoc.hcursor
    val login = cursor.downField("login").as[String] match {
      case Right(value) => value
    }
    login
  }

  def generateToken(authData: Requests.AuthData, expiration: Int): String =
    JwtCirce.encode(generateClaim(authData, expiration), secretKey, algorithm)

  def generateTokensResponse(authData: Requests.AuthData): Responses.Token =
    Responses.Token(generateToken(authData, accessExpiration), generateToken(authData, refreshExpiration))

  def isValidToken(token: String): Boolean = Jwt.isValid(token, secretKey, Seq(algorithm))

  def decodeToken(token: String) = JwtCirce.decode(token, secretKey, Seq(algorithm))

  def authenticated: Directive1[JwtClaim] =
    optionalHeaderValueByName("Authorization") flatMap {
      case Some(token) =>
        decodeToken(token)
          .map(provide)
          .getOrElse(throw ExpiredTokenException())
      case _ => throw UnauthorizedException() //если нет в header'e ничего
    }

}

