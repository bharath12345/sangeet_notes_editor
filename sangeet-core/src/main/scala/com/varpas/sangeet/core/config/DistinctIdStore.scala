package com.varpas.sangeet.core.config

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardCopyOption}
import java.util.UUID

/** Persists a stable anonymous UUID for PostHog's `distinct_id`. One UUID per installation, lives at
  * `~/.sangeet/distinct_id` (deliberately a sibling of `~/.sangeet/crash-pending/` from Phase 9 — not inside
  * AppConfig.json, so wiping app settings doesn't reset the identity).
  *
  * The format is just the raw UUID string, no JSON wrapper. Anything that can't be parsed as a UUID is treated as
  * absent — we mint a fresh one and overwrite. If the filesystem won't let us write (read-only home dir, full disk), we
  * still return a UUID so analytics keeps working for the session; it just won't be stable across launches.
  */
object DistinctIdStore:

  /** Default location. Sibling of crash-pending/ so a future cleanup script can sweep both. */
  def defaultPath: Path = Path.of(System.getProperty("user.home"), ".sangeet", "distinct_id")

  /** Read the existing UUID; if missing or malformed, generate + persist a new one. Never throws. */
  def loadOrCreate(path: Path = defaultPath): String =
    readExisting(path).getOrElse {
      val fresh = UUID.randomUUID().toString
      tryWrite(path, fresh)
      fresh
    }

  private def readExisting(path: Path): Option[String] =
    try
      if !Files.isRegularFile(path) then None
      else
        val raw = Files.readString(path, StandardCharsets.UTF_8).trim
        try
          UUID.fromString(raw); Some(raw)
        catch case _: IllegalArgumentException => None
    catch case _: Throwable => None

  private def tryWrite(path: Path, uuid: String): Unit =
    try
      val parent = path.getParent
      if parent != null && !Files.isDirectory(parent) then Files.createDirectories(parent)
      val tmp = path.resolveSibling(path.getFileName.toString + ".tmp")
      Files.writeString(tmp, uuid + "\n", StandardCharsets.UTF_8)
      Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    catch case _: Throwable => ()
