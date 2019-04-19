package ru.tinkoff.fintech.stocks.http.routes

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import cats.data.Reader
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import ru.tinkoff.fintech.stocks.Env
import ru.tinkoff.fintech.stocks.http._

import scala.concurrent.ExecutionContext.Implicits.global

class AccountRoutes extends FailFastCirceSupport with JwtHelper {

  val route = Reader[Env, server.Route] { env =>
    import io.circe.generic.auto._

    pathPrefix("api" / "account") {
      path("info") {
        authenticated { claim =>
          get {
            val login = getLoginFromClaim(claim)
//            log.info(s"get account info for user: $login")
            complete {
              for {
                accountInfo <- env.userService.accountInfo(login).run(env)
              } yield StatusCodes.OK -> accountInfo
            }
          }
        }
      }
    }
  }
}