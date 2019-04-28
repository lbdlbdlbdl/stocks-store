package ru.tinkoff.fintech.stocks.http.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import cats.data.Reader
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import ru.tinkoff.fintech.stocks.Env
import ru.tinkoff.fintech.stocks.http._
import JwtHelper._
import akka.event.LoggingAdapter

import scala.concurrent.ExecutionContext

class AccountRoutes(implicit val ec: ExecutionContext,
                    logger: LoggingAdapter) extends FailFastCirceSupport {

  val route = Reader[Env, server.Route] { env =>
    import io.circe.generic.auto._

    pathPrefix("api" / "account") {
      (get & authenticated & path("info")) { claim =>
        val login = getLoginFromClaim(claim)
        logger.info(s"get account info for user: $login")
        complete {
          for {
            accountInfo <- env.userService.accountInfo(login)
          } yield StatusCodes.OK -> accountInfo
        }
      }
    }
  }
}
