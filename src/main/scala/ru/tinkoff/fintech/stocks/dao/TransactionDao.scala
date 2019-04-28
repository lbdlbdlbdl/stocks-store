package ru.tinkoff.fintech.stocks.dao

import ru.tinkoff.fintech.stocks.db.models.{StocksPackage, User}
import ru.tinkoff.fintech.stocks.exception.Exceptions.ValidationException

import scala.concurrent.{ExecutionContext, Future}

class TransactionDao(implicit val ec: ExecutionContext) {

  import quillContext._

  def transactionBuy(idStock: Long, idUser: Long, price: Double, count: Int): Future[Unit] = {
    val bag = StocksPackage(None, idUser, idStock, count)
    val updateSt = quote {
      query[StocksPackage].filter(s => s.stockId == lift(idStock) && s.userId == lift(idUser)).update {
        ent => ent.count -> (ent.count + lift(count))
      }
    }
    val addSt = quote {
      query[StocksPackage].insert(lift(bag)).returning(_.id)
    }

    val updateBal = quote {
      query[User].filter(s => s.id.forall(_ == lift(idUser)) && s.balance >= lift(price)).update {
        ent => ent.balance -> (ent.balance - lift(price))
      }
    }

    transaction { implicit ec =>
      for {
        upd <- run(updateSt)
        _ <- if (upd == 0) run(addSt).map(newId => bag.copy(id = newId)) else Future.unit
        bal <- run(updateBal)
      } yield {
        if (bal == 0) throw ValidationException("Insufficient funds in the account"); ()
      }
    }
  }

  def transactionSell(idStock: Long, idUser: Long, price: Double, count: Int): Future[Unit] = {
    val updateSt = quote {
      query[StocksPackage].filter(s => s.stockId == lift(idStock) && s.userId == lift(idUser) && s.count >= lift(count))
        .update {
          ent => ent.count -> (ent.count - lift(count))
        }
    }
    val updateBal = quote {
      query[User].filter(_.id.forall(_ == lift(idUser))).update {
        ent => ent.balance -> (ent.balance + lift(price))
      }
    }
    transaction { implicit ec =>
      for {
        upd <- run(updateSt)
        _ <- run(updateBal)
      } yield {
        if (upd == 0) throw ValidationException("Not enough shares in the account"); ()
      }

    }
  }
}