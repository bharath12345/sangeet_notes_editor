package com.varpas.sangeet.desktop.render

import scalafx.scene.text.Font

import com.varpas.sangeet.core.model.SwarScript
import com.varpas.sangeet.core.render.ScriptMap

object FontCache:

  private var cache: Map[(String, Double), Font] = Map.empty

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
