package ru.tinkoff.fintech.stocks

import akka.actor.ActorSystem
import akka.event.Logging
import akka.event.Logging.LogLevel
import akka.http.scaladsl.Http
import akka.http.scaladsl.server._
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.LogEntry
import akka.stream.{ActorMaterializer, Materializer}
import ch.megard.akka.http.cors.scaladsl.model.HttpOriginMatcher
import ch.megard.akka.http.cors.scaladsl.settings
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import com.typesafe.config.ConfigFactory
import io.getquill.{Escape, PostgresAsyncContext}
import org.flywaydb.core.Flyway
import ru.tinkoff.fintech.stocks.dao.StockDao
import ru.tinkoff.fintech.stocks.http.routes.{AccountRoutes, StockRoutes, TransactionRoutes, UserRoutes}
import ru.tinkoff.fintech.stocks.http._

import scala.concurrent.ExecutionContext
import scala.util.Failure

object Server extends JwtHelper {

  def applyMigrations(jdbcUrl: String): Unit = {
    import org.postgresql.ds.PGSimpleDataSource

    val dataSource = new PGSimpleDataSource()
    dataSource.setURL(jdbcUrl)

    val flyway = Flyway.configure.dataSource(dataSource).load()
    //        flyway.clean()
    //            flyway.baseline()
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


    def requestMethodAs(logLevel: LogLevel)(req: HttpRequest) = {
      LogEntry(s" ${req.uri}, HEADERS: ${req.headers}", logLevel)
    }

    val withLogging = {
      import akka.http.scaladsl.server.Directives.logRequest
      logRequest(requestMethodAs(Logging.InfoLevel) _)
    }

    val allRoutes = {

      import ru.tinkoff.fintech.stocks.http.ExceptionHandlers._
      import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

      val ur = new UserRoutes()
      val ar = new AccountRoutes()
      val sr = new StockRoutes()
      val tr = new TransactionRoutes()

      val corsSettings = CorsSettings.defaultSettings.withAllowedOrigins(HttpOriginMatcher.`*`)

      withLogging {
        cors(corsSettings) {
          handleExceptions(CustomExceptionHandler) {
            ur.authRoutes ~ ar.accountRoutes ~ sr.stocksRoutes ~ tr.stocksRoutes
          }
        }
      }
    }

    implicit val stockDao: StockDao = new StockDao()

    def initializeTask(): Unit = {
      new PriceGenerationTask()
    }

    initializeTask()
        Http().bindAndHandle(allRoutes, interface = "0.0.0.0", port = port) andThen {
//    Http().bindAndHandle(allRoutes, "localhost", 8081) andThen {
      case Failure(err) => err.printStackTrace(); system.terminate()

    }
  }
}
