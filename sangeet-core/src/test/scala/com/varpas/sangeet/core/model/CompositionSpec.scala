package com.varpas.sangeet.core.model

import org.scalatest.funsuite.AnyFunSuite

class CompositionSpec extends AnyFunSuite:

  val sampleRaag = Raag(
    name = "Yaman",
    thaat = Some("Kalyan"),
    arohana = Some(List("Ni", "Re", "Ga", "Ma", "Pa", "Dha", "Ni", "Sa")),
    avarohana = Some(List("Sa", "Ni", "Dha", "Pa", "Ma", "Ga", "Re", "Sa")),
    vadi = Some("Ga"),
    samvadi = Some("Ni"),
    pakad = Some("Ni Re Ga Ma Dha Ni Re Sa"),
    prahar = Some(1)
  )

  val sampleTaal = Taal(
    name = "Teentaal",
    matras = 16,
    vibhags = List(
      Vibhag(4, VibhagMarker.Sam),
      Vibhag(4, VibhagMarker.Taali(2)),
      Vibhag(4, VibhagMarker.Khali),
      Vibhag(4, VibhagMarker.Taali(3))
    ),
    theka = None
  )

  val sampleMetadata = Metadata(
    title = "Test Composition",
    compositionType = CompositionType.Gat,
    raag = sampleRaag,
    taal = sampleTaal,
    laya = Some(Laya.Vilambit),
    script = None,
    instrument = Some("Sitar"),
    composer = Some("Test Composer"),
    author = Some("Test Author"),
    source = Some("Guruji"),
    showStrokeLine = true,
    showSahityaLine = false,
    createdAt = "2026-05-01T00:00:00Z",
    updatedAt = "2026-05-01T00:00:00Z"
  )

  test("Composition construction with sections") {
    val section = Section(
      name = "Sthayi",
      sectionType = SectionType.Sthayi,
      events = List.empty,
      tihai = None
    )
    val composition = Composition(sampleMetadata, List(section))
    assert(composition.sections.size == 1)
    assert(composition.metadata.title == "Test Composition")
  }

  test("Metadata default values") {
    assert(sampleMetadata.showStrokeLine == true)
    assert(sampleMetadata.showSahityaLine == false)
  }

  test("CompositionType enum values") {
    assert(CompositionType.Bandish.isInstanceOf[CompositionType])
    assert(CompositionType.Gat.isInstanceOf[CompositionType])
    assert(CompositionType.Palta.isInstanceOf[CompositionType])
    val custom = CompositionType.Custom("MyType")
    custom match
      case CompositionType.Custom(name) => assert(name == "MyType")
      case _                            => fail("Expected Custom CompositionType")
  }

  test("Tihai construction") {
    val start   = BeatPosition(0, 1, Rational.onBeat)
    val landing = BeatPosition(1, 1, Rational.onBeat)
    val tihai   = Tihai(start, landing)
    assert(tihai.startBeat == start)
    assert(tihai.landingBeat == landing)
  }

  test("Section with tihai") {
    val start   = BeatPosition(0, 1, Rational.onBeat)
    val landing = BeatPosition(1, 1, Rational.onBeat)
    val tihai   = Tihai(start, landing)
    val section = Section(
      name = "Taan 1",
      sectionType = SectionType.Taan,
      events = List.empty,
      tihai = Some(tihai)
    )
    assert(section.tihai.isDefined)
    assert(section.tihai.get.startBeat == start)
  }
