package sangeet.editor

import sangeet.model.*

case class CompositionEditor(
  composition: Composition,
  currentSectionIndex: Int,
  cursor: CursorModel
):

  def currentSection: Section =
    composition.sections(currentSectionIndex)

  def updateCurrentSection(section: Section): CompositionEditor =
    val newSections = composition.sections.updated(currentSectionIndex, section)
    copy(composition = composition.copy(sections = newSections))

  def addEvent(event: Event): CompositionEditor =
    val section = currentSection
    val newEvents = section.events :+ event
    updateCurrentSection(section.copy(events = newEvents))

object CompositionEditor:

  def empty(taal: Taal, raag: Raag): CompositionEditor =
    val now = java.time.Instant.now().toString
    val metadata = Metadata(
      title = "Untitled",
      compositionType = CompositionType.Gat,
      raag = raag,
      taal = taal,
      laya = None,
      instrument = Some("Sitar"),
      composer = None,
      author = None,
      source = None,
      createdAt = now,
      updatedAt = now
    )
    val composition = Composition(
      metadata = metadata,
      sections = List(Section("Sthayi", SectionType.Sthayi, Nil)),
      tihais = Nil
    )
    CompositionEditor(composition, 0, CursorModel(taal))
