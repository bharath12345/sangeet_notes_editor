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

  /** Max cycle index in the current section's events, or 0 if empty. */
  def maxCycle: Int =
    val section = currentSection
    if section.events.isEmpty then 0
    else section.events.map(_.position.cycle).max

  /** Remove section at index. Returns None if it's the last section. */
  def removeSection(idx: Int): Option[CompositionEditor] =
    if composition.sections.size <= 1 then None
    else
      val newSections = composition.sections.patch(idx, Nil, 1)
      val newIdx = if currentSectionIndex >= newSections.size then newSections.size - 1
                   else if currentSectionIndex > idx then currentSectionIndex - 1
                   else currentSectionIndex
      Some(copy(
        composition = composition.copy(sections = newSections),
        currentSectionIndex = newIdx,
        cursor = if newIdx != currentSectionIndex then CursorModel(composition.metadata.taal) else cursor
      ))

  /** Rename section at index. */
  def renameSection(idx: Int, newName: String): CompositionEditor =
    val section = composition.sections(idx)
    val newSections = composition.sections.updated(idx, section.copy(name = newName))
    copy(composition = composition.copy(sections = newSections))

  /** Move section from one index to another. */
  def moveSection(from: Int, to: Int): CompositionEditor =
    if from == to || from < 0 || to < 0 ||
       from >= composition.sections.size || to >= composition.sections.size then this
    else
      val section = composition.sections(from)
      val without = composition.sections.patch(from, Nil, 1)
      val newSections = without.patch(to, List(section), 0)
      val newIdx = if currentSectionIndex == from then to
                   else if from < currentSectionIndex && to >= currentSectionIndex then currentSectionIndex - 1
                   else if from > currentSectionIndex && to <= currentSectionIndex then currentSectionIndex + 1
                   else currentSectionIndex
      copy(composition = composition.copy(sections = newSections), currentSectionIndex = newIdx)

  /** Modify the last Swar event in current section. Returns None if no Swar found. */
  def modifyLastSwar(f: Event.Swar => Event.Swar): Option[CompositionEditor] =
    val section = currentSection
    val lastSwarIdx = section.events.lastIndexWhere(_.isInstanceOf[Event.Swar])
    if lastSwarIdx >= 0 then
      val swar = section.events(lastSwarIdx).asInstanceOf[Event.Swar]
      val newEvents = section.events.updated(lastSwarIdx, f(swar))
      Some(updateCurrentSection(section.copy(events = newEvents)))
    else None

  /** Set stroke on the swar event at the given cursor position.
    * If multiple swars exist at the same beat, uses subIndex to pick the right one.
    * Returns None if no swar found at that position. */
  def setStrokeAt(cursor: CursorModel, stroke: Stroke): Option[CompositionEditor] =
    val section = currentSection
    val swarsAtBeat = section.events.zipWithIndex.collect {
      case (s: Event.Swar, idx) if s.beat.cycle == cursor.cycle && s.beat.beat == cursor.beat => (s, idx)
    }
    if swarsAtBeat.isEmpty then None
    else
      val targetIdx = math.min(cursor.subIndex, swarsAtBeat.size - 1)
      val (swar, eventIdx) = swarsAtBeat(targetIdx)
      val newEvents = section.events.updated(eventIdx, swar.copy(stroke = Some(stroke)))
      Some(updateCurrentSection(section.copy(events = newEvents)))

  /** Clear the explicit stroke on the swar at cursor position (revert to auto Da/Ra). */
  def clearStrokeAt(cursor: CursorModel): Option[CompositionEditor] =
    val section = currentSection
    val swarsAtBeat = section.events.zipWithIndex.collect {
      case (s: Event.Swar, idx) if s.beat.cycle == cursor.cycle && s.beat.beat == cursor.beat => (s, idx)
    }
    if swarsAtBeat.isEmpty then None
    else
      val targetIdx = math.min(cursor.subIndex, swarsAtBeat.size - 1)
      val (swar, eventIdx) = swarsAtBeat(targetIdx)
      val newEvents = section.events.updated(eventIdx, swar.copy(stroke = None))
      Some(updateCurrentSection(section.copy(events = newEvents)))

  /** Count swar events at a given beat position (for subdivision navigation in stroke mode). */
  def swarsAtBeat(cycle: Int, beat: Int): Int =
    currentSection.events.count {
      case s: Event.Swar => s.beat.cycle == cycle && s.beat.beat == beat
      case _ => false
    }

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
    taanCount: Int = 0,
    showStrokeLine: Boolean = false,
    showSahityaLine: Boolean = false
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
      showStrokeLine = showStrokeLine,
      showSahityaLine = showSahityaLine,
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
      sections = sections
    )
    CompositionEditor(composition, 0, CursorModel(taal))
