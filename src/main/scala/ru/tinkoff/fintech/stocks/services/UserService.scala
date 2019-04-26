package ru.tinkoff.fintech.stocks.services

import akka.actor.ActorSystem
import akka.event.Logging
import ru.tinkoff.fintech.stocks.db.models._
//{Stock, StocksPackage, User}
import ru.tinkoff.fintech.stocks.exception.Exceptions._
import ru.tinkoff.fintech.stocks.http._
import ru.tinkoff.fintech.stocks.http.dtos.Responses._
import ru.tinkoff.fintech.stocks.http.dtos.Requests._
import ru.tinkoff.fintech.stocks.result.Result
import JwtHelper._
import ru.tinkoff.fintech.stocks.Env
import ru.tinkoff.fintech.stocks.dao.{StocksPackageDao, UserDao}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserService(stocksService: StocksService)
                 (implicit val userDao: UserDao,
                  val stocksPackageDao: StocksPackageDao) {

  private def newUser(login: String, password: String): User =
    User(None, login, User.dummyHash(password), User.dummySalt, balance = 1000) //1000 rub

  private def addStocksForNewUser(user: User): Future[Unit] =
    for {
      _ <- stocksPackageDao.add(StocksPackage(None, user.id.get, 1, 4))
      _ <- stocksPackageDao.add(StocksPackage(None, user.id.get, 2, 2))
    } yield ()

  private def getTokensForUser(user: User): Token = generateTokensResponse(AuthData(user.login))

  def createUser(login: String, password: String): Future[Token] =
    for {
      maybeUser <- userDao.find(login)
      user <-
        if (maybeUser.isDefined) throw ValidationException("User already exists.")
        else userDao.add(newUser(login, password))
      _ <- addStocksForNewUser(user)
      tokens = getTokensForUser(user)
    } yield tokens


  def authenticate(login: String, providedPassword: String): Future[Token] =
    for {
      maybeUser <- userDao.find(login)
      maybeValidUser = maybeUser.filter(user => user.passwordHash == User.dummyHash(providedPassword))
      user <- maybeValidUser match {
        case Some(user) => Future.successful(user)
        case _ => Future.failed(UnauthorizedException("Username and password combination not found."))
      }
    } yield getTokensForUser(user)


  def refreshTokens(refreshToken: String): Future[Token] =
    Future {
      if (!isValidToken(refreshToken))
        throw UnauthorizedException("Refresh token is invalid, please log in.")
      generateTokensResponse(AuthData(getLoginFromClaim(getClaim(refreshToken))))
    }

  def accountInfo(login: String): Future[AccountInfo] =
    for {
      maybeUser <- userDao.find(login)
      user = maybeUser.getOrElse(throw NotFoundException("User not found."))
      stocksPackage <- stocksPackageDao.find(user.id.get)
      stockBatches <- stocksService.stockPackages2StockBatches(stocksPackage)
    } yield AccountInfo(login, user.balance, stockBatches)


}
