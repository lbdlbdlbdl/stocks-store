package ru.tinkoff.fintech.stocks.http

import akka.http.scaladsl.model._
import StatusCodes._
import akka.http.scaladsl.server._
import Directives._

import Exceptions._

object ExceptionHandlers {

  private def completeFailedRequest[E <: Throwable](statusCode: StatusCode, exception: E) =
    extractUri { uri => complete(HttpResponse(statusCode, entity = exception.getMessage)) }

  val CustomExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case unEx: UnauthorizedException => completeFailedRequest[UnauthorizedException](Unauthorized, unEx)
      case vEx: ValidationException => completeFailedRequest[ValidationException](BadRequest, vEx)
      case nfEx: NotFoundException => completeFailedRequest[NotFoundException](NotFound, nfEx)
      case expEx: ExpiredTokenException => completeFailedRequest[ExpiredTokenException](Forbidden, expEx)
      case ex: Exception => completeFailedRequest[Exception](InternalServerError, ex)
    }
}
