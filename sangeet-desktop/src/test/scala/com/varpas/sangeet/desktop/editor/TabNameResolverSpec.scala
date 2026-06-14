package com.varpas.sangeet.desktop.editor

import org.scalatest.funsuite.AnyFunSuite

class TabNameResolverSpec extends AnyFunSuite:

  test("hasCollision detects exact matches") {
    assert(TabNameResolver.hasCollision("abc", Seq("xyz", "abc", "def")))
    assert(!TabNameResolver.hasCollision("abc", Seq("xyz", "def")))
    assert(!TabNameResolver.hasCollision("abc", Nil))
  }

  test("hasCollision is case-sensitive") {
    assert(!TabNameResolver.hasCollision("abc", Seq("ABC")))
  }

  test("nextAvailableTitle starts at (2) when the base name is the only collision") {
    assert(TabNameResolver.nextAvailableTitle("abc", Seq("abc")) == "abc (2)")
  }

  test("nextAvailableTitle picks the next free N") {
    assert(TabNameResolver.nextAvailableTitle("abc", Seq("abc", "abc (2)")) == "abc (3)")
    assert(TabNameResolver.nextAvailableTitle("abc", Seq("abc", "abc (2)", "abc (3)")) == "abc (4)")
  }

  test("nextAvailableTitle picks the lowest free N, even with gaps") {
    assert(TabNameResolver.nextAvailableTitle("abc", Seq("abc", "abc (3)", "abc (5)")) == "abc (2)")
    assert(TabNameResolver.nextAvailableTitle("abc", Seq("abc", "abc (2)", "abc (4)")) == "abc (3)")
  }

  test("nextAvailableTitle does not double-up the (N) suffix when given an already-renamed title") {
    assert(TabNameResolver.nextAvailableTitle("abc (2)", Seq("abc", "abc (2)")) == "abc (3)")
  }

  test("stripParenSuffix removes trailing (N) and surrounding whitespace") {
    assert(TabNameResolver.stripParenSuffix("abc (2)") == "abc")
    assert(TabNameResolver.stripParenSuffix("abc (10)") == "abc")
    assert(TabNameResolver.stripParenSuffix("abc") == "abc")
    assert(TabNameResolver.stripParenSuffix("abc (notnum)") == "abc (notnum)")
  }

  test("nextAvailableTitle works against an empty set") {
    assert(TabNameResolver.nextAvailableTitle("abc", Nil) == "abc (2)")
  }
