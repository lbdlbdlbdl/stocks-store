package ru.tinkoff.fintech.stocks.http.routes

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import cats.data.{Reader, ReaderT}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import ru.tinkoff.fintech.stocks.Env
import ru.tinkoff.fintech.stocks.http._
import ru.tinkoff.fintech.stocks.http.dtos.Requests
import ru.tinkoff.fintech.stocks.result.Result

import scala.concurrent.ExecutionContext.Implicits.global

class UserRoutes extends FailFastCirceSupport with JwtHelper {

  val route = Reader[Env, server.Route] { env =>
    import io.circe.generic.auto._

    pathPrefix("api" / "auth") {
      path("signup") {
        post {
          entity(as[Requests.UserRequest]) { user =>
//            log.info(s"begin signup, user: $user")
            complete {
              for {
                tokens <- env.userService.createUser(user.login, user.password).run(env)
              } yield StatusCodes.OK -> tokens
            }
          }
        }
      } ~
        path("signin") {
          post {
            entity(as[Requests.UserRequest]) { user =>
//              log.info(s"begin signin, user: $user")
              complete {
                for {
                  tokens <- env.userService.authenticate(user.login, user.password).run(env)
                } yield StatusCodes.OK -> tokens
              }
            }
          }
        } ~
        path("refresh") {
          post {
            entity(as[Requests.RefreshToken]) { refreshToken =>
//              log.info(s"begin refresh token")
              complete {
                for {
                  tokens <- env.userService.refreshTokens(refreshToken.refreshToken)
                } yield StatusCodes.OK -> tokens
              }
            }
          }
        }
    }
  }
}
