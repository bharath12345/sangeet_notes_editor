package sangeet.layout

import sangeet.model.*

object GridLayout:

  def layout(section: Section, taal: Taal, config: LayoutConfig): SectionGrid =
    val cells = BeatGrouper.group(section.events)
    val lines = LineBreaker.break(cells, taal, config)
    SectionGrid(section.name, section.sectionType, lines)

  def layoutAll(composition: Composition, config: LayoutConfig): List[SectionGrid] =
    composition.sections.map(s => layout(s, composition.metadata.taal, config))
