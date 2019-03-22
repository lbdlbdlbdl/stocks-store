package ru.tinkoff.fintech.stocks.services

import ru.tinkoff.fintech.stocks.dao.UserDao
import ru.tinkoff.fintech.stocks.db.models.User
import ru.tinkoff.fintech.stocks.http.Exceptions._
import ru.tinkoff.fintech.stocks.http._

import scala.concurrent.{ExecutionContext, Future}

class UserService(val userDao: UserDao)
                 (implicit val exctx: ExecutionContext) extends JwtHelper {

  private def newUser(login: String, password: String): User =
    User(None, login, User.dummyHash(password), User.dummySalt)

  def refreshTokens(refreshToken: String): Future[Responses.Token] =
    Future { //наверное это плохо
      if (isValidToken(refreshToken)) {
        val claims = decodeToken(refreshToken).get
        generateTokens(Requests.AuthData(claims.content))
      } else throw UnauthorizedException("Invalid token.")
    }


  def createUser(login: String, password: String): Future[Responses.Token] =
    for {
      maybeUser <- userDao.find(login)
      user <-
        if (maybeUser.isDefined) throw ValidationException("User already exists.")
        else userDao.add(newUser(login, password))
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
