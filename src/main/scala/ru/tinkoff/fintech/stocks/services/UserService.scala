package ru.tinkoff.fintech.stocks.services

import akka.actor.ActorSystem
import akka.event.Logging
import cats.data.{Reader, ReaderT}
import ru.tinkoff.fintech.stocks.db.{Stock, StocksPackage, User, _}
import ru.tinkoff.fintech.stocks.exception.Exceptions._
import ru.tinkoff.fintech.stocks.http._
import ru.tinkoff.fintech.stocks.http.dtos.Responses._
import ru.tinkoff.fintech.stocks.http.dtos.Requests._
import ru.tinkoff.fintech.stocks.result.Result

import JwtHelper._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class UserService { //кусочек ооп


  private def newUser(login: String, password: String): User =
    User(None, login, User.dummyHash(password), User.dummySalt, balance = 1000) //1000 rub

  private def addStocksForNewUser(user: User): Result[Unit] = ReaderT { env =>
    for {
      _ <- env.stocksPackageDao.add(StocksPackage(None, user.id.get, 1, 4))
      _ <- env.stocksPackageDao.add(StocksPackage(None, user.id.get, 2, 2))
    } yield ()
  }

  private def getTokensForUser(user: User): Token = generateTokensResponse(AuthData(user.login))

  def createUser(login: String, password: String): Result[Token] = ReaderT { env =>
    for {
      maybeUser <- env.userDao.find(login)
      user <-
        if (maybeUser.isDefined) throw ValidationException("User already exists.")
        else env.userDao.add(newUser(login, password))
      _ <- addStocksForNewUser(user).run(env)
      tokens = getTokensForUser(user)
    } yield tokens
  }

  def authenticate(login: String, providedPassword: String): Result[Token] = ReaderT { env =>
    for {
      maybeUser <- env.userDao.find(login)
      user <- maybeUser.filter(user => user.passwordHash == User.dummyHash(providedPassword)) match {
        case Some(user) => user
        case _ => throw UnauthorizedException("Username and password combination not found.")
      }
    } yield getTokensForUser(user)
  }

  def refreshTokens(refreshToken: String): Future[Token] = {
    Future {
      if (!isValidToken(refreshToken))
        throw UnauthorizedException("Refresh token is invalid, please log in.")
      generateTokensResponse(AuthData(getLoginFromClaim(getClaim(refreshToken))))
    }
  }

  def accountInfo(login: String): Result[AccountInfo] = ReaderT {
    env =>
      for {
        maybeUser <- env.userDao.find(login)
        user = maybeUser.getOrElse(throw NotFoundException("User not found."))
        stocksPackage <- env.stocksPackageDao.find(user.id.get)
        stockBatches <- env.stocksService.stockPackages2StockBatches(stocksPackage).run(env)
      } yield AccountInfo(login, user.balance, stockBatches)
  }

}
