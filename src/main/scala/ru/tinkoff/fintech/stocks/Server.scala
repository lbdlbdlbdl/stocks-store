package ru.tinkoff.fintech.stocks

import akka.actor.ActorSystem
import akka.event.Logging
import akka.event.Logging.LogLevel
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.LogEntry
import akka.stream.{ActorMaterializer, Materializer}
import ch.megard.akka.http.cors.scaladsl.model.HttpOriginMatcher
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import com.typesafe.config.ConfigFactory
import io.getquill.{Escape, PostgresAsyncContext}
import org.flywaydb.core.Flyway
import ru.tinkoff.fintech.stocks.dao._
import ru.tinkoff.fintech.stocks.http._
import ru.tinkoff.fintech.stocks.http.routes._
import ru.tinkoff.fintech.stocks.services._

import scala.concurrent.ExecutionContext
import scala.util.Failure

object Server {

  def applyMigrations(jdbcUrl: String): Unit = {
    import org.postgresql.ds.PGSimpleDataSource

    val dataSource = new PGSimpleDataSource()
    dataSource.setURL(jdbcUrl)

    val flyway = Flyway.configure.dataSource(dataSource).load()
//    flyway.clean()
//    flyway.baseline()
    flyway.migrate()
  }

  def main(args: Array[String]): Unit = {

    val config = ConfigFactory.load()
    val port = config.getInt("ru.tinkoff.fintech.stocks.port")
    val jdbcUrl = config.getString("ru.tinkoff.fintech.stocks.db.url")

    applyMigrations(jdbcUrl)

    implicit val quillContext: PostgresAsyncContext[Escape] =
      new PostgresAsyncContext(Escape, "ru.tinkoff.fintech.stocks.db")


    implicit val system: ActorSystem = ActorSystem()
    implicit val executionContext: ExecutionContext = system.dispatcher
    implicit val materializer: Materializer = ActorMaterializer()

    //    implicit val logger = Logging(system, getClass)


    def requestMethodAs(logLevel: LogLevel)(req: HttpRequest) =
      LogEntry(s"${req.method.name} - ${req.uri} ${req.headers}", logLevel)

    val withLogging = {
      import akka.http.scaladsl.server.Directives.logRequest
      logRequest(requestMethodAs(Logging.InfoLevel) _)
    }

    implicit val logger = Logging.getLogger(system, this)
    implicit val userDao = new UserDao()
    implicit val stockDao = new StockDao()
    implicit val stocksPackageDao = new StocksPackageDao()
    implicit val transactionHistoryDao = new TransactionHistoryDao()
    implicit val priceHistoryDao = new PriceHistoryDao()

    val stocksService = new StocksService()
    val userService = new UserService(stocksService)
    val transactionService = new TransactionService()

    //singleton style
    val newEnv = Env(userService, stocksService, transactionService)

    val allRoutes = {

      import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
      import ru.tinkoff.fintech.stocks.exception.ExceptionHandlers._

      val routes = new AllRoutes(newEnv).routes

      val corsSettings = CorsSettings.defaultSettings.withAllowedOrigins(HttpOriginMatcher.`*`)

      withLogging {
        cors(corsSettings) {
          handleExceptions(CustomExceptionHandler) {
            routes
          }
        }
      }
    }

    def initializeTask(): Unit = new PriceGenerationTask(stockDao, priceHistoryDao)

    initializeTask()

    Http().bindAndHandle(allRoutes, interface = "0.0.0.0", port = port) andThen {
      //    Http().bindAndHandle(allRoutes, "localhost", 8081) andThen {
      case Failure(err) => err.printStackTrace(); system.terminate()

    }
  }
}
