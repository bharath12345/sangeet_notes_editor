package com.varpas.sangeet.core.api

import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.layout.{GridLayout, LayoutConfig, SectionGrid}

object LayoutApi:

  /** Compute layout for a single section. */
  def computeSectionLayout(
    section: Section,
    taal: Taal,
    config: LayoutConfig
  ): SectionGrid =
    GridLayout.layout(section, taal, config)

  /** Compute layout for all sections in a composition. */
  def computeLayout(
    composition: Composition,
    config: LayoutConfig
  ): List[SectionGrid] =
    GridLayout.layoutAll(composition, config)
