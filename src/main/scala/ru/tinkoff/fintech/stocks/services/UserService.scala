package ru.tinkoff.fintech.stocks.services

import ru.tinkoff.fintech.stocks.dao.UserDao
import ru.tinkoff.fintech.stocks.db.models.User
import ru.tinkoff.fintech.stocks.http._

import scala.concurrent.{ExecutionContext, Future}

class UserService(val userDao: UserDao)
                 (implicit val exctx: ExecutionContext) extends JwtHelper {

  private def newUser(login: String, password: String): User =
    User(None, login, User.dummyHash(password), User.dummySalt)

  //Either?
  def createUser(login: String, password: String): Future[Either[ErrorMessage, Responses.Token]] = {
    for {
      maybeUser <- userDao.find(login)
      user <-
        if (maybeUser.isDefined) Left(ErrorMessage("User already exists."))
        else userDao.add(newUser(login, password))
      tokens = getTokens(user)
    } yield Right(Responses.Token(tokens.authToken, tokens.refreshToken))
  }

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

  //Either?
  private def getTokens(user: User): Responses.Token =
    for {
      authData <- Requests.AuthData(user.login)
      tokens = generateTokens(authData)
    } yield tokens

  //Either?
  def authenticate(login: String, providedPassword: String): Future[Either[ErrorMessage, Responses.Token]] =
    for {
      maybeUser <- userDao.find(login)
      correctUser <- maybeUser.filter(user => user.passwordHash == User.dummyHash(providedPassword)) match {
        case Some(user) => user
        case _ => Left(ErrorMessage("Username and password combination not found."))
      }
      tokens = getTokens(correctUser)
    } yield Right(Responses.Token(tokens.authToken, tokens.refreshToken))

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
