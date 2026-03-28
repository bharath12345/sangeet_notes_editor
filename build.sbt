val scala3Version = "3.4.2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "sangeet-notes-editor",
    version := "0.1.0",
    scalaVersion := scala3Version,
    scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked"),
    externalResolvers := Seq(
      Resolver.mavenLocal,
      Resolver.mavenCentral,
    ),
    libraryDependencies ++= Seq(
      "org.scalafx"       %% "scalafx"        % "21.0.0-R32"
        excludeAll(
          ExclusionRule(organization = "org.openjfx", name = "javafx-web"),
          ExclusionRule(organization = "org.openjfx", name = "javafx-media"),
          ExclusionRule(organization = "org.openjfx", name = "javafx-swing"),
          ExclusionRule(organization = "org.openjfx", name = "javafx-fxml"),
        ),
      "io.circe"          %% "circe-core"     % "0.14.7",
      "io.circe"          %% "circe-parser"   % "0.14.7",
      "io.circe"          %% "circe-generic"  % "0.14.7",
      "org.apache.pdfbox"  % "pdfbox"         % "3.0.2",
      "org.scalatest"     %% "scalatest"      % "3.2.18" % Test,
    ),
    fork := true,

    // Assembly configuration for fat JAR
    Compile / mainClass := Some("sangeet.editor.MainApp"),
    assembly / mainClass := Some("sangeet.editor.MainApp"),
    assembly / assemblyJarName := "sangeet-notes-editor.jar",
    assembly / assemblyMergeStrategy := {
      // Exclude JavaFX native libs for other platforms (keep only current)
      case x if x.endsWith(".dll")                 => MergeStrategy.discard  // Windows natives
      case x if x.endsWith(".so")                  => MergeStrategy.discard  // Linux natives
      case PathList("META-INF", "versions", _*)    => MergeStrategy.first
      case PathList("META-INF", "MANIFEST.MF")     => MergeStrategy.discard
      case PathList("META-INF", "services", _*)    => MergeStrategy.concat
      case PathList("META-INF", _*)                => MergeStrategy.first
      case "module-info.class"                     => MergeStrategy.discard
      case x if x.endsWith(".class")               => MergeStrategy.first
      case x                                       => MergeStrategy.first
    },
  )
