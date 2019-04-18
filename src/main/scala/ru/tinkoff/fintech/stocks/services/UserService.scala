package ru.tinkoff.fintech.stocks.services

import akka.actor.ActorSystem
import cats.data.{Reader, ReaderT}

import ru.tinkoff.fintech.stocks.db.{Stock, StocksPackage, User, _}
import ru.tinkoff.fintech.stocks.exception.Exceptions._
import ru.tinkoff.fintech.stocks.http._
import ru.tinkoff.fintech.stocks.http.dtos.{Requests, Responses}
import ru.tinkoff.fintech.stocks.result.Result

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class UserService extends JwtHelper { //кусочек ООП

  private def newUser(login: String, password: String): User =
    User(None, login, User.dummyHash(password), User.dummySalt, balance = 1000) //1000 rub

  private def newStockBatchResponse(st: Future[Stock], count: StocksPackage): Future[Responses.StockBatch] =
    st.map(s => Responses.StockBatch(s.id, s.code, s.name, s.iconUrl, s.salePrice, 0, count.count)) //изменить priceDelta

  def addStocksForNewUser(user: User): Result[Unit] = ReaderT { env =>
    for {
      //UPDATE BALANCE, transaction
      _ <- env.stocksPackageDao.add(StocksPackage(None, user.id.get, 1, 4))
      _ <-  env.stocksPackageDao.add(StocksPackage(None, user.id.get, 2, 2))
    } yield ()
  }

  def accountInfo(login: String): Result[Responses.AccountInfo] = ReaderT { env =>

    def stockList(stocksPackage: List[StocksPackage], accumStocks: List[Future[Responses.StockBatch]] = Nil): Future[List[Responses.StockBatch]] = {
      stocksPackage match {
        case stock :: Nil => Future.sequence(accumStocks :+ newStockBatchResponse(env.stockDao.getStock(stock.stockId), stock))
        case stock :: tail => stockList(tail, accumStocks :+ newStockBatchResponse(env.stockDao.getStock(stock.stockId), stock))
        case _ => Future(Nil)
      }
    }

    for {
      maybeUser <- env.userDao.find(login)
      user =
      if (maybeUser.isEmpty) throw new Exception("User not found.")
      else maybeUser.get
      stocksPackage <- env.stocksPackageDao.find(user.id.get)
      stockList <- stockList(stocksPackage)

    } yield Responses.AccountInfo(login, user.balance, stockList)
  }

  def refreshTokens(refreshToken: String): Future[Responses.Token] = {
    if (!isValidToken(refreshToken))
      throw UnauthorizedException("Refresh token is invalid, please log in.")
    Future(generateTokens(Requests.AuthData(getLoginFromClaim(getClaim(refreshToken)))))
  }

  def createUser(login: String, password: String): Result[Responses.Token] = ReaderT { env =>
    for {
      maybeUser <- env.userDao.find(login)
      user <-
        if (maybeUser.isDefined) throw ValidationException("User already exists.")
        else env.userDao.add(newUser(login, password))
      _ = addStocksForNewUser(user)//.run((uDao, sDao, spDao))
      tokens = getTokens(user)
    } yield Responses.Token(tokens.accessToken, tokens.refreshToken)
  }


  def getTokens(user: User): Responses.Token = generateTokens(Requests.AuthData(user.login))

  def authenticate(login: String, providedPassword: String): Result[Responses.Token] = ReaderT { env =>
    for {
      maybeUser <- env.userDao.find(login)
      correctUser =
      if (maybeUser.isEmpty) throw UnauthorizedException("User not found.")
      else if (maybeUser.get.passwordHash == User.dummyHash(providedPassword)) maybeUser.get
      else throw UnauthorizedException("Username and password combination not found.")
      tokens = getTokens(correctUser)
    } yield Responses.Token(tokens.accessToken, tokens.refreshToken)
  }

}
