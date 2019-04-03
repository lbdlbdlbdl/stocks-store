package ru.tinkoff.fintech.stocks.services

import akka.actor.ActorSystem
import ru.tinkoff.fintech.stocks.dao.UserDao
import ru.tinkoff.fintech.stocks.dao._
import ru.tinkoff.fintech.stocks.db.{Stock, StocksPackage, User, _}
import ru.tinkoff.fintech.stocks.http.Exceptions._
import ru.tinkoff.fintech.stocks.http._

import scala.concurrent.{ExecutionContext, Future}

class UserService(val userDao: UserDao,
                  val stocksPackageDao: StocksPackageDao,
                  val stockDao: StockDao)
                 (implicit val exctx: ExecutionContext,
                  implicit val system: ActorSystem) extends JwtHelper {

  import akka.event.Logging
  val log = Logging.getLogger(system, this)

  private def newUser(login: String, password: String): User =
    User(None, login, User.dummyHash(password), User.dummySalt, balance = 1000) //1000 rub

  def addStocksForNewUser(user: User): Unit = {
    //transaction
    //UPDATE BALANCE
    log.info(s"begin add stocks for new user")
    stocksPackageDao.add(StocksPackage(None, user.id.get, 1, 4))
    stocksPackageDao.add(StocksPackage(None, user.id.get, 2, 2))
  }

  private def newStockResponse(st: Future[Stock], count: StocksPackage): Future[Responses.Stock] =
    st.map(s => Responses.Stock(s.id, s.code, s.name, s.iconUrl, s.buyPrice, 0, count.count)) //изменить priceDelta

  def accountInfo(login: String): Future[Responses.AccountInfo] = {

    def stockList(stocksPackage: List[StocksPackage], accumStocks: List[Future[Responses.Stock]] = Nil): Future[List[Responses.Stock]] = {
      stocksPackage match {
        case stock :: Nil => Future.sequence(accumStocks :+ newStockResponse(stockDao.getStock(stock.stockId), stock))
        case stock :: tail => stockList(tail, accumStocks :+ newStockResponse(stockDao.getStock(stock.stockId), stock))
        case _ => Future(Nil)
      }
    }

    for {
      maybeUser <- userDao.find(login)
      user =
      if (maybeUser.isEmpty) throw new Exception("User not found.")
      else maybeUser.get
      stocksPackage <- stocksPackageDao.find(user.id.get)
      stockList <- stockList(stocksPackage)

    } yield Responses.AccountInfo(login, user.balance, stockList)
  }

  def refreshTokens(refreshToken: String): Future[Responses.Token] =
    Future(generateTokens(Requests.AuthData(getClaim(refreshToken).content)))

  def createUser(login: String, password: String): Future[Responses.Token] =
    for {
      maybeUser <- userDao.find(login)
      user <-
        if (maybeUser.isDefined) throw ValidationException("User already exists.")
        else userDao.add(newUser(login, password))
      sp = addStocksForNewUser(user)
      tokens = getTokens(user)
    } yield Responses.Token(tokens.accessToken, tokens.refreshToken)


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
      if (maybeUser.isEmpty) throw UnauthorizedException("User not found.")
      else if (maybeUser.get.passwordHash == User.dummyHash(providedPassword)) maybeUser.get
      else throw UnauthorizedException("Username and password combination not found.")
      tokens = getTokens(correctUser)
    } yield Responses.Token(tokens.accessToken, tokens.refreshToken)

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
