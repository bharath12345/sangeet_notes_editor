package com.varpas.sangeet.desktop.diagnostics

import java.util.concurrent.atomic.AtomicInteger

/** Counters that accumulate over a single desktop session, flushed once into the `AppQuit` PostHog event so we don't
  * blow the analytics budget on per-keystroke captures. Process-global because the call sites ([[EditorKeyHandler]])
  * don't have a clean handle to the MainApp-scoped state.
  *
  * Reset on app launch is implicit: a fresh JVM means fresh counters. Tests that exercise the increment paths should
  * call `reset()` if they care about exact totals.
  */
object SessionStats:

  private val _swarInputCount: AtomicInteger = new AtomicInteger(0)

  def incrementSwarInput(): Unit =
    val _ = _swarInputCount.incrementAndGet()

  def swarInputCount: Int = _swarInputCount.get()

  def reset(): Unit = _swarInputCount.set(0)
