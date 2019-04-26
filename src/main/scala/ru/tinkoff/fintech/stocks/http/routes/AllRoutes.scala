package ru.tinkoff.fintech.stocks.http.routes

import akka.http.scaladsl.server.Directives._

import ru.tinkoff.fintech.stocks.Env

class AllRoutes(env: Env) {

  val ur = new UserRoutes().route.run(env) //very simple monadic way
  val ar = new AccountRoutes().route.run(env)
  val sr = new StockRoutes().route.run(env)
  val tr = new TransactionRoutes().route.run(env)

  def routes = ur ~ ar ~ sr ~ tr

}
