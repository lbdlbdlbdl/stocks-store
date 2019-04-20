package ru.tinkoff.fintech.stocks.http.routes

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.getquill.{Escape, PostgresAsyncContext}
import ru.tinkoff.fintech.stocks.dao.{PriceHistoryDao, StockDao, StocksPackageDao, UserDao}
import ru.tinkoff.fintech.stocks.http._
import ru.tinkoff.fintech.stocks.services._
import akka.http.scaladsl.server.Directives.logRequest
import ru.tinkoff.fintech.stocks.http.dtos.Requests

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class UserRoutes(implicit val exctx: ExecutionContext,
                 implicit val qctx: PostgresAsyncContext[Escape],
                 implicit val system: ActorSystem) extends FailFastCirceSupport with JwtHelper {

  import akka.event.Logging
  val log = Logging.getLogger(system, this)


  val userDao = new UserDao()
  val storageDao = new StocksPackageDao()
  val stockDao = new StockDao()
  val priceHistoryDao =new PriceHistoryDao()
  val userService = new UserService(userDao, storageDao, stockDao, priceHistoryDao)

  val authRoutes = {
    import io.circe.generic.auto._

    pathPrefix("api" / "auth") {
      path("signup") {
        post {
          entity(as[Requests.UserRequest]) { user =>
            log.info(s"begin signup, user: $user")
            val tokens = userService.createUser(user.login, user.password)
            onSuccess(tokens) { tokens => complete(StatusCodes.OK, tokens) }
          }
        }
      } ~
        path("signin") {
          post {
            entity(as[Requests.UserRequest]) { user =>
              log.info(s"begin signup, user: $user")
              val tokens = userService.authenticate(user.login, user.password)
              onSuccess(tokens) { tokens => complete(StatusCodes.OK, tokens) }
            }
          }
        } ~
        path("refresh") {
          post {
            entity(as[Requests.RefreshToken]) { refreshToken =>
              val res = userService.refreshTokens(refreshToken.refreshToken)
              onComplete(res) {
                case Success(tokens) => complete(StatusCodes.OK, tokens)
              }
            }
          }
        }
    }
  }
}
