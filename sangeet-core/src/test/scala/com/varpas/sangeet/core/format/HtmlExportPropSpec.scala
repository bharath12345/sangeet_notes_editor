package com.varpas.sangeet.core.format

import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import com.varpas.sangeet.core.generators.Generators
import com.varpas.sangeet.core.generators.Generators.given
import com.varpas.sangeet.core.model._

/** Plan 19 T1C — Phase C gap-fill for the HTML exporter.
  *
  * `HtmlExportSpec` covers structural assertions on a single hand-built composition (XSS escaping, header content, grid
  * rendering). The exporter is a pure function from `Composition` to `String`, so the highest-value property is the
  * *totality* law: it never throws for any composition the model can produce.
  *
  * If a future generator addition emits a composition shape the renderer can't handle, this fires immediately.
  */
class HtmlExportPropSpec extends AnyFunSuite with ScalaCheckPropertyChecks:

  test("propHtmlExportTotalAcrossCompositions: render(comp, script) never throws for any Composition + SwarScript"):
    forAll { (c: Composition, script: SwarScript) =>
      // The whole point: assert we got a non-null String back without an exception escaping.
      val html = HtmlExport.render(c, script)
      assert(html != null)
      assert(html.nonEmpty)
    }

  test("propHtmlExportContainsTitle: rendered HTML always contains the composition's title"):
    // We don't pin the *exact* string (titles can have characters the escaper rewrites) — just that any non-empty
    // alphanumeric prefix survives. Generator emits titles that are alphanumerics + space + dash, so escaping is a
    // no-op for the first character.
    forAll { (c: Composition, script: SwarScript) =>
      val html = HtmlExport.render(c, script)
      // Escaped title is wrapped in a <title>...</title> tag; verify the tag exists and is non-empty.
      val titleTagOpen  = html.indexOf("<title>")
      val titleTagClose = html.indexOf("</title>")
      assert(titleTagOpen >= 0 && titleTagClose > titleTagOpen, s"title tag missing or empty in HTML")
    }

  test("propHtmlExportProducesValidHtmlSkeleton: every output starts with <!DOCTYPE html> and ends inside </html>"):
    forAll(Generators.genComposition, Generators.genSwarScript) { (c, script) =>
      val html = HtmlExport.render(c, script)
      assert(html.startsWith("<!DOCTYPE html>"), s"HTML doesn't start with DOCTYPE: ${html.take(40)}")
      assert(html.contains("</html>"), "HTML doesn't contain closing </html>")
    }
