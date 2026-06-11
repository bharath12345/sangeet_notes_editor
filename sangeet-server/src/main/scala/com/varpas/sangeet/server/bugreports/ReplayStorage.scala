package com.varpas.sangeet.server.bugreports

import cats.effect.IO
import com.google.cloud.storage.{BlobId, StorageOptions}

/** Reads bug-report payloads back out of GCS for the replay viewer (Phase 6). Mirror of [[BugReportStorage]] for the
  * write side; reuses the same bucket (env var `BUG_REPORTS_BUCKET`) — the viewer is just reading what the endpoint
  * wrote.
  */
trait ReplayStorage:

  /** Fetch the payload for the given reportId. Returns the raw JSON bytes (so the endpoint can stream them straight out
    * without re-encoding). `Left` distinguishes "missing" from "couldn't reach storage", which lets the route return
    * distinct status codes.
    */
  def get(reportId: String): IO[Either[ReplayStorage.Error, Array[Byte]]]

object ReplayStorage:

  /** Distinct error cases so the HTTP layer can map them to status codes:
    *   - NotFound → 404
    *   - NotConfigured → 503 (env var missing — diagnostic, not user-facing)
    *   - ReadFailed → 502 (we reached GCS but it errored)
    */
  enum Error:
    case NotFound
    case NotConfigured
    case ReadFailed(message: String)

  def fromEnv: ReplayStorage =
    sys.env.get("BUG_REPORTS_BUCKET") match
      case Some(bucket) => GcsReplayStorage(bucket)
      case None         => UnconfiguredReplayStorage

object UnconfiguredReplayStorage extends ReplayStorage:
  def get(reportId: String): IO[Either[ReplayStorage.Error, Array[Byte]]] =
    IO.pure(Left(ReplayStorage.Error.NotConfigured))

/** Real impl backed by GCS. Storage client is `lazy val` so absence of ADC at class-load (e.g., during tests) doesn't
  * fail. We use `storage.get(blob)` rather than `readAllBytes` so a missing object is a `null` return rather than a
  * thrown `StorageException` — cleaner branching for the 404 case.
  */
final class GcsReplayStorage(bucket: String) extends ReplayStorage:

  private lazy val storage = StorageOptions.getDefaultInstance.getService

  def get(reportId: String): IO[Either[ReplayStorage.Error, Array[Byte]]] =
    IO.blocking {
      val blob = storage.get(BlobId.of(bucket, s"$reportId.json"))
      if blob == null then Left(ReplayStorage.Error.NotFound)
      else Right(blob.getContent())
    }.attempt
      .map {
        case Right(result) => result
        case Left(t) =>
          Left(ReplayStorage.Error.ReadFailed(s"${t.getClass.getSimpleName}: ${t.getMessage}"))
      }
