package ru.tinkoff.fintech.stocks.http

import com.typesafe.config.ConfigFactory
import io.circe.parser._
import io.circe._
import io.circe.generic.auto._
import io.circe.{HCursor, Json}
import io.circe.syntax._
import pdi.jwt.{Jwt, JwtAlgorithm, JwtCirce, JwtClaim}
import ru.tinkoff.fintech.stocks.exception.Exceptions.{ExpiredTokenException, UnauthorizedException}
import ru.tinkoff.fintech.stocks.http.dtos.Responses._
import ru.tinkoff.fintech.stocks.http.dtos.Requests._


/** *
  * Useful for gated routes and not only.
  */
object JwtHelper {

  private val secretKey = ConfigFactory.load().getString("jwt.secretKey") // = "change-me-please"
  private val accessExpiration = ConfigFactory.load().getInt("jwt.token.access.expirationInSeconds")
  private val refreshExpiration = ConfigFactory.load().getInt("jwt.token.refresh.expirationInSeconds")
  private val algorithm = JwtAlgorithm.HS512

  private def generateClaim(authData: AuthData, expiration: Int): JwtClaim =
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

  def generateToken(authData: AuthData, expiration: Int): String =
    JwtCirce.encode(generateClaim(authData, expiration), secretKey, algorithm)

  def generateTokensResponse(authData: AuthData): Token =
    Token(generateToken(authData, accessExpiration), generateToken(authData, refreshExpiration))

  def isValidToken(token: String): Boolean = Jwt.isValid(token, secretKey, Seq(algorithm))

  def decodeToken(token: String) = JwtCirce.decode(token, secretKey, Seq(algorithm))

}
