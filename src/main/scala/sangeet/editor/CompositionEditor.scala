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

  def removeLastEvent: Option[CompositionEditor] =
    val section = currentSection
    if section.events.nonEmpty then
      val newEvents = section.events.init
      Some(updateCurrentSection(section.copy(events = newEvents)))
    else None

object CompositionEditor:

  def empty(taal: Taal, raag: Raag): CompositionEditor =
    create(
      title = "Untitled",
      compositionType = CompositionType.Gat,
      taal = taal,
      raag = raag,
      laya = None
    )

  def create(
    title: String,
    compositionType: CompositionType,
    taal: Taal,
    raag: Raag,
    laya: Option[Laya],
    taanCount: Int = 0
  ): CompositionEditor =
    val now = java.time.Instant.now().toString
    val metadata = Metadata(
      title = title,
      compositionType = compositionType,
      raag = raag,
      taal = taal,
      laya = laya,
      instrument = Some("Sitar"),
      composer = None,
      author = None,
      source = None,
      createdAt = now,
      updatedAt = now
    )
    val sections = compositionType match
      case CompositionType.Palta =>
        List(Section("Palta", SectionType.Palta, Nil))
      case CompositionType.Gat =>
        val base = List(
          Section("Gat", SectionType.Custom("Gat"), Nil),
          Section("Antara", SectionType.Antara, Nil)
        )
        val taans = (1 to taanCount).map { i =>
          Section(s"Taan $i", SectionType.Taan, Nil)
        }.toList
        base ++ taans
      case _ =>
        List(Section("Sthayi", SectionType.Sthayi, Nil))
    val composition = Composition(
      metadata = metadata,
      sections = sections,
      tihais = Nil
    )
    CompositionEditor(composition, 0, CursorModel(taal))
