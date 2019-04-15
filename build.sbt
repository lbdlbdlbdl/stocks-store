lazy val stocksStore =
  (project in file("."))
    .enablePlugins(JavaAppPackaging)
    .settings(
      name := "stocks-store",
      version := "0.1"
    )
    .settings(Settings.common)
    .settings(libraryDependencies ++= Dependencies.allDeps)

libraryDependencies ++= Seq(
  "com.pauldijou" %% "jwt-core" % "2.1.0"
)
libraryDependencies ++= Seq(
  "com.pauldijou" %% "jwt-circe" % "2.1.0"
)
libraryDependencies += "ch.megard" %% "akka-http-cors" % "0.4.0"
