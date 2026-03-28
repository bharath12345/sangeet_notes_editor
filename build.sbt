val scala3Version = "3.4.2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "sangeet-notes-editor",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked"),
    externalResolvers := Seq(
      Resolver.mavenLocal,
      Resolver.mavenCentral,
    ),
    libraryDependencies ++= Seq(
      "org.scalafx"       %% "scalafx"        % "21.0.0-R32",
      "io.circe"          %% "circe-core"     % "0.14.7",
      "io.circe"          %% "circe-parser"   % "0.14.7",
      "io.circe"          %% "circe-generic"  % "0.14.7",
      "org.apache.pdfbox"  % "pdfbox"         % "3.0.2",
      "org.scalatest"     %% "scalatest"      % "3.2.18" % Test,
    ),
    fork := true,
  )
