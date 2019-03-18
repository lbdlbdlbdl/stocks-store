package ru.tinkoff.fintech.stocks.services

import ru.tinkoff.fintech.stocks.db.models.User

class UserService {

  def authenticate(token: String): Option[User] = ???

}
