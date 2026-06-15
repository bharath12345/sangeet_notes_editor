package com.varpas.sangeet.desktop.render

import scalafx.scene.text.Font

import com.varpas.sangeet.core.model.SwarScript
import com.varpas.sangeet.core.render.ScriptMap
import com.varpas.sangeet.desktop.editor.AppLogger

/** Caches ScalaFX [[Font]] instances by (family, size).
  *
  * [[init]] must be called once at desktop startup, before any rendering. It registers the bundled Noto fonts with the
  * JavaFX font system so that subsequent `Font("Noto Sans Devanagari", …)` lookups resolve to the bundled glyphs
  * regardless of which fonts the user has installed system-wide.
  *
  * Bundling guarantees pixel-identical rendering of Devanagari / Kannada / Telugu sahitya with the web app (which loads
  * the same Noto families from the Google Fonts CDN). Without this, `Font(name, size)` silently falls back to a system
  * default if Noto isn't installed.
  *
  * Failures during font loading are logged as warnings — the cache still functions, callers just get whatever the
  * platform default is for the missing family.
  */
object FontCache:

  /** Resource path → preload size pair. The size passed to `Font.loadFont` is irrelevant for registration; we only need
    * any positive value so JavaFX parses the file and adds the family to its font database.
    */
  private val bundledFonts: List[String] = List(
    "/fonts/NotoSans-Regular.ttf",
    "/fonts/NotoSans-Bold.ttf",
    "/fonts/NotoSansDevanagari-Regular.ttf",
    "/fonts/NotoSansDevanagari-Bold.ttf",
    "/fonts/NotoSansKannada-Regular.ttf",
    "/fonts/NotoSansTelugu-Regular.ttf"
  )

  private var cache: Map[(String, Double), Font] = Map.empty
  private var initialized: Boolean               = false

  /** Load all bundled Noto fonts into the JavaFX font system. Idempotent — subsequent calls are no-ops. Safe to call
    * before the JavaFX toolkit is fully up; `javafx.scene.text.Font.loadFont` only needs class-loader access to the
    * resource stream.
    */
  def init(): Unit = synchronized {
    if initialized then return
    initialized = true
    bundledFonts.foreach { path =>
      try
        val stream = getClass.getResourceAsStream(path)
        if stream == null then AppLogger.info(s"FontCache: bundled font resource not found: $path")
        else
          val loaded = javafx.scene.text.Font.loadFont(stream, 10.0)
          if loaded == null then AppLogger.info(s"FontCache: javafx.Font.loadFont returned null for $path")
          stream.close()
      catch case ex: Exception => AppLogger.info(s"FontCache: failed to load $path — ${ex.getMessage}")
    }
  }

  def font(name: String, size: Double): Font =
    cache.getOrElse(
      (name, size), {
        val f = Font(name, size)
        cache = cache.updated((name, size), f)
        f
      }
    )

  def scriptFont(script: SwarScript, size: Double): Font =
    font(ScriptMap.fontName(script), size)
