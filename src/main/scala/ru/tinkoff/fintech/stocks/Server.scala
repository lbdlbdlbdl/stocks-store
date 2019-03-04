package ru.tinkoff.fintech.stocks

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.directives.LogEntry
import io.getquill.{PostgresAsyncContext, SnakeCase}
import com.typesafe.config.ConfigFactory
import akka.stream.ActorMaterializer
import org.flywaydb.core.Flyway

import scala.util.{Failure, Success}

object Server {

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

    lazy val quillContext =
      new PostgresAsyncContext(SnakeCase, "ru.tinkoff.fintech.stocks.db")

    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    import system.dispatcher

    {
      import quillContext._
      run(quote {
        infix"select 1".as[Int]
      })
    }

    val helloRoutes = {
      import akka.http.scaladsl.server.Directives._

      def requestMethodAsInfo(req: HttpRequest) =
        LogEntry(s"${req.method.name} - ${req.uri}", Logging.InfoLevel)

      logRequest(requestMethodAsInfo _) {
        path("hello" / Segment) { s =>
          complete(s"Hello, ${s.capitalize}!")
        } ~ pathEndOrSingleSlash {
          redirect("hello/world", StatusCodes.TemporaryRedirect)
        }
      }
    }

    Http().bindAndHandle(helloRoutes, interface = "0.0.0.0", port = port) andThen {
      case Failure(err) => err.printStackTrace(); system.terminate()
    }
  }
}
