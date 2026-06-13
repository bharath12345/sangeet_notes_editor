package com.varpas.sangeet.core.strings

import org.scalatest.funsuite.AnyFunSuite

class UiStringsCodegenSpec extends AnyFunSuite:

  test("emits compile-ready Scala header"):
    val out = UiStringsCodegen.emitScala("""{"entries":{}}""")
    assert(out.contains("package com.varpas.sangeet.core.strings"))
    assert(out.contains("object UiStrings:"))
    assert(out.contains("GENERATED FILE"))

  test("emits typed val constant for 'value' entry"):
    val json = """{"entries":{"toolbar.file.new":{"value":"New","platform":"both","description":""}}}"""
    val out  = UiStringsCodegen.emitScala(json)
    assert(out.contains("""val toolbarFileNew: String = "New""""))

  test("emits typed def function for parameterized entry"):
    val json = """
      {"entries":{"toolbar.beatCount":{
        "template":"Beats: {current} / {total}",
        "params":[{"name":"current","type":"int"},{"name":"total","type":"int"}],
        "platform":"both","description":""
      }}}
    """
    val out  = UiStringsCodegen.emitScala(json)
    assert(out.contains("def toolbarBeatCount(current: Int, total: Int): String"))
    assert(out.contains("""s"Beats: $current / $total""""))

  test("escapes double quotes and backslashes in values"):
    val json = """{"entries":{"k":{"value":"\"quoted\" \\ back","platform":"both","description":""}}}"""
    val out  = UiStringsCodegen.emitScala(json)
    assert(out.contains("""val k: String = "\"quoted\" \\ back""""))

  test("sorts entries deterministically"):
    val json =
      """{"entries":{"z":{"value":"Z","platform":"both","description":""},"a":{"value":"A","platform":"both","description":""}}}"""
    val out = UiStringsCodegen.emitScala(json)
    assert(out.indexOf("val a:") < out.indexOf("val z:"))
