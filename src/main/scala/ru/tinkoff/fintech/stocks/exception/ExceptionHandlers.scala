package ru.tinkoff.fintech.stocks.exception

import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives.{complete, extractUri}
import akka.http.scaladsl.server.ExceptionHandler
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import Exceptions._

object ExceptionHandlers extends FailFastCirceSupport {
  import io.circe.generic.auto._

  final case class ErrorBody(statusCode: String, message: String)

  private def completeFailedRequest[E <: Throwable](statusCode: StatusCode, exception: E) = {
    val body = ErrorBody(statusCode.toString(), s"${exception.getMessage}") // ${exception.printStackTrace()}")
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
