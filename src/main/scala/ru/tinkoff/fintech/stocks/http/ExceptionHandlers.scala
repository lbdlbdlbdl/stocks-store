package ru.tinkoff.fintech.stocks.http

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import ru.tinkoff.fintech.stocks.http.Exceptions._

object ExceptionHandlers extends FailFastCirceSupport {

  final case class ErrorBody(statusCode: String, message: String)

  private def completeFailedRequest[E <: Throwable](statusCode: StatusCode, exception: E) = {

    val body = ErrorBody(statusCode.toString(), exception.getMessage)
    extractUri { uri => complete(statusCode, body) }
  }

  implicit def CustomExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case unEx: UnauthorizedException => completeFailedRequest[UnauthorizedException](Unauthorized, unEx)
      case vEx: ValidationException => completeFailedRequest[ValidationException](BadRequest, vEx)
      case nfEx: NotFoundException => completeFailedRequest[NotFoundException](NotFound, nfEx)
      case expEx: ExpiredTokenException => completeFailedRequest[ExpiredTokenException](Forbidden, expEx)
      case ex: Exception => completeFailedRequest[Exception](InternalServerError, ex)
    }
}
