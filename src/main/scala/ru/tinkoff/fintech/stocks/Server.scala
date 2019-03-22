package ru.tinkoff.fintech.stocks

import akka.actor.ActorSystem
import akka.event.Logging
import akka.event.Logging.LogLevel
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.LogEntry
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.ConfigFactory
import io.getquill.{Escape, PostgresAsyncContext}
import org.flywaydb.core.Flyway

import ru.tinkoff.fintech.stocks.http.routes.UserRoutes
import ru.tinkoff.fintech.stocks.http._

import scala.concurrent.ExecutionContext
import scala.util.Failure

object Server extends JwtHelper {

  def applyMigrations(jdbcUrl: String): Unit = {
    import org.postgresql.ds.PGSimpleDataSource

    val dataSource = new PGSimpleDataSource()
    dataSource.setURL(jdbcUrl)

    val flyway = Flyway.configure.dataSource(dataSource).load()
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

    def requestMethodAs(logLevel: LogLevel)(req: HttpRequest) =
      LogEntry(s"${req.method.name} - ${req.uri}", logLevel)

    val withLogging = {
      import akka.http.scaladsl.server.Directives.logRequest
      logRequest(requestMethodAs(Logging.InfoLevel) _)
    }

    val allRoutes = {

      import ru.tinkoff.fintech.stocks.http.ExceptionHandlers._

      val ur = new UserRoutes()
      withLogging {
        handleExceptions(CustomExceptionHandler) {
          authenticated { claim =>
            ur.authRoutes
          }
        }
      }
    }
//    Http().bindAndHandle(allRoutes, interface = "0.0.0.0", port = port) andThen {
        Http().bindAndHandle(allRoutes, "0.0.0.0", port) andThen {
    case Failure(err) => err.printStackTrace(); system.terminate()
    }
  }
}
