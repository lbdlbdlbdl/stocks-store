package ru.tinkoff.fintech.stocks.http

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import com.typesafe.config.ConfigFactory
import io.circe.generic.auto._
import io.circe.syntax._
import pdi.jwt._
import ru.tinkoff.fintech.stocks.http.Exceptions._

/** *
  * Useful for gated routes and not only.
  */
class JwtHelper {

  val secretKey = ConfigFactory.load().getString("jwt.secretKey") // = "change-me-please"
  val accessExpiration = ConfigFactory.load().getInt("jwt.token.access.expirationInSeconds")
  val refreshExpiration = ConfigFactory.load().getInt("jwt.token.refresh.expirationInSeconds")
  val algorithm = JwtAlgorithm.HS512

  private def generateClaim(authData: Requests.AuthData, expiration: Int): JwtClaim =
    JwtClaim(authData.asJson.toString())
      .expiresIn(expiration)
      .issuedNow

  def generateToken(authData: Requests.AuthData, expiration: Int): String =
    JwtCirce.encode(generateClaim(authData, expiration), secretKey, algorithm)

  def generateTokens(authData: Requests.AuthData): Responses.Token =
    Responses.Token(generateToken(authData, accessExpiration), generateToken(authData, refreshExpiration))

  def isValidToken(token: String): Boolean = Jwt.isValid(token, secretKey, Seq(algorithm))

  def decodeToken(token: String) = JwtCirce.decode(token, secretKey, Seq(algorithm))

  def authenticated: Directive1[JwtClaim] =
    optionalHeaderValueByName("Authorization") flatMap {
      case Some(token) if isValidToken(token) =>
        decodeToken(token)
          .map(provide)
          .getOrElse(throw ExpiredTokenException())
      case _ => throw UnauthorizedException()
    }

//  def authenticated(userAction: Requests.AuthData => Route): Route = {
//
//    def extractToken(request: HttpMessage): Try[JwtClaim] = {
//      val header = request.getHeader("Authorization") //Optinonal[]
//      if (header.isPresent) {
//        val encodedToken = header.get().value()
//        if (isValidToken(encodedToken))
//          JwtCirce.decode(encodedToken, secretKey, Seq(algorithm)) match {
//            case Success(value) => Success(value)
//            case Failure(exception) => throw exception
//          }
//        else throw new Exception("Invalid token.")
//      }
//      else throw new Exception("There's no token in Authorization header.")
//    }
//
//    extractRequest { request =>
//      val token = extractToken(request)
//      token.fold(
//        exception => complete(StatusCodes.Unauthorized, exception.getMessage),
//        jwtClaim =>
//          decode[Requests.AuthData](jwtClaim.content).fold(
//            exception => complete(StatusCodes.Unauthorized, s"Malformed user data ${exception.getMessage}"), userAction)
//      )
//    }
//  }
}

