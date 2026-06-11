val scala3Version = "3.4.2"

ThisBuild / scalaVersion := scala3Version
ThisBuild / version := "0.2.0"
ThisBuild / licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT"))
ThisBuild / scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Wunused:imports")
ThisBuild / externalResolvers := Seq(Resolver.mavenLocal, Resolver.mavenCentral)

// Scalafix
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

// Scoverage
ThisBuild / coverageMinimumStmtTotal := 80
ThisBuild / coverageFailOnMinimum := true
ThisBuild / coverageExcludedPackages := "com\\.varpas\\.sangeet\\.desktop\\..*"

lazy val root = project
  .in(file("."))
  .aggregate(sangeetCore, sangeetDesktop, sangeetServer)
  .settings(
    name := "sangeet-notes-editor",
    // Root project does not compile source directly
    Compile / sources := Seq.empty,
    Test / sources := Seq.empty,
  )

lazy val sangeetCore = project
  .in(file("sangeet-core"))
  .settings(
    name := "sangeet-core",
    libraryDependencies ++= Seq(
      "io.circe"          %% "circe-core"    % "0.14.7",
      "io.circe"          %% "circe-parser"  % "0.14.7",
      "io.circe"          %% "circe-generic" % "0.14.7",
      "org.scalatest"     %% "scalatest"     % "3.2.18" % Test,
    ),
    fork := true,
  )

val tapirVersion = "1.10.0"
val http4sVersion = "0.23.27"
val catsEffectVersion = "3.5.4"
val micrometerVersion = "1.13.0"

lazy val sangeetServer = project
  .in(file("sangeet-server"))
  .dependsOn(sangeetCore)
  .settings(
    name := "sangeet-server",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-core"              % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"        % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"     % tapirVersion,
      "org.http4s"                  %% "http4s-ember-server"      % http4sVersion,
      "org.typelevel"               %% "cats-effect"              % catsEffectVersion,
      "io.circe"                    %% "circe-core"               % "0.14.7",
      "io.circe"                    %% "circe-generic"            % "0.14.7",
      // Observability: Prometheus scrape format for local debugging + push to
      // GCP Cloud Monitoring in production (via Micrometer's Stackdriver
      // registry, which auto-authenticates via Application Default Credentials
      // on Cloud Run). JVM/process bindings come bundled with micrometer-core.
      // See docs/plans/plan-12-*.md Phase 1.
      "io.micrometer"               %  "micrometer-core"             % micrometerVersion,
      "io.micrometer"               %  "micrometer-registry-prometheus" % micrometerVersion,
      "io.micrometer"               %  "micrometer-registry-stackdriver" % micrometerVersion,
      "org.scalatest"               %% "scalatest"                % "3.2.18" % Test,
    ),
    fork := true,
    Compile / mainClass := Some("com.varpas.sangeet.server.Main"),
    assembly / mainClass := Some("com.varpas.sangeet.server.Main"),
    // sbt-assembly's default discards META-INF/maven/**. Tapir SwaggerUI needs
    // META-INF/maven/org.webjars/swagger-ui/pom.properties at startup to detect
    // the bundled webjar version — keep that file explicitly.
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "maven", "org.webjars", "swagger-ui", _*) => MergeStrategy.singleOrError
      case PathList("META-INF", "MANIFEST.MF")                            => MergeStrategy.discard
      case PathList("META-INF", "services", _*)                           => MergeStrategy.concat
      case PathList("META-INF", "versions", _*)                           => MergeStrategy.first
      case x if x.endsWith("module-info.class")                           => MergeStrategy.discard
      case _                                                              => MergeStrategy.first
    },
  )

lazy val sangeetDesktop = project
  .in(file("sangeet-desktop"))
  .dependsOn(sangeetCore)
  .settings(
    name := "sangeet-desktop",
    libraryDependencies ++= Seq(
      "org.scalafx"   %% "scalafx" % "21.0.0-R32"
        excludeAll(
          ExclusionRule(organization = "org.openjfx", name = "javafx-swing"),
          ExclusionRule(organization = "org.openjfx", name = "javafx-fxml"),
        ),
      "com.vladsch.flexmark" % "flexmark-all" % "0.64.8",
      "org.kordamp.ikonli"   % "ikonli-javafx" % "12.4.0",
      "org.kordamp.ikonli"   % "ikonli-materialdesign2-pack" % "12.4.0",
      "org.scalatest" %% "scalatest" % "3.2.18" % Test,
    ),
    Compile / resourceGenerators += Def.task {
      val src = (ThisBuild / baseDirectory).value / "docs" / "user-guide"
      val dst = (Compile / resourceManaged).value / "user-guide"
      IO.delete(dst)
      if (src.exists) IO.copyDirectory(src, dst)
      (dst ** "*.md").get
    }.taskValue,
    fork := true,
    javaHome := {
      val j25 = file("/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home")
      val j21 = file("/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home")
      val j17 = file("/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home")
      if (j25.exists()) Some(j25)
      else if (j21.exists()) Some(j21)
      else if (j17.exists()) Some(j17)
      else sys.env.get("JAVA_HOME").map(file(_))
    },
    javaOptions ++= {
      if (sys.props("os.name").toLowerCase.contains("mac")) {
        val iconPath = (ThisBuild / baseDirectory).value / "packaging" / "icons" / "sangeet-icon-256.png"
        Seq("-Xms512m", "-Xmx2g",
            "-Xdock:name=Sangeet Notes Editor",
            s"-Xdock:icon=${iconPath.getAbsolutePath}",
            "-Dapple.awt.application.name=Sangeet Notes Editor")
      } else Seq("-Xms512m", "-Xmx2g")
    },
    Compile / mainClass := Some("com.varpas.sangeet.desktop.MainApp"),
    assembly / mainClass := Some("com.varpas.sangeet.desktop.MainApp"),
    assembly / assemblyJarName := "sangeet-notes-editor.jar",
    assembly / assemblyMergeStrategy := {
      case x if x.endsWith(".dll")              => MergeStrategy.discard
      case x if x.endsWith(".so")               => MergeStrategy.discard
      case PathList("META-INF", "versions", _*) => MergeStrategy.first
      case PathList("META-INF", "MANIFEST.MF")  => MergeStrategy.discard
      case PathList("META-INF", "services", _*) => MergeStrategy.concat
      case PathList("META-INF", _*)             => MergeStrategy.first
      case "module-info.class"                  => MergeStrategy.discard
      case x if x.endsWith(".class")            => MergeStrategy.first
      case x                                    => MergeStrategy.first
    },
  )
