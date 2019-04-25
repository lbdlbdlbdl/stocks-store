package ru.tinkoff.fintech.stocks.dao

import ru.tinkoff.fintech.stocks.db.models.Stock

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StockDao{

  import quillContext._

  //найдем описание акции
  def getStock(id: Long): Future[Stock] = {
    run(quote {
      query[Stock].filter(_.id == lift(id)).take(1)
    }).map(_.head)
  }

  def getStockOption(id: Long): Future[Option[Stock]] = {
    run(quote {
      query[Stock].filter(_.id == lift(id)).take(1)
    }).map(_.headOption)
  }

  def idsList(): Future[List[Long]] = {
    run(quote {
      query[Stock].map(_.id)
    })
  }

  def updatePrices(id: Long, buyPrice: Double, sellPrice: Double): Future[Unit] = {
      run(
        //        quote(infix"UPDATE Stock SET buyPrice = $buyPrice, salePrice = $sellPrice WHERE id = $id")
        quote {
          query[Stock].filter(_.id == lift(id)).update(_.salePrice -> lift(sellPrice), _.buyPrice -> lift(buyPrice))
        }
      ).map(_ => ())
  }

  def findStrInName(str: String): Future[List[Stock]] = {
    run(quote {
      query[Stock].filter(s => s.name like s"%${lift(str)}%")
    })
  }

  def getPagedQueryWithFind(searchedStr: String, offset: Int, querySize: Int): Future[List[Stock]] = {
    run(quote {
      query[Stock]
        .drop(lift(offset - 1))
        .filter(s => s.name.toLowerCase like lift(searchedStr.toLowerCase))//s"%${lift(searchedStr.toLowerCase)}%")
        .take(lift(querySize))
    })
  }
}
