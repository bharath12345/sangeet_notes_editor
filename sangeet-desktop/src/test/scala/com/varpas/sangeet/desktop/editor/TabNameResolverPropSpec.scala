package com.varpas.sangeet.desktop.editor

import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/** Plan 19, Tier 3 (sangeet-desktop), Phases B+C — minimal scope.
  *
  * Desktop's PBT scope is intentionally small (the plan designates only ~3 files for PBT here): most of
  * `sangeet-desktop` is ScalaFX UI glue or thin fire-and-forget HTTP clients (DesktopMetrics, BugReportClient,
  * PostHogClient) where the value of a property test is dominated by I/O concerns already covered by their existing
  * example-based suites.
  *
  * After T3A's [[AppConfigPropSpec]] (JSON round-trip), the next-best pure-logic target on the desktop side is
  * [[TabNameResolver]] — small, no side effects, with three crisp invariants worth pinning with ScalaCheck:
  *
  *   1. `nextAvailableTitle` always returns a string NOT present in `existing` (uniqueness) 2. `nextAvailableTitle`
  *      never produces a doubled suffix like `"abc (2) (2)"` (regression: the suffix-stripping branch must always run
  *      on already-renamed inputs) 3. `stripParenSuffix` is idempotent — stripping twice yields the same result as
  *      stripping once
  *
  * No further desktop PBT is warranted: TabManager is JavaFX-stateful, SampleComposition is a single literal, and the
  * metrics / diagnostics clients are dominated by HTTP plumbing. T3 ends here.
  */
class TabNameResolverPropSpec extends AnyFunSuite with ScalaCheckPropertyChecks:

  // Keep generated titles tame: nonempty alphanumeric (matches the user-facing
  // shape of real tab labels — "yaman-vilambit-gat", "untitled-1", etc.) so we
  // exercise the resolver's logic rather than hammering it with whitespace or
  // unicode edge cases that aren't part of its contract.
  private val genTitle: Gen[String] =
    Gen.alphaNumStr.suchThat(_.nonEmpty)

  // Bound the existing-titles set so the search inside nextAvailableTitle stays
  // fast; the resolver's correctness doesn't depend on cardinality, so 0–10
  // pre-existing tabs is a representative sample of realistic UI state.
  private val genExisting: Gen[Seq[String]] =
    Gen.choose(0, 10).flatMap(n => Gen.listOfN(n, genTitle))

  test("propUniqueness: nextAvailableTitle returns a title not present in `existing`") {
    forAll(genTitle, genExisting) { (base, existing) =>
      val resolved = TabNameResolver.nextAvailableTitle(base, existing)
      assert(!existing.contains(resolved))
    }
  }

  test("propNoDoubledSuffix: nextAvailableTitle never produces a doubled `(N) (M)` tail") {
    // The contract: re-resolving an already-renamed title must strip the
    // existing suffix before appending a new one. We sample the resolver with
    // a base that ALREADY ends in " (N)" so the strip path is exercised.
    val genBaseN = for
      stem <- genTitle
      n    <- Gen.choose(2, 9)
    yield s"$stem ($n)"
    forAll(genBaseN, genExisting) { (baseWithSuffix, existing) =>
      val resolved = TabNameResolver.nextAvailableTitle(baseWithSuffix, existing)
      // No string ending in " (N) (M)" — i.e. two paren-number tails back-to-back.
      val doubled = """.*\(\d+\)\s*\(\d+\)\s*$""".r
      assert(
        !doubled.matches(resolved),
        s"resolved=$resolved had a doubled suffix"
      )
    }
  }

  test("propStripIdempotent: stripParenSuffix(stripParenSuffix(t)) == stripParenSuffix(t)") {
    // Idempotency: the regex only peels off one trailing `(N)`, so applying it
    // twice should be a no-op on the second pass. Generates both bare titles
    // and titles with a paren-N suffix to cover both branches.
    val genAnyTitle = Gen.oneOf(
      genTitle,
      for
        stem <- genTitle
        n    <- Gen.choose(0, 99)
      yield s"$stem ($n)"
    )
    forAll(genAnyTitle) { title =>
      val once  = TabNameResolver.stripParenSuffix(title)
      val twice = TabNameResolver.stripParenSuffix(once)
      assert(once == twice)
    }
  }
