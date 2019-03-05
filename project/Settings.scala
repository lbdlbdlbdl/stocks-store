import sbt._
import sbt.Keys._
import spray.revolver.RevolverPlugin.autoImport._

object Settings {
  val common: Seq[Def.Setting[_]] = Seq(
    scalaVersion := "2.12.8",
    scalacOptions ++= Seq(
      "-target:jvm-1.8",
      "-explaintypes",
      "-Ypartial-unification",
      "-language:higherKinds",
      "-language:implicitConversions",
    ),
    reColors := Seq("blue", "green", "magenta")
  )
}
