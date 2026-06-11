package com.varpas.sangeet.server.bugreports

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.charset.StandardCharsets
import java.time.Duration

import cats.effect.IO
import io.circe.Json

/** Best-effort creator of GitHub issues for incoming bug reports. The trait exists so that tests can record calls
  * without hitting api.github.com. Production wires the real impl from env (`GITHUB_REPO`, `GITHUB_TOKEN`); when either
  * is missing, `fromEnv` returns a no-op so local dev and tests don't have to set anything up.
  */
trait GitHubIssuesClient:

  /** Create an issue and return its `html_url` on success, or a short diagnostic on failure. Never throws — failure to
    * file an issue must not affect the user-facing response (the GCS write is the source of truth).
    */
  def createIssue(title: String, body: String, labels: List[String]): IO[Either[String, String]]

object GitHubIssuesClient:

  /** Reads `GITHUB_REPO` (e.g. `bharath12345/sangeet_notes_editor`) and `GITHUB_TOKEN`. Both must be set for the real
    * client to be wired; otherwise returns a disabled no-op.
    */
  def fromEnv: GitHubIssuesClient =
    (sys.env.get("GITHUB_REPO"), sys.env.get("GITHUB_TOKEN")) match
      case (Some(repo), Some(token)) if repo.nonEmpty && token.nonEmpty =>
        new HttpGitHubIssuesClient(repo, token)
      case _ =>
        DisabledGitHubIssuesClient

/** No-op used when GitHub integration isn't configured. Returns `Left` so the caller can log the reason rather than
  * silently dropping the call.
  */
object DisabledGitHubIssuesClient extends GitHubIssuesClient:
  def createIssue(title: String, body: String, labels: List[String]): IO[Either[String, String]] =
    IO.pure(Left("GitHub integration not configured (set GITHUB_REPO and GITHUB_TOKEN)"))

/** Real impl. Uses JDK 11's `java.net.http.HttpClient` to avoid pulling in another dep just for one POST per bug
  * report. Synchronous send wrapped in `IO.blocking` — bug-report volume is at most single-digit per day; no async
  * machinery needed.
  */
final class HttpGitHubIssuesClient(repo: String, token: String) extends GitHubIssuesClient:

  private val client: HttpClient = HttpClient
    .newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .build()

  def createIssue(title: String, body: String, labels: List[String]): IO[Either[String, String]] =
    val issueJson = Json
      .obj(
        "title"  -> Json.fromString(title),
        "body"   -> Json.fromString(body),
        "labels" -> Json.fromValues(labels.map(Json.fromString))
      )
      .noSpaces

    val request = HttpRequest
      .newBuilder()
      .uri(URI.create(s"https://api.github.com/repos/$repo/issues"))
      .timeout(Duration.ofSeconds(15))
      .header("Accept", "application/vnd.github+json")
      .header("Authorization", s"Bearer $token")
      .header("X-GitHub-Api-Version", "2022-11-28")
      .header("Content-Type", "application/json")
      .header("User-Agent", "sangeet-server-bug-reporter")
      .POST(HttpRequest.BodyPublishers.ofString(issueJson, StandardCharsets.UTF_8))
      .build()

    IO.blocking(client.send(request, HttpResponse.BodyHandlers.ofString())).attempt.map {
      case Right(resp) if resp.statusCode() == 201 =>
        io.circe.parser
          .parse(resp.body())
          .flatMap(_.hcursor.get[String]("html_url"))
          .left
          .map(err => s"GitHub returned 201 but body did not parse: ${err.getMessage}")
      case Right(resp) =>
        val snippet = Option(resp.body()).getOrElse("").take(300)
        Left(s"GitHub API returned ${resp.statusCode()}: $snippet")
      case Left(t) =>
        Left(s"GitHub API call threw ${t.getClass.getSimpleName}: ${t.getMessage}")
    }
