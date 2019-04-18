package ru.tinkoff.fintech.stocks.exception

object Exceptions {

  final case class ValidationException(message: String = "Invalid input data.", cause: Throwable = None.orNull)
    extends Throwable(message, cause)

  final case class UnauthorizedException(message: String = "User is not authorized.", cause: Throwable = None.orNull)
    extends Throwable(message, cause)

  final case class NotFoundException(message: String = "Requested info not found.", cause: Throwable = None.orNull)
    extends Exception(message, cause)

  final case class ExpiredTokenException(message: String = "Access token needs to be refreshed.", cause: Throwable = None.orNull)
    extends Exception(message, cause)

}
