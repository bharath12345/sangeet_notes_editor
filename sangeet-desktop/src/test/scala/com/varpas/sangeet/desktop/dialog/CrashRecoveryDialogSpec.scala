package com.varpas.sangeet.desktop.dialog

import io.circe.Json
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/** Targeted regression tests for [[CrashRecoveryDialog.buildPayload]] added in PR-3d (E4).
  *
  * The dialog itself can't be exercised headlessly without spinning up JavaFX, but [[buildPayload]] is the pure `Json
  * \=> BugReportPayload` conversion that used to call `.toOption.getOrElse(default)` on every field. The fix preserves
  * the user-visible default behaviour but now logs missing/malformed fields. These tests pin the default values (so a
  * future regression that breaks them is caught) and the malformed-sentinel resilience.
  */
class CrashRecoveryDialogSpec extends AnyFlatSpec with Matchers:

  "buildPayload" should "fall back to defaults when the sentinel is missing every field" in {
    val emptyCrash = Json.obj()
    val payload    = CrashRecoveryDialog.buildPayload(emptyCrash, None, None)

    payload.description should include("[CRASH] unknown:")
    payload.description should include("Thread: unknown")
    payload.eventLog shouldBe empty
    payload.email shouldBe None
    payload.composition shouldBe None
  }

  it should "extract all sentinel fields when present" in {
    val crash = Json.obj(
      "exception"   -> Json.fromString("java.lang.RuntimeException"),
      "message"     -> Json.fromString("boom"),
      "stackTrace"  -> Json.fromString("at Foo.bar(Foo.scala:42)"),
      "threadName"  -> Json.fromString("JavaFX Application Thread"),
      "eventLogger" -> Json.arr(Json.obj("kind" -> Json.fromString("Key")))
    )

    val payload = CrashRecoveryDialog.buildPayload(crash, Some("the user note"), Some("user@example.com"))

    payload.description should include("the user note")
    payload.description should include("[CRASH] java.lang.RuntimeException: boom")
    payload.description should include("Thread: JavaFX Application Thread")
    payload.description should include("at Foo.bar(Foo.scala:42)")
    payload.eventLog should have size 1
    payload.email shouldBe Some("user@example.com")
  }

  it should "tolerate a sentinel where eventLogger is the wrong type (E4 regression)" in {
    // Before PR-3d, this used .getOrElse(List.empty) silently; now we still
    // recover but the log call documents the schema drift.
    val malformed = Json.obj(
      "exception"   -> Json.fromString("Foo"),
      "eventLogger" -> Json.fromString("not-a-list")
    )
    val payload = CrashRecoveryDialog.buildPayload(malformed, None, None)
    payload.eventLog shouldBe empty
    payload.description should include("[CRASH] Foo:")
  }

  it should "tolerate a sentinel where a field has the wrong type (E4 regression)" in {
    val malformed = Json.obj(
      "exception"  -> Json.fromInt(42),      // wrong type
      "message"    -> Json.arr(),            // wrong type
      "threadName" -> Json.fromBoolean(true) // wrong type
    )
    val payload = CrashRecoveryDialog.buildPayload(malformed, None, None)
    // Defaults preserved so the recovery dialog isn't dead-on-arrival; the
    // log warnings (asserted indirectly via "no exception thrown") are the
    // diagnostic trail.
    payload.description should include("[CRASH] unknown:")
    payload.description should include("Thread: unknown")
  }
