package ru.tinkoff.fintech.stocks.http

import akka.http.scaladsl.model.{HttpMessage, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.config.ConfigFactory
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import pdi.jwt._

import scala.util.{Failure, Success}

/** *
  * Useful for gated routes and not only.
  */
trait JwtHelper {

  val secretKey = ConfigFactory.load().getString("jwt.secretKey") //"change-me-please"
  val accessExpiration = ConfigFactory.load().getInt("jwt.token.access.expirationInSeconds")
  val refreshExpiration = ConfigFactory.load().getInt("jwt.token.refresh.expirationInSeconds")
  val algorithm = JwtAlgorithm.HS512

  def generateTokens(authData: Requests.AuthData): Responses.Token = {
    val accessClaim = JwtClaim(authData.asJson.toString())
      .expiresIn(accessExpiration)
      .issuedNow

    val refreshClaim = JwtClaim(authData.asJson.toString())
      .expiresIn(refreshExpiration)
      .issuedNow

    val accessToken = JwtCirce.encode(accessClaim, secretKey, algorithm)
    val refreshToken = JwtCirce.encode(refreshClaim, secretKey, algorithm)

    Responses.Token(accessToken, refreshToken)
  }

  private def isValidToken(token: String): Boolean = Jwt.isValid(token, secretKey, Seq(algorithm))

  def authenticated(userAction: Requests.AuthData => Route): Route = {

    def extractToken(request: HttpMessage): Either[ErrorMessage, JwtClaim] = {
      val header = request.getHeader("Authorization") //Optinonal[]
      if (header.isPresent) {
        val encodedToken = header.get().value()
        if (isValidToken(encodedToken))
          JwtCirce.decode(encodedToken, secretKey, Seq(algorithm)) match {
            case Success(value) => Right(value)
            case Failure(exception) => Left(ErrorMessage(exception.getMessage))
          }
        else Left(ErrorMessage("Invalid token."))
      }
      else Left(ErrorMessage("There's no token in Authorization header."))
    }

    extractRequest { request =>
      val token = extractToken(request)
      token.fold(
        error => complete(StatusCodes.Unauthorized, error.message),
        jwtClaim =>
          decode[Requests.AuthData](jwtClaim.content).fold(
            error => complete(StatusCodes.Unauthorized, s"Malformed user data ${error.getMessage}"), userAction)
      )
    }
  }
}

