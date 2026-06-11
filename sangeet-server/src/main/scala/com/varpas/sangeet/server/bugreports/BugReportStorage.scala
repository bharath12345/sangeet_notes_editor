package com.varpas.sangeet.server.bugreports

import java.nio.charset.StandardCharsets

import cats.effect.IO
import com.google.cloud.storage.{BlobId, BlobInfo, StorageOptions}
import io.circe.Json

/** Persistence for bug-report payloads. Trait so tests can swap in a fake without spinning up real GCS. The single
  * concrete production impl writes to a GCS bucket via Application Default Credentials (auto-resolved on Cloud Run via
  * the metadata server).
  */
trait BugReportStorage:

  /** Store the body under the given reportId. Returns `Right(())` on success or `Left(message)` on any failure (storage
    * not configured, network error, permission denied, etc.). Never throws.
    */
  def store(reportId: String, body: Json): IO[Either[String, Unit]]

object BugReportStorage:

  /** Pick the impl based on env vars. Set `BUG_REPORTS_BUCKET=sangeet-bug-reports` in Cloud Run to enable real GCS
    * writes. Locally with the var unset, the endpoint returns `Left(...)` rather than silently dropping the payload —
    * clearer signal when wiring things up.
    */
  def fromEnv: BugReportStorage =
    sys.env.get("BUG_REPORTS_BUCKET") match
      case Some(bucket) => GcsBugReportStorage(bucket)
      case None         => UnconfiguredBugReportStorage

/** Always-fails impl used when no bucket is configured. */
object UnconfiguredBugReportStorage extends BugReportStorage:
  def store(reportId: String, body: Json): IO[Either[String, Unit]] =
    IO.pure(Left("Bug-report storage not configured (set BUG_REPORTS_BUCKET env var)"))

/** Real impl backed by GCS. The `Storage` client is initialized lazily so that the absence of ADC during compile/test
  * doesn't fail at class-load time.
  */
final class GcsBugReportStorage(bucket: String) extends BugReportStorage:

  private lazy val storage = StorageOptions.getDefaultInstance.getService

  def store(reportId: String, body: Json): IO[Either[String, Unit]] =
    IO.blocking {
      val blobInfo = BlobInfo
        .newBuilder(BlobId.of(bucket, s"$reportId.json"))
        .setContentType("application/json")
        .build()
      val bytes = body.noSpaces.getBytes(StandardCharsets.UTF_8)
      storage.create(blobInfo, bytes)
      ()
    }.attempt
      .map {
        case Right(_) => Right(())
        case Left(t)  => Left(s"GCS write failed: ${t.getClass.getSimpleName}: ${t.getMessage}")
      }
