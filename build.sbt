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
ThisBuild / coverageExcludedPackages := "com\\.varpas\\.sangeet\\.desktop\\..*;com\\.varpas\\.sangeet\\.core\\.codegen\\..*;com\\.varpas\\.sangeet\\.server\\.codegen\\..*"

lazy val root = project
  .in(file("."))
  .aggregate(sangeetCore, sangeetDesktop, sangeetServer)
  .settings(
    name := "sangeet-notes-editor",
    // Root project does not compile source directly
    Compile / sources := Seq.empty,
    Test / sources := Seq.empty,
  )

lazy val genUiStrings = taskKey[Seq[File]]("Generate UiStrings.scala from ui-strings.json")

lazy val sangeetCore = project
  .in(file("sangeet-core"))
  .settings(
    name := "sangeet-core",
    libraryDependencies ++= Seq(
      "io.circe"          %% "circe-core"    % "0.14.15",
      "io.circe"          %% "circe-parser"  % "0.14.15",
      "io.circe"          %% "circe-generic" % "0.14.15",
      "org.scalatest"     %% "scalatest"     % "3.2.18" % Test,
      // Plan 19 T1A: ScalaCheck integration for ScalaTest. The scalatestplus
      // artifact pulls in a compatible scalacheck (1.17.x). See
      // docs/developer/testing/property-based-testing.md.
      "org.scalatestplus" %% "scalacheck-1-17" % "3.2.18.0" % Test,
    ),
    genUiStrings := UiStringsCodegen.run(
      catalog = (Compile / resourceDirectory).value / "ui-strings.json",
      output  = baseDirectory.value / "src" / "main" / "scala" / "com" / "varpas" / "sangeet" / "core" / "strings" / "UiStrings.scala"
    ),
    fork := true,
  )

val tapirVersion = "1.10.0"
val http4sVersion = "0.23.27"
val catsEffectVersion = "3.5.4"
val micrometerVersion = "1.13.0"

lazy val sangeetServer = project
  .in(file("sangeet-server"))
  // Plan 19 T2B: depend on sangeet-core's test sources too so server property
  // specs can import `com.varpas.sangeet.core.generators.Generators` rather
  // than copy-pasting domain generators. See
  // docs/developer/testing/property-based-testing.md — "Generators are shared
  // by reference within a language; never copy-pasted across modules."
  .dependsOn(sangeetCore % "compile->compile;test->test")
  .settings(
    name := "sangeet-server",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-core"              % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"        % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"     % tapirVersion,
      // PR-1c (plan 18): export Tapir endpoints to OpenAPI YAML for
      // docs/developer/specs/openapi.yaml. tapir-openapi-docs walks the
      // endpoint ADT into the apispec model; openapi-circe-yaml renders
      // it as YAML. 0.8.0 matches the apispec model pulled in transitively
      // by tapir 1.10.0 — bumping in lockstep avoids the duplicate-class
      // hazard from two apispec-model jars on the classpath.
      "com.softwaremill.sttp.tapir"   %% "tapir-openapi-docs"  % tapirVersion,
      "com.softwaremill.sttp.apispec" %% "openapi-circe-yaml"  % "0.8.0",
      "org.http4s"                  %% "http4s-ember-server"      % http4sVersion,
      // Used for the /replay viewer routes (Phase 6) — DSL for inline matchers,
      // `StaticFile.fromResource` for serving the replay.html player from the
      // assembly JAR's classpath, and circe encoders for inline JSON error
      // bodies returned outside the Tapir layer.
      "org.http4s"                  %% "http4s-dsl"               % http4sVersion,
      "org.http4s"                  %% "http4s-circe"             % http4sVersion,
      "org.typelevel"               %% "cats-effect"              % catsEffectVersion,
      "io.circe"                    %% "circe-core"               % "0.14.15",
      "io.circe"                    %% "circe-generic"            % "0.14.15",
      // Observability: Prometheus scrape format for local debugging + push to
      // GCP Cloud Monitoring in production (via Micrometer's Stackdriver
      // registry, which auto-authenticates via Application Default Credentials
      // on Cloud Run). JVM/process bindings come bundled with micrometer-core.
      // See docs/developer/plans/plan-12-*.md Phase 1.
      "io.micrometer"               %  "micrometer-core"             % micrometerVersion,
      "io.micrometer"               %  "micrometer-registry-prometheus" % micrometerVersion,
      "io.micrometer"               %  "micrometer-registry-stackdriver" % micrometerVersion,
      // Bug-report endpoint writes payloads to GCS. Authenticates via ADC on
      // Cloud Run (metadata-server-issued tokens); a no-op locally unless
      // GOOGLE_APPLICATION_CREDENTIALS is set.
      "com.google.cloud"            %  "google-cloud-storage"        % "2.73.0",
      // SLF4J binding so we can actually see the Stackdriver registry's log
      // output. Without an impl, all SLF4J calls go to no-op and Micrometer's
      // push errors are silently swallowed. slf4j-simple writes to stderr
      // with no config; INFO level by default catches push failures.
      "org.slf4j"                   %  "slf4j-simple"                % "2.0.13",
      "org.scalatest"               %% "scalatest"                % "3.2.18" % Test,
      // Plan 19 T1A: ScalaCheck integration (unused in server today; wired so
      // Tier 2 Phase A can import generators from sangeet-core test sources).
      "org.scalatestplus"           %% "scalacheck-1-17"          % "3.2.18.0" % Test,
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
      // Signed-JAR signature files from upstream deps (e.g. Google Cloud client
      // libraries) become invalid once we merge classes from other jars in,
      // and the JVM then refuses to load the fat jar. Discard them — assembled
      // jars are unsigned by design.
      case PathList("META-INF", name)
          if name.endsWith(".SF") || name.endsWith(".DSA") || name.endsWith(".RSA") ||
            name.endsWith(".EC")                                          => MergeStrategy.discard
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
          ExclusionRule(organization = "org.openjfx", name = "javafx-fxml"),
        ),
      // javafx-swing was previously excluded above to keep the bundle smaller;
      // re-included for Phase 8 because SwingFXUtils is the standard path
      // from WritableImage → BufferedImage → PNG bytes used in the bug-report
      // screenshot capture. Adds ~50 KB.
      "org.openjfx" % "javafx-swing" % "21.0.7",
      "com.vladsch.flexmark" % "flexmark-all" % "0.64.8",
      "org.kordamp.ikonli"   % "ikonli-javafx" % "12.4.0",
      "org.kordamp.ikonli"   % "ikonli-materialdesign2-pack" % "12.4.0",
      // Phase 10: PostHog Java SDK for anonymous desktop usage metrics. The
      // client is constructed only when SANGEET_POSTHOG_API_KEY is present
      // (and SANGEET_ANALYTICS_DISABLED is unset); otherwise a no-op.
      "com.posthog"          % "posthog-server" % "2.7.0",
      "org.scalatest" %% "scalatest" % "3.2.18" % Test,
      // Plan 19 T1A: ScalaCheck integration (unused in desktop today; wired so
      // Tier 3 Phase A can import generators from sangeet-core test sources).
      "org.scalatestplus" %% "scalacheck-1-17" % "3.2.18.0" % Test,
    ),
    Compile / resourceGenerators += Def.task {
      val src = (ThisBuild / baseDirectory).value / "docs" / "user-guide"
      val dst = (Compile / resourceManaged).value / "user-guide"
      IO.delete(dst)
      if (src.exists) IO.copyDirectory(src, dst)
      (dst ** "*.md").get
    }.taskValue,
    // Phase 10: bake SANGEET_POSTHOG_API_KEY into a classpath resource at
    // build time so packaged releases (.dmg/.msi) ship with the project
    // key without requiring end users to set an env var. The env var still
    // overrides at runtime for dev/CI. Empty file when the env var is
    // unset at build time — runtime falls back to noop.
    Compile / resourceGenerators += Def.task {
      val key = sys.env.getOrElse("SANGEET_POSTHOG_API_KEY", "")
      val dst = (Compile / resourceManaged).value / "posthog.properties"
      IO.write(dst, s"apiKey=$key\n")
      Seq(dst)
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

// PR-1c (plan 18): spec exporters wired as top-level sbt tasks so `make
// gen-specs` can drive them, and so the `check-specs` CI job can call them
// without poking inside the module aliases. Each delegates to the right
// sub-project's runMain, passing the repo-root path as a single positional
// argument. Forked JVMs inherit the per-module baseDirectory as cwd, so we
// resolve the spec path against the build root explicitly.
lazy val generateOpenApi    = taskKey[Unit]("Export OpenAPI YAML from Tapir endpoints")
lazy val generateSwarSchema = taskKey[Unit]("Export JSON Schema for .swar composition files")

generateOpenApi := Def.taskDyn {
  val out = ((ThisBuild / baseDirectory).value / "docs" / "developer" / "specs" / "openapi.yaml").getAbsolutePath
  (sangeetServer / Compile / runMain).toTask(s" com.varpas.sangeet.server.codegen.OpenApiExporter $out")
}.value

generateSwarSchema := Def.taskDyn {
  val out = ((ThisBuild / baseDirectory).value / "docs" / "developer" / "specs" / "swar.schema.json").getAbsolutePath
  (sangeetCore / Compile / runMain).toTask(s" com.varpas.sangeet.core.codegen.SwarSchemaExporter $out")
}.value
