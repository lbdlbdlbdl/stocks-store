package ru.tinkoff.fintech.stocks.services

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import cats.data.OptionT
import cats.instances.future.catsStdInstancesForFuture
import cats.Functor
import cats.instances.future._
import cats.implicits.catsKernelStdOrderForFiniteDuration
//import cats.implicits._
import ru.tinkoff.fintech.stocks.db.models._
import ru.tinkoff.fintech.stocks.exception.Exceptions._
import ru.tinkoff.fintech.stocks.http._
import ru.tinkoff.fintech.stocks.http.dtos.Responses._
import ru.tinkoff.fintech.stocks.http.dtos.Requests._
import JwtHelper._
import ru.tinkoff.fintech.stocks.dao.{StocksPackageDao, UserDao}

import scala.concurrent.{ExecutionContext, Future}

class UserService(stocksService: StocksService,
                  userDao: UserDao,
                  stocksPackageDao: StocksPackageDao)
                 (implicit val ec: ExecutionContext,
                  logger: LoggingAdapter) {

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
      user <- maybeUser match {
        case Some(_) => throw ValidationException("User already exists.")
        case None => userDao.add(newUser(login, password))
      }
      _ <- addStocksForNewUser(user)
      tokens = getTokensForUser(user)
    } yield tokens


  def authenticate(login: String, providedPassword: String): Future[Token] =
    for {
      maybeUser <- userDao.find(login)
      maybeValidUser = maybeUser.filter(user => user.passwordHash == User.dummyHash(providedPassword))
      user = maybeValidUser match {
        case Some(usr) => usr
        case _ => throw UnauthorizedException("Username and password combination not found.")
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
      user <- OptionT(userDao.find(login)).getOrElse(throw NotFoundException("User not found."))
      stocksPackage <- stocksPackageDao.find(user.id.get, with0count = false)
      stockBatches <- stocksService.stockPackages2StockBatches(stocksPackage)
    } yield AccountInfo(login, user.balance, stockBatches)


}
