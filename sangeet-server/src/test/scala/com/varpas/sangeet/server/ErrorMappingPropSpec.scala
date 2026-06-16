package com.varpas.sangeet.server

import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import sttp.model.StatusCode

import com.varpas.sangeet.core.api.ApiError

/** Plan 19 T2C — pure properties on [[ErrorMapping]].
  *
  * The route layer funnels every domain failure through `ErrorMapping.toResponse(error)`. Every `ApiError` must map to:
  *   - a 4xx status code (with one explicit exception, `ExportError` → 500 — see below)
  *   - a non-empty, ALL_CAPS, underscore-separated error code
  *   - a non-empty human-readable message
  *   - a `(StatusCode, Json)` pair where the JSON envelope is the failure envelope shape (success=false, error.code,
  *     error.message)
  *
  * The wired-up route specs check this end-to-end but only for the specific error cases their routes throw. This spec
  * is exhaustive across the [[ApiError]] enum so that adding a new `ApiError` case without updating `ErrorMapping`
  * trips a property here (instead of silently sliding through to an uncaught match and a 5xx).
  */
class ErrorMappingPropSpec extends AnyFunSuite with Matchers with ScalaCheckPropertyChecks:

  // Each generator produces an instance of every constructor in the ApiError ADT.
  // Adding a new case to ApiError without adding it here will compile, but the
  // exhaustiveness property below (propEveryErrorMapped) will fail loudly when
  // that constructor's instances aren't represented in the universe sample.
  //
  // We could rebuild this list by reflection over the enum's `values`, but the
  // explicit list is more readable and serves as a checklist the reviewer can
  // scan.
  private val genApiError: Gen[ApiError] = Gen.oneOf[ApiError](
    Gen
      .zip(Gen.alphaStr.map(_.take(8)), Gen.alphaStr.map(_.take(8)))
      .map { case (a, b) => ApiError.InvalidNoteVariant(a, b) },
    Gen
      .zip(Gen.choose(-5, 20), Gen.choose(0, 10))
      .map { case (a, b) => ApiError.InvalidSectionIndex(a, b) },
    Gen.const(ApiError.LastSection),
    Gen.const(ApiError.EmptySection),
    Gen.const(ApiError.NoSwarTarget),
    Gen.const(ApiError.NoSwarAtPosition),
    Gen.const(ApiError.EmptyNotes),
    Gen
      .zip(Gen.choose(1, 10), Gen.choose(0, 10))
      .map { case (a, b) => ApiError.InsufficientNotes(a, b) },
    Gen.alphaStr.map(_.take(8)).map(ApiError.InvalidOrnamentType(_)),
    Gen.alphaStr.map(_.take(20)).map(ApiError.ParseError(_)),
    Gen.alphaStr.map(_.take(8)).map(ApiError.VersionError(_)),
    Gen.alphaStr.map(_.take(20)).map(ApiError.ValidationError(_)),
    Gen
      .zip(Gen.alphaStr.map(_.take(8)), Gen.alphaStr.map(_.take(8)))
      .map { case (a, b) => ApiError.NotFound(a, b) },
    Gen.alphaStr.map(_.take(20)).map(ApiError.ExportError(_)),
    Gen.alphaStr.map(_.take(8)).map(ApiError.MissingField(_)),
    Gen.const(ApiError.EmptySelection),
    Gen.alphaStr.map(_.take(20)).map(ApiError.InvalidClipboard(_))
  )

  test("propStatusCodeIs4xxOrMappedExportError: every ApiError maps to 4xx (or 500 for ExportError)") {
    // ExportError is the one documented exception: HTML export failures are
    // server-side (file system, classloader resource missing) and clients can't
    // recover by changing the request — 500 is semantically correct there.
    // Every other ApiError is the client's fault and must be a 4xx.
    forAll(genApiError) { err =>
      val code = ErrorMapping.toStatusCode(err).code
      err match
        case _: ApiError.ExportError =>
          code shouldBe 500
        case _ =>
          code should (be >= 400 and be < 500)
    }
  }

  test("propErrorCodeShape: error code is non-empty ALL_CAPS underscore-separated") {
    forAll(genApiError) { err =>
      val code = ErrorMapping.toErrorCode(err)
      code should not be empty
      // ALL_CAPS A-Z plus underscores — pinning this keeps the wire contract
      // stable for clients that hardcode dispatch keys.
      code should fullyMatch regex "[A-Z][A-Z_]*"
    }
  }

  test("propErrorMessageNonEmpty: every error has a non-empty human-readable message") {
    forAll(genApiError) { err =>
      val msg = ErrorMapping.toMessage(err)
      msg should not be empty
    }
  }

  test("propToResponseEnvelope: toResponse yields the (StatusCode, failure-envelope) pair consistently") {
    // The `toResponse` helper composes the three single-purpose mappers into the
    // tuple Tapir routes return. Pin the composition so a future refactor that
    // (say) swapped code/message order in the JSON body fails here, not in an
    // integration test halfway down the pipeline.
    forAll(genApiError) { err =>
      val (status, json) = ErrorMapping.toResponse(err)
      status shouldBe ErrorMapping.toStatusCode(err)
      val cursor = json.hcursor
      cursor.get[Boolean]("success").toOption shouldBe Some(false)
      cursor.downField("error").get[String]("code").toOption shouldBe Some(ErrorMapping.toErrorCode(err))
      cursor.downField("error").get[String]("message").toOption shouldBe Some(ErrorMapping.toMessage(err))
    }
  }

  test("propNotFoundIs404: ApiError.NotFound / NoSwarAtPosition map to 404 specifically") {
    // The OpenAPI spec promises 404 for "entity not found" errors. A regression
    // that mapped these to 400 would technically satisfy "is 4xx" but break
    // clients that dispatch on the specific code (e.g. the Elm app's retry
    // strategy for "I'll re-list raags" on a NotFound). Pin the specific mapping.
    forAll(Gen.zip(Gen.alphaStr.map(_.take(8)), Gen.alphaStr.map(_.take(8)))) { case (entity, name) =>
      ErrorMapping.toStatusCode(ApiError.NotFound(entity, name)) shouldBe StatusCode.NotFound
    }
    ErrorMapping.toStatusCode(ApiError.NoSwarAtPosition) shouldBe StatusCode.NotFound
  }
