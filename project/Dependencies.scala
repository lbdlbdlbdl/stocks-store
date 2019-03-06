import sbt._

object V {
  val akka = "2.5.19"
  val akkaHttp = "10.1.7"
  val circe = "0.11.+"
  val akkaHttpJson = "1.24.+"
  val scalaLogging = "3.9.0"
  val logback = "1.2.+"
  val postgresql = "42.2.5.jre7"
  val flyway = "6.0.0-beta"
  val jwt = "1.1.0"
  val quill = "3.0.1"
}

object Dependencies {

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
    "io.getquill" %% "quill-async-postgres" % V.quill,
  )

  lazy val allDeps: Seq[ModuleID] =
    Seq(akkaHttp, logging, json, db).flatten
}
