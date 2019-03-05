package ru.tinkoff.fintech.stocks

import java.nio.charset.StandardCharsets

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.directives.LogEntry
import io.getquill.{Escape, PostgresAsyncContext, SnakeCase}
import com.typesafe.config.ConfigFactory
import akka.stream.{ActorMaterializer, Materializer}
import io.getquill.context.async.{AsyncContext, TransactionalExecutionContext}
import org.flywaydb.core.Flyway

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

final case class User(
    id: Option[Long], // поле генерируется базой при вставке
    login: String,
    passwordHash: String,
    salt: String
)

object User {
  def dummySalt = scala.util.Random.nextString(128)
  def dummyHash(str: String): String = {
    import java.security.MessageDigest
    val digest = MessageDigest.getInstance("SHA-256")
    digest.reset()
    val bytes = digest.digest(str.getBytes(StandardCharsets.UTF_8))
    bytes.map("%02x".format(_)).mkString
  }
}

object Server {

  def applyMigrations(jdbcUrl: String): Unit = {
    import org.postgresql.ds.PGSimpleDataSource

    val dataSource = new PGSimpleDataSource()
    dataSource.setURL(jdbcUrl)

    val flyway = Flyway.configure.dataSource(dataSource).load()
    flyway.migrate()
  }

  class Dao(context: PostgresAsyncContext[Escape]) {
    import context._

    // поищем что-нибудь в БД
    def findUserByLogin(login: String)(
        implicit ec: ExecutionContext): Future[Option[User]] = {
      run(quote {
        query[User].filter(_.login == lift(login)).take(1)
      }).map(_.headOption)
    }

    // списочек логинов
    def listOfLogins(implicit ec: ExecutionContext): Future[List[String]] = {
      run(quote {
        query[User].map(_.login)
      })
    }

    // или добавим что-то новое
    def addUser(user: User)(implicit ec: ExecutionContext): Future[User] = {
      run(quote {
        query[User].insert(lift(user)).returning(_.id)
      }).map(newId => user.copy(id = newId))
    }
  }

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val port = config.getInt("ru.tinkoff.fintech.stocks.port")
    val jdbcUrl = config.getString("ru.tinkoff.fintech.stocks.db.url")

    applyMigrations(jdbcUrl)

    val quillContext: PostgresAsyncContext[Escape] =
      new PostgresAsyncContext(Escape, "ru.tinkoff.fintech.stocks.db")

    implicit val system: ActorSystem = ActorSystem()
    implicit val executionContext: ExecutionContext = system.dispatcher
    implicit val materializer: Materializer = ActorMaterializer()

    val dao = new Dao(quillContext)

    {
      import quillContext._

      transaction { implicit ctx =>
        for {
          maybeUser <- dao.findUserByLogin("peka")
          user <- maybeUser match {
            case Some(user) => Future.successful(user)
            case None =>
              val salt = User.dummySalt
              dao.addUser(
                User(id = None,
                     login = "peka",
                     salt = salt,
                     passwordHash = User.dummyHash("qwerty123" + salt))
              )
          }
        } yield user
      }
    }

    def requestMethodAsInfo(req: HttpRequest) =
      LogEntry(s"${req.method.name} - ${req.uri}", Logging.InfoLevel)

    val withLogging = {
      import akka.http.scaladsl.server.Directives.logRequest
      logRequest(requestMethodAsInfo _)
    }

    val helloRoutes = {
      import akka.http.scaladsl.server.Directives._

      path("hello" / Segment) { s =>
        complete(s"Hello, ${s.capitalize}!")
      } ~ pathEndOrSingleSlash {
        redirect("hello/world", StatusCodes.TemporaryRedirect)
      }
    }

    val userRoutes = {
      import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
      import akka.http.scaladsl.server.Directives._
      import io.circe.generic.auto._

      pathPrefix("user") {
        path(Segment) { login =>
          onSuccess(dao.findUserByLogin(login)) {
            case Some(user) =>
              complete(user) // wow, so secure! very safe
            case None =>
              complete(StatusCodes.NotFound, s"User $login not found")
          }
        } ~ pathEndOrSingleSlash {
          onSuccess(dao.listOfLogins) { logins =>
            complete(logins)
          }
        }
      }
    }

    val allRoutes = {
      import akka.http.scaladsl.server.Directives._

      withLogging {
        helloRoutes ~ userRoutes
      }
    }

    Http().bindAndHandle(allRoutes, interface = "0.0.0.0", port = port) andThen {
      case Failure(err) => err.printStackTrace(); system.terminate()
    }
  }
}
