package ru.tinkoff.fintech.stocks.http.routes

import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import cats.data.{Reader, ReaderT}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import ru.tinkoff.fintech.stocks.Env
import ru.tinkoff.fintech.stocks.http._
import ru.tinkoff.fintech.stocks.http.dtos.{Requests, Responses}
import ru.tinkoff.fintech.stocks.result.Result

import scala.concurrent.ExecutionContext

class UserRoutes(implicit val ec: ExecutionContext,
                 logger: LoggingAdapter) extends FailFastCirceSupport {

  val route = Reader[Env, server.Route] { env =>
    import io.circe.generic.auto._

    pathPrefix("api" / "auth") {
      path("signup") {
        post {
          entity(as[Requests.UserRequest]) { user =>
            logger.info(s"begin signup, user: $user")
            complete {
              for {
                tokens <- env.userService.createUser(user.login, user.password)
              } yield StatusCodes.OK -> tokens
            }
          }
        }
      } ~
        path("signin") {
          post {
            entity(as[Requests.UserRequest]) { user =>
              logger.info(s"begin signin, user: $user")
              complete {
                for {
                  tokens <- env.userService.authenticate(user.login, user.password)
                } yield StatusCodes.OK -> tokens
              }
            }
          }
        } ~
        path("refresh") {
          post {
            entity(as[Requests.RefreshToken]) { refreshToken =>
              logger.info(s"begin refresh token")
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
