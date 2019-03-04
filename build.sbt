lazy val stocksStore =
  (project in file("."))
    .enablePlugins(JavaAppPackaging)
    .settings(version := "0.1")
    .settings(Settings.common)
    .settings(libraryDependencies ++= Dependencies.allDeps)
