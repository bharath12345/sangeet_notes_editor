package com.varpas.sangeet.core.config

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.UUID

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DistinctIdStoreSpec extends AnyFlatSpec with Matchers:

  private def tempPath(): Path =
    val dir = Files.createTempDirectory("distinct-id-spec-")
    dir.toFile.deleteOnExit()
    dir.resolve("distinct_id")

  "DistinctIdStore.loadOrCreate" should "generate + persist a UUID when the file is missing" in {
    val path = tempPath()
    Files.exists(path) shouldBe false

    val id = DistinctIdStore.loadOrCreate(path)
    noException should be thrownBy UUID.fromString(id)
    Files.exists(path) shouldBe true
    Files.readString(path, StandardCharsets.UTF_8).trim shouldBe id
  }

  it should "return the same UUID on a subsequent call (idempotent)" in {
    val path = tempPath()
    val a    = DistinctIdStore.loadOrCreate(path)
    val b    = DistinctIdStore.loadOrCreate(path)
    a shouldBe b
  }

  it should "tolerate trailing whitespace / newlines in the stored file" in {
    val path = tempPath()
    val good = UUID.randomUUID().toString
    Files.writeString(path, s"$good   \n\n", StandardCharsets.UTF_8)

    DistinctIdStore.loadOrCreate(path) shouldBe good
  }

  it should "regenerate a fresh UUID when the file is malformed" in {
    val path = tempPath()
    Files.writeString(path, "not-a-uuid", StandardCharsets.UTF_8)

    val id = DistinctIdStore.loadOrCreate(path)
    noException should be thrownBy UUID.fromString(id)
    id should not be "not-a-uuid"
    // And the file should now hold the fresh UUID.
    Files.readString(path, StandardCharsets.UTF_8).trim shouldBe id
  }

  it should "regenerate when the file is empty" in {
    val path = tempPath()
    Files.writeString(path, "", StandardCharsets.UTF_8)
    val id = DistinctIdStore.loadOrCreate(path)
    noException should be thrownBy UUID.fromString(id)
  }

  it should "return a valid UUID without throwing when the parent dir cannot be created" in {
    // /proc on Linux is OS-specific; use a path under a file (not a dir) which can never be a parent.
    val nonExistentParent = Files.createTempFile("not-a-dir-", ".txt")
    nonExistentParent.toFile.deleteOnExit()
    val unwritable = nonExistentParent.resolve("subdir").resolve("distinct_id")

    val id = DistinctIdStore.loadOrCreate(unwritable)
    noException should be thrownBy UUID.fromString(id)
    // Persistence may have failed silently — that's the contract. Just don't throw.
  }
