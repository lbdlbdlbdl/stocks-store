package ru.tinkoff.fintech.stocks

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.{optionalHeaderValueByName, provide}
import pdi.jwt.JwtClaim
import ru.tinkoff.fintech.stocks.exception.Exceptions.{ExpiredTokenException, UnauthorizedException}
import ru.tinkoff.fintech.stocks.http.JwtHelper.decodeToken

package object http {

  def authenticated: Directive1[JwtClaim] =
    optionalHeaderValueByName("Authorization") flatMap {
      case Some(token) =>
        decodeToken(token)
          .map(provide)
          .getOrElse(throw ExpiredTokenException())
      case _ => throw UnauthorizedException() //если нет в header'e ничего
    }
}
