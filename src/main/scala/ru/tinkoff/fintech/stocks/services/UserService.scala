package ru.tinkoff.fintech.stocks.services

import java.util.Optional

import ru.tinkoff.fintech.stocks.dao._
import ru.tinkoff.fintech.stocks.db.{Stock, Storage, User}
import ru.tinkoff.fintech.stocks.db._
import ru.tinkoff.fintech.stocks.http.Exceptions._
import ru.tinkoff.fintech.stocks.http._

import scala.concurrent.{ExecutionContext, Future}

class UserService(val userDao: UserDao, val storageDao: StorageDao, val stockDao: StockDao)
                 (implicit val exctx: ExecutionContext) extends JwtHelper {

  private def newUser(login: String, password: String): User = {
    User(None, login, User.dummyHash(password), User.dummySalt, 200)
  }

  private def newStock(st: Future[Stock], count: Storage): Future[Responses.Stock] =
    st.map(s => Responses.Stock(s.id, s.code, s.name, s.iconUrl, s.buy, 0, count.count)) //изменить priceDelta

  def accountInfo(login: String): Future[Responses.AccountInfo] = {

    def stockList(packag: List[Storage], stocks: List[Future[Responses.Stock]] = Nil): Future[List[Responses.Stock]] = {
      packag match {
        case stock :: Nil => Future.sequence(stocks :+ newStock(stockDao.infoStock(stock.idStock), stock))
        case stock :: tail => stockList(tail, stocks :+ newStock(stockDao.infoStock(stock.idStock), stock))
        case _ => Future(Nil)
      }
    }

    for {
      maybeUser <- userDao.find(login)
      user =
      if (maybeUser.isEmpty) throw new Exception("User not found.")
      else maybeUser.get
      packag <- storageDao.findById(login)
      stockList <- stockList(packag)

    } yield Responses.AccountInfo(login, user.balance, stockList)
  }


  def refreshTokens(refreshToken: String): Future[Responses.Token] =
    Future(generateTokens(Requests.AuthData(getClaim(refreshToken).content)))

  def createUser(login: String, password: String): Future[Responses.Token] =
    for {
      maybeUser <- userDao.find(login)
      user <-
        if (maybeUser.isDefined) throw new Exception("User already exists.")
        else userDao.add(newUser(login, password))
      tokens = getTokens(user)
    } yield Responses.Token(tokens.authToken, tokens.refreshToken)


  /*
  def createUser(login: String, password: String): Future[Boolean] = {
    for {
      maybeUser <- userDao.find(login)
      res <-
        if (maybeUser.isDefined) Future.successful(false)
        else userDao
          .add(User(None, login, User.dummyHash(password), User.dummySalt))
          .map(_ => true)
    } yield res
  }
  */

  def getTokens(user: User): Responses.Token = generateTokens(Requests.AuthData(user.login))

  def authenticate(login: String, providedPassword: String): Future[Responses.Token] =
    for {
      maybeUser <- userDao.find(login)
      correctUser =
      if (maybeUser.isEmpty) throw NotFoundException("User not found.")
      else if (maybeUser.get.passwordHash == User.dummyHash(providedPassword)) maybeUser.get
      else throw NotFoundException("Username and password combination not found.")
      tokens = getTokens(correctUser)
    } yield Responses.Token(tokens.authToken, tokens.refreshToken)

  /*
  def authenticate(login: String, providedPassword: String): Future[Responses.Token] =
    for {
      maybeUser <- userDao.find(login)
      correctUser <- maybeUser.filter(user => user.passwordHash == User.dummyHash(providedPassword)) match {
        case Some(user) => user
        case _ => throw new Exception("Username and password combination not found.")
      }
      tokens = getTokens(correctUser)
      res = Responses.Token(tokens.authToken, tokens.refreshToken)
    } yield res
*/
  /*
  def authenticate(login: String, providedPassword: String): Future[Requests.Token] =
    for {
      maybeUser <- userDao.find(login)
      token = maybeUser
        .filter(user => user.passwordHash == User.dummyHash(providedPassword))
        .map(user => AuthData(user.login))
        .map(authData => JwtHelper.generateTokens(authData))
        .getOrElse(throw new Exception("Username and password combination not found.") )
    } yield Requests.Token(token.authToken, token.refreshToken)
    */


}
