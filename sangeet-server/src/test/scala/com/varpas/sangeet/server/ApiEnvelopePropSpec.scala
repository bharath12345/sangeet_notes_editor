package com.varpas.sangeet.server

import io.circe.parser._
import io.circe.{Decoder, Encoder, Json}
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/** Plan 19 T2C — pure properties on the [[ApiEnvelope]] helper.
  *
  * Every 2xx response in the API is produced by `ApiEnvelope.success` / `successRaw`, and every 4xx by
  * `ApiEnvelope.failure`. The route-level spec families (Reference/Cursor/Composition/Editor/BugReport) check that
  * specific endpoints produce the envelope; this spec checks the envelope itself round-trips and is mutually exclusive
  * with the failure envelope.
  *
  * Why a separate spec for the helper: a regression in `ApiEnvelope` would silently change the wire format every client
  * consumes. Per-endpoint properties would all break in lockstep, making the root cause hard to spot. A focused
  * envelope-shape spec catches that one bug in one place with one stable assertion.
  *
  * These properties are pure (no HTTP) — they exercise the encoder/decoder contract directly, so they're fast (no
  * Tapir/http4s plumbing) and complete a CI run in milliseconds.
  */
class ApiEnvelopePropSpec extends AnyFunSuite with Matchers with ScalaCheckPropertyChecks:

  // ─── Generators ──────────────────────────────────────────────────────────────

  /** Arbitrary JSON payloads — exercised as the `data` field of success envelopes. Recursion is bounded at depth 2 so
    * the property runs fast and still covers nested objects + arrays of primitives.
    */
  private def genJson(depth: Int): Gen[Json] =
    val genPrim: Gen[Json] = Gen.frequency(
      3 -> Gen.alphaNumStr.map(s => Json.fromString(s.take(16))),
      3 -> Gen.choose(-1000, 1000).map(Json.fromInt),
      2 -> Gen.oneOf(Json.True, Json.False),
      1 -> Gen.const(Json.Null)
    )
    if depth <= 0 then genPrim
    else
      Gen.frequency(
        4 -> genPrim,
        2 -> Gen.choose(0, 3).flatMap(n => Gen.listOfN(n, genJson(depth - 1)).map(xs => Json.arr(xs*))),
        2 -> Gen
          .choose(0, 3)
          .flatMap(n =>
            Gen
              .listOfN(n, Gen.zip(Gen.alphaStr.map(_.take(8)).suchThat(_.nonEmpty), genJson(depth - 1)))
              .map(kvs => Json.obj(kvs*))
          )
      )

  private val genArbitraryJson: Gen[Json] = genJson(2)

  /** A non-empty alphabetic error code, mirroring [[ErrorMapping.toErrorCode]] outputs (`PARSE_ERROR`, `NOT_FOUND`,
    * ...). The exact spelling doesn't matter for envelope-shape properties.
    */
  private val genErrorCode: Gen[String] =
    Gen.choose(1, 32).flatMap(n => Gen.listOfN(n, Gen.alphaUpperChar)).map(_.mkString)

  /** Error message — any short printable string. Includes spaces and punctuation to cover real-world messages from
    * [[ErrorMapping.toMessage]] (e.g. "Section index 5 out of range (max: 2)").
    */
  private val genErrorMessage: Gen[String] =
    Gen.choose(1, 64).flatMap(n => Gen.listOfN(n, Gen.asciiPrintableChar)).map(_.mkString)

  // ─── Properties ──────────────────────────────────────────────────────────────

  test("propSuccessRawShape: successRaw(data) ALWAYS produces {success:true, data}") {
    forAll(genArbitraryJson) { data =>
      val env    = ApiEnvelope.successRaw(data)
      val cursor = env.hcursor
      cursor.get[Boolean]("success").toOption shouldBe Some(true)
      cursor.downField("data").succeeded shouldBe true
      // And error fields must be absent — mutually exclusive with failure.
      cursor.downField("error").succeeded shouldBe false
      // Echo-back: the data field is byte-stable with what we passed in.
      cursor.downField("data").as[Json].toOption shouldBe Some(data)
    }
  }

  test("propFailureShape: failure(code, msg) ALWAYS produces {success:false, error:{code, message}}") {
    forAll(genErrorCode, genErrorMessage) { (code, msg) =>
      val env    = ApiEnvelope.failure(code, msg)
      val cursor = env.hcursor
      cursor.get[Boolean]("success").toOption shouldBe Some(false)
      val errC = cursor.downField("error")
      errC.succeeded shouldBe true
      errC.get[String]("code").toOption shouldBe Some(code)
      errC.get[String]("message").toOption shouldBe Some(msg)
      // And data must be absent on failure envelopes.
      cursor.downField("data").succeeded shouldBe false
    }
  }

  test("propEnvelopeMutuallyExclusive: success and failure envelopes never share a top-level shape") {
    // Strong invariant: for ANY (data, code, msg) triple, the success envelope's
    // `success` field is the negation of the failure envelope's. Clients that
    // dispatch on this one field must never see both `true` and `error` set or
    // both `false` and `data` set.
    forAll(genArbitraryJson, genErrorCode, genErrorMessage) { (d, c, m) =>
      val s   = ApiEnvelope.successRaw(d).hcursor
      val f   = ApiEnvelope.failure(c, m).hcursor
      val sOk = s.get[Boolean]("success").getOrElse(false)
      val fOk = f.get[Boolean]("success").getOrElse(true)
      sOk shouldBe true
      fOk shouldBe false
      // Concretely: a single envelope NEVER has both `data` and `error` set.
      s.downField("error").succeeded shouldBe false
      f.downField("data").succeeded shouldBe false
    }
  }

  test("propSuccessTypedRoundTrip: success(typed) round-trips through Decoder") {
    // The typed `success[A]` overload encodes via the implicit Encoder[A] and the
    // resulting envelope's `data` field must decode back to the same A.
    // Pick strings + ints as representative encoders (both circe-derived).
    given Encoder[String] = io.circe.Encoder.encodeString
    given Decoder[String] = io.circe.Decoder.decodeString
    given Encoder[Int]    = io.circe.Encoder.encodeInt
    given Decoder[Int]    = io.circe.Decoder.decodeInt

    forAll(Gen.alphaNumStr.map(_.take(20))) { s =>
      val env     = ApiEnvelope.success(s)
      val decoded = env.hcursor.downField("data").as[String]
      decoded shouldBe Right(s)
    }
    forAll(Gen.choose(Int.MinValue, Int.MaxValue)) { i =>
      val env     = ApiEnvelope.success(i)
      val decoded = env.hcursor.downField("data").as[Int]
      decoded shouldBe Right(i)
    }
  }

  test("propEnvelopeIsValidJson: every envelope serialises to parseable JSON") {
    // Defence against an encoder that produces malformed JSON for some edge-case
    // payload — every successRaw/failure result must round-trip through circe.
    forAll(genArbitraryJson) { data =>
      val str = ApiEnvelope.successRaw(data).noSpaces
      parse(str).isRight shouldBe true
    }
    forAll(genErrorCode, genErrorMessage) { (c, m) =>
      val str = ApiEnvelope.failure(c, m).noSpaces
      parse(str).isRight shouldBe true
    }
  }
