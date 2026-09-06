import sbt._

object Dependencies {
  // For project/ build itself (used by UiStringsCodegen)
  val circeCore = "io.circe" %% "circe-core" % "0.14.16"
  val circeParser = "io.circe" %% "circe-parser" % "0.14.16"
}
