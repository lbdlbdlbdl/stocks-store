package ru.tinkoff.fintech.stocks

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.{optionalHeaderValueByName, parameters, provide}
import pdi.jwt.JwtClaim
import ru.tinkoff.fintech.stocks.exception.Exceptions.{ExpiredTokenException, UnauthorizedException}
import ru.tinkoff.fintech.stocks.http.JwtHelper.decodeToken
import ru.tinkoff.fintech.stocks.http.dtos.Requests.{PageParameters}

import akka.http.scaladsl.server.Directives._

package object http {

  def authenticated: Directive1[JwtClaim] =
    optionalHeaderValueByName("Authorization") flatMap {
      case Some(token) =>
        decodeToken(token)
          .map(provide)
          .getOrElse(throw ExpiredTokenException())
      case _ => throw UnauthorizedException() //если нет в header'e ничего
    }

  def paginationParams: Directive1[PageParameters] =
    parameters(
      "search".?,
      "count".as[Int] ?,
      "itemId".as[Int] ?).as(PageParameters)

}
