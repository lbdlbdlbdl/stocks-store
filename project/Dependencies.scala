import sbt._

object V {
  val scalatest = "3.0.5"
  val akka = "2.5.+"
  val akkaHttp = "10.1.+"
  val circe = "0.11.+"
  val akkaHttpJson = "1.24.+"
  val scalaLogging = "3.9.+"
  val logback = "1.2.+"
  val postgresql = "42.2.5.jre7"
  val flyway = "6.0.0-beta"
  val jwt = "1.1.0"
}

object Dependencies {

  lazy val scalatest = Seq(
    "org.scalatest" %% "scalatest" % V.scalatest % Test,
  )

  lazy val akkaHttp = Seq(
    "com.typesafe.akka" %% "akka-http" % V.akkaHttp,
    "com.typesafe.akka" %% "akka-stream" % V.akka,
    "com.typesafe.akka" %% "akka-slf4j" % V.akka,
    "de.heikoseeberger" %% "akka-http-circe" % V.akkaHttpJson,
  )

  lazy val json = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser",
  ).map(_ % V.circe)

  lazy val jwt = Seq(
    "com.pauldijou" %% "jwt-core" % V.jwt,
    "com.pauldijou" %% "jwt-circe" % V.jwt,
  )

  lazy val logging = Seq(
    "ch.qos.logback" % "logback-classic" % V.logback,
    "com.typesafe.scala-logging" %% "scala-logging" % V.scalaLogging,
  )

  lazy val db = Seq(
    "org.postgresql" % "postgresql" % V.postgresql,
    "org.flywaydb" % "flyway-core" % V.flyway,
    "io.getquill" %% "quill-async-postgres" % "3.0.1"
  )

  lazy val allDeps: Seq[ModuleID] =
    Seq(akkaHttp, logging, json, db).flatten
}
