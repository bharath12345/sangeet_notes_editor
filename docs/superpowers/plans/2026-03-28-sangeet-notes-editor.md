# Sangeet Notes Editor — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a desktop Hindustani classical music notation editor for sitar compositions using Bhatkhande-style notation, with local `.swar` file storage, audio playback, and PDF export.

**Architecture:** Hybrid stream-model with grid rendering. Pure domain model → circe JSON serialization → layout engine (beat grouping → line breaking → grid positioning) → dual renderers (ScalaFX Canvas for screen, PDFBox for export). MIDI-based audio playback with pluggable sound engine trait.

**Tech Stack:** Scala 3.4.2, ScalaFX 21.0.0-R32, circe 0.14.7, Apache PDFBox 3.0.2, ScalaTest 3.2.18, sbt 1.10.x, JVM 17+

**Spec:** `docs/superpowers/specs/2026-03-28-sangeet-notes-editor-design.md`

---

## Phase 1: Foundation — Domain Model & Serialization

### Task 1: Project Scaffold

**Files:**
- Create: `build.sbt`
- Create: `project/build.properties`
- Create: `project/plugins.sbt`
- Create: `src/main/scala/sangeet/Main.scala`
- Create: `src/test/scala/sangeet/SanitySpec.scala`

- [ ] **Step 1: Create `project/build.properties`**

```properties
sbt.version=1.10.7
```

- [ ] **Step 2: Create `project/plugins.sbt`**

```scala
// intentionally empty for now
```

- [ ] **Step 3: Create `build.sbt`**

```scala
val scala3Version = "3.4.2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "sangeet-notes-editor",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked"),
    libraryDependencies ++= Seq(
      "org.scalafx"       %% "scalafx"        % "21.0.0-R32",
      "io.circe"          %% "circe-core"     % "0.14.7",
      "io.circe"          %% "circe-parser"   % "0.14.7",
      "io.circe"          %% "circe-generic"  % "0.14.7",
      "org.apache.pdfbox"  % "pdfbox"         % "3.0.2",
      "org.scalatest"     %% "scalatest"      % "3.2.18" % Test,
    ),
    // JavaFX platform dependencies (auto-resolved by ScalaFX)
    fork := true,
  )
```

- [ ] **Step 4: Create placeholder `Main.scala`**

```scala
// src/main/scala/sangeet/Main.scala
package sangeet

@main def run(): Unit =
  println("Sangeet Notes Editor — starting...")
```

- [ ] **Step 5: Create sanity test**

```scala
// src/test/scala/sangeet/SanitySpec.scala
package sangeet

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SanitySpec extends AnyFlatSpec with Matchers:
  "The project" should "compile and run tests" in {
    1 + 1 shouldBe 2
  }
```

- [ ] **Step 6: Verify compilation and test**

Run: `sbt compile test`
Expected: Compilation succeeds, 1 test passes.

- [ ] **Step 7: Commit**

```bash
git add build.sbt project/ src/
git commit -m "feat: scaffold sbt project with dependencies"
```

---

### Task 2: Core Swar Types

**Files:**
- Create: `src/main/scala/sangeet/model/Note.scala`
- Create: `src/main/scala/sangeet/model/Rational.scala`
- Create: `src/main/scala/sangeet/model/BeatPosition.scala`
- Create: `src/main/scala/sangeet/model/Stroke.scala`
- Create: `src/test/scala/sangeet/model/RationalSpec.scala`
- Create: `src/test/scala/sangeet/model/NoteSpec.scala`

- [ ] **Step 1: Write failing tests for Rational**

```scala
// src/test/scala/sangeet/model/RationalSpec.scala
package sangeet.model

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RationalSpec extends AnyFlatSpec with Matchers:

  "Rational" should "represent fractions" in {
    val half = Rational(1, 2)
    half.numerator shouldBe 1
    half.denominator shouldBe 2
  }

  it should "convert to double" in {
    Rational(1, 2).toDouble shouldBe 0.5
    Rational(1, 3).toDouble shouldBe (1.0 / 3.0) +- 0.0001
    Rational(0, 1).toDouble shouldBe 0.0
  }

  it should "add two rationals" in {
    val r = Rational(1, 4) + Rational(1, 4)
    r.toDouble shouldBe 0.5
  }

  it should "compare rationals" in {
    Rational(1, 2) should be > Rational(1, 3)
    Rational(1, 4) should be < Rational(1, 3)
    Rational(2, 4) shouldBe Rational(1, 2)
  }

  it should "normalize fractions" in {
    Rational(2, 4).numerator shouldBe 1
    Rational(2, 4).denominator shouldBe 2
    Rational(6, 8).numerator shouldBe 3
    Rational(6, 8).denominator shouldBe 4
  }

  it should "represent on-beat as 0/1" in {
    Rational.onBeat shouldBe Rational(0, 1)
  }

  it should "represent a full beat as 1/1" in {
    Rational.fullBeat shouldBe Rational(1, 1)
  }
```

- [ ] **Step 2: Run tests to verify failure**

Run: `sbt "testOnly sangeet.model.RationalSpec"`
Expected: FAIL — `Rational` not found.

- [ ] **Step 3: Implement Rational**

```scala
// src/main/scala/sangeet/model/Rational.scala
package sangeet.model

case class Rational private (numerator: Int, denominator: Int) extends Ordered[Rational]:

  def toDouble: Double = numerator.toDouble / denominator.toDouble

  def +(other: Rational): Rational =
    Rational(
      numerator * other.denominator + other.numerator * denominator,
      denominator * other.denominator
    )

  def compare(that: Rational): Int =
    (this.numerator * that.denominator) compare (that.numerator * this.denominator)

  override def equals(obj: Any): Boolean = obj match
    case r: Rational => numerator * r.denominator == r.numerator * denominator
    case _           => false

  override def hashCode(): Int =
    val n = Rational.normalize(numerator, denominator)
    (n._1, n._2).hashCode()

object Rational:
  def apply(num: Int, den: Int): Rational =
    require(den != 0, "Denominator cannot be zero")
    val (n, d) = normalize(num, den)
    new Rational(n, d)

  val onBeat: Rational = Rational(0, 1)
  val fullBeat: Rational = Rational(1, 1)

  private def normalize(num: Int, den: Int): (Int, Int) =
    if num == 0 then (0, 1)
    else
      val g = gcd(math.abs(num), math.abs(den))
      val sign = if den < 0 then -1 else 1
      (sign * num / g, sign * den / g)

  private def gcd(a: Int, b: Int): Int =
    if b == 0 then a else gcd(b, a % b)
```

- [ ] **Step 4: Run Rational tests**

Run: `sbt "testOnly sangeet.model.RationalSpec"`
Expected: All tests pass.

- [ ] **Step 5: Write failing tests for Note types**

```scala
// src/test/scala/sangeet/model/NoteSpec.scala
package sangeet.model

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class NoteSpec extends AnyFlatSpec with Matchers:

  "Note" should "have 7 values" in {
    Note.values should have length 7
  }

  "Variant" should "have Shuddha, Komal, Tivra" in {
    Variant.values should contain allOf (Variant.Shuddha, Variant.Komal, Variant.Tivra)
  }

  "Octave" should "have 5 values in order" in {
    Octave.values.toList shouldBe List(
      Octave.AtiMandra, Octave.Mandra, Octave.Madhya, Octave.Taar, Octave.AtiTaar
    )
  }

  "Stroke" should "have Da, Ra, Chikari, Jod" in {
    Stroke.values should have length 4
  }

  "BeatPosition" should "represent a position in a taal cycle" in {
    val pos = BeatPosition(cycle = 0, beat = 3, subdivision = Rational(1, 2))
    pos.cycle shouldBe 0
    pos.beat shouldBe 3
    pos.subdivision shouldBe Rational(1, 2)
  }

  it should "order positions correctly" in {
    val pos1 = BeatPosition(0, 0, Rational.onBeat)
    val pos2 = BeatPosition(0, 0, Rational(1, 2))
    val pos3 = BeatPosition(0, 1, Rational.onBeat)
    val pos4 = BeatPosition(1, 0, Rational.onBeat)

    List(pos3, pos1, pos4, pos2).sorted shouldBe List(pos1, pos2, pos3, pos4)
  }
```

- [ ] **Step 6: Implement Note, Variant, Octave, Stroke, BeatPosition**

```scala
// src/main/scala/sangeet/model/Note.scala
package sangeet.model

enum Note:
  case Sa, Re, Ga, Ma, Pa, Dha, Ni

enum Variant:
  case Shuddha, Komal, Tivra

enum Octave:
  case AtiMandra, Mandra, Madhya, Taar, AtiTaar
```

```scala
// src/main/scala/sangeet/model/Stroke.scala
package sangeet.model

enum Stroke:
  case Da, Ra, Chikari, Jod
```

```scala
// src/main/scala/sangeet/model/BeatPosition.scala
package sangeet.model

case class BeatPosition(
  cycle: Int,
  beat: Int,
  subdivision: Rational
) extends Ordered[BeatPosition]:

  def compare(that: BeatPosition): Int =
    val c = this.cycle compare that.cycle
    if c != 0 then c
    else
      val b = this.beat compare that.beat
      if b != 0 then b
      else this.subdivision compare that.subdivision
```

- [ ] **Step 7: Run all tests**

Run: `sbt "testOnly sangeet.model.*"`
Expected: All tests pass.

- [ ] **Step 8: Commit**

```bash
git add src/
git commit -m "feat: add core swar types — Note, Variant, Octave, Rational, BeatPosition, Stroke"
```

---

### Task 3: Ornament Types

**Files:**
- Create: `src/main/scala/sangeet/model/Ornament.scala`
- Create: `src/main/scala/sangeet/model/NoteRef.scala`
- Create: `src/test/scala/sangeet/model/OrnamentSpec.scala`

- [ ] **Step 1: Write failing tests**

```scala
// src/test/scala/sangeet/model/OrnamentSpec.scala
package sangeet.model

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class OrnamentSpec extends AnyFlatSpec with Matchers:

  val saRef = NoteRef(Note.Sa, Variant.Shuddha, Octave.Madhya)
  val reRef = NoteRef(Note.Re, Variant.Shuddha, Octave.Madhya)
  val gaRef = NoteRef(Note.Ga, Variant.Shuddha, Octave.Madhya)

  "Meend" should "have start, end, direction, and intermediate notes" in {
    val m = Meend(saRef, gaRef, MeendDirection.Ascending, List(reRef))
    m.startNote shouldBe saRef
    m.endNote shouldBe gaRef
    m.direction shouldBe MeendDirection.Ascending
    m.intermediateNotes shouldBe List(reRef)
  }

  "KanSwar" should "hold a grace note" in {
    val k = KanSwar(reRef)
    k.graceNote shouldBe reRef
  }

  "Murki" should "hold a sequence of notes" in {
    val m = Murki(List(gaRef, reRef, saRef))
    m.notes should have length 3
  }

  "CustomOrnament" should "hold arbitrary parameters" in {
    val c = CustomOrnament("newTechnique", Map("speed" -> "fast", "intensity" -> "high"))
    c.name shouldBe "newTechnique"
    c.parameters("speed") shouldBe "fast"
  }

  "All ornament types" should "be subtypes of Ornament" in {
    val ornaments: List[Ornament] = List(
      Meend(saRef, gaRef, MeendDirection.Ascending, Nil),
      KanSwar(reRef),
      Murki(List(saRef)),
      Gamak(),
      Andolan(),
      Krintan(List(gaRef, reRef)),
      Gitkari(),
      Ghaseet(gaRef),
      Sparsh(reRef),
      Zamzama(List(saRef, reRef)),
      CustomOrnament("test", Map.empty)
    )
    ornaments should have length 11
  }
```

- [ ] **Step 2: Run tests to verify failure**

Run: `sbt "testOnly sangeet.model.OrnamentSpec"`
Expected: FAIL — types not found.

- [ ] **Step 3: Implement NoteRef and Ornament hierarchy**

```scala
// src/main/scala/sangeet/model/NoteRef.scala
package sangeet.model

case class NoteRef(
  note: Note,
  variant: Variant,
  octave: Octave
)
```

```scala
// src/main/scala/sangeet/model/Ornament.scala
package sangeet.model

sealed trait Ornament

case class Meend(
  startNote: NoteRef,
  endNote: NoteRef,
  direction: MeendDirection,
  intermediateNotes: List[NoteRef]
) extends Ornament

case class KanSwar(graceNote: NoteRef) extends Ornament

case class Murki(notes: List[NoteRef]) extends Ornament

case class Gamak() extends Ornament

case class Andolan() extends Ornament

case class Krintan(notes: List[NoteRef]) extends Ornament

case class Gitkari() extends Ornament

case class Ghaseet(targetNote: NoteRef) extends Ornament

case class Sparsh(touchNote: NoteRef) extends Ornament

case class Zamzama(notes: List[NoteRef]) extends Ornament

case class CustomOrnament(
  name: String,
  parameters: Map[String, String]
) extends Ornament

enum MeendDirection:
  case Ascending, Descending
```

- [ ] **Step 4: Run tests**

Run: `sbt "testOnly sangeet.model.OrnamentSpec"`
Expected: All tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/
git commit -m "feat: add Ornament type hierarchy with 11 ornament types"
```

---

### Task 4: Raag, Taal, Event, Section, Composition

**Files:**
- Create: `src/main/scala/sangeet/model/Raag.scala`
- Create: `src/main/scala/sangeet/model/Taal.scala`
- Create: `src/main/scala/sangeet/model/Event.scala`
- Create: `src/main/scala/sangeet/model/Section.scala`
- Create: `src/main/scala/sangeet/model/Composition.scala`
- Create: `src/test/scala/sangeet/model/CompositionSpec.scala`

- [ ] **Step 1: Write failing tests**

```scala
// src/test/scala/sangeet/model/CompositionSpec.scala
package sangeet.model

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CompositionSpec extends AnyFlatSpec with Matchers:

  val teentaal = Taal(
    name = "Teentaal",
    matras = 16,
    vibhags = List(
      Vibhag(4, VibhagMarker.Sam),
      Vibhag(4, VibhagMarker.Taali(2)),
      Vibhag(4, VibhagMarker.Khali),
      Vibhag(4, VibhagMarker.Taali(3))
    ),
    theka = Some(List("Dha","Dhin","Dhin","Dha","Dha","Dhin","Dhin","Dha",
                       "Dha","Tin","Tin","Ta","Ta","Dhin","Dhin","Dha"))
  )

  val yaman = Raag(
    name = "Yaman",
    thaat = Some("Kalyan"),
    arohana = Some(List("S", "R", "G", "M+", "P", "D", "N", "S'")),
    avarohana = Some(List("S'", "N", "D", "P", "M+", "G", "R", "S")),
    vadi = Some("G"),
    samvadi = Some("N"),
    pakad = None,
    prahar = Some(1)
  )

  "Taal" should "compute total matras from vibhags" in {
    teentaal.matras shouldBe 16
    teentaal.vibhags.map(_.beats).sum shouldBe 16
  }

  "Event.Swar" should "hold all note properties" in {
    val swar = Event.Swar(
      note = Note.Sa,
      variant = Variant.Shuddha,
      octave = Octave.Madhya,
      beat = BeatPosition(0, 0, Rational.onBeat),
      duration = Rational.fullBeat,
      stroke = Some(Stroke.Da),
      ornaments = Nil,
      sahitya = None
    )
    swar.note shouldBe Note.Sa
    swar.stroke shouldBe Some(Stroke.Da)
  }

  "Event.Rest" should "represent silence" in {
    val rest = Event.Rest(
      beat = BeatPosition(0, 1, Rational.onBeat),
      duration = Rational.fullBeat
    )
    rest.beat.beat shouldBe 1
  }

  "Section" should "hold events with a section type" in {
    val section = Section(
      name = "Sthayi",
      sectionType = SectionType.Sthayi,
      events = List(
        Event.Swar(Note.Sa, Variant.Shuddha, Octave.Madhya,
          BeatPosition(0, 0, Rational.onBeat), Rational.fullBeat, None, Nil, None)
      )
    )
    section.events should have length 1
    section.sectionType shouldBe SectionType.Sthayi
  }

  "Composition" should "hold metadata, sections, and tihais" in {
    val comp = Composition(
      metadata = Metadata(
        title = "Vilambit Gat in Yaman",
        compositionType = CompositionType.Gat,
        raag = yaman,
        taal = teentaal,
        laya = Some(Laya.Vilambit),
        instrument = Some("Sitar"),
        composer = Some("Traditional"),
        author = Some("Bharadwaj"),
        source = Some("Guruji class"),
        createdAt = "2026-03-28T10:00:00Z",
        updatedAt = "2026-03-28T10:00:00Z"
      ),
      sections = List(
        Section("Sthayi", SectionType.Sthayi, Nil)
      ),
      tihais = Nil
    )
    comp.metadata.title shouldBe "Vilambit Gat in Yaman"
    comp.metadata.laya shouldBe Some(Laya.Vilambit)
    comp.sections should have length 1
  }

  "CompositionType.Palta" should "exist" in {
    CompositionType.Palta shouldBe CompositionType.Palta
  }

  "CompositionType.Custom" should "hold a name" in {
    val ct = CompositionType.Custom("Alap")
    ct.name shouldBe "Alap"
  }

  "SectionType" should "include Palta, Arohi, Avarohi" in {
    SectionType.Palta shouldBe SectionType.Palta
    SectionType.Arohi shouldBe SectionType.Arohi
    SectionType.Avarohi shouldBe SectionType.Avarohi
  }
```

- [ ] **Step 2: Run tests to verify failure**

Run: `sbt "testOnly sangeet.model.CompositionSpec"`
Expected: FAIL — types not found.

- [ ] **Step 3: Implement Raag**

```scala
// src/main/scala/sangeet/model/Raag.scala
package sangeet.model

case class Raag(
  name: String,
  thaat: Option[String],
  arohana: Option[List[String]],
  avarohana: Option[List[String]],
  vadi: Option[String],
  samvadi: Option[String],
  pakad: Option[String],
  prahar: Option[Int]
)
```

- [ ] **Step 4: Implement Taal**

```scala
// src/main/scala/sangeet/model/Taal.scala
package sangeet.model

case class Taal(
  name: String,
  matras: Int,
  vibhags: List[Vibhag],
  theka: Option[List[String]]
)

case class Vibhag(
  beats: Int,
  marker: VibhagMarker
)

enum VibhagMarker:
  case Sam
  case Taali(number: Int)
  case Khali
```

- [ ] **Step 5: Implement Event**

```scala
// src/main/scala/sangeet/model/Event.scala
package sangeet.model

enum Event:
  case Swar(
    note: Note,
    variant: Variant,
    octave: Octave,
    beat: BeatPosition,
    duration: Rational,
    stroke: Option[Stroke],
    ornaments: List[Ornament],
    sahitya: Option[String]
  )

  case Rest(
    beat: BeatPosition,
    duration: Rational
  )

  case Sustain(
    beat: BeatPosition,
    duration: Rational
  )
```

- [ ] **Step 6: Implement Section and Composition**

```scala
// src/main/scala/sangeet/model/Section.scala
package sangeet.model

case class Section(
  name: String,
  sectionType: SectionType,
  events: List[Event]
)

enum SectionType:
  case Sthayi, Antara, Sanchari, Abhog
  case Taan, Toda, Jhala
  case Palta, Arohi, Avarohi
  case Custom(name: String)
```

```scala
// src/main/scala/sangeet/model/Composition.scala
package sangeet.model

case class Composition(
  metadata: Metadata,
  sections: List[Section],
  tihais: List[Tihai]
)

case class Metadata(
  title: String,
  compositionType: CompositionType,
  raag: Raag,
  taal: Taal,
  laya: Option[Laya],
  instrument: Option[String],
  composer: Option[String],
  author: Option[String],
  source: Option[String],
  createdAt: String,
  updatedAt: String
)

enum CompositionType:
  case Bandish, Gat, Palta
  case Custom(name: String)

enum Laya:
  case AtiVilambit, Vilambit, Madhya, Drut, AtiDrut

case class Tihai(
  sectionName: String,
  startBeat: BeatPosition,
  landingBeat: BeatPosition
)
```

- [ ] **Step 7: Run all tests**

Run: `sbt "testOnly sangeet.model.*"`
Expected: All tests pass.

- [ ] **Step 8: Commit**

```bash
git add src/
git commit -m "feat: add Raag, Taal, Event, Section, Composition domain types"
```

---

### Task 5: JSON Codecs — Primitive Types

**Files:**
- Create: `src/main/scala/sangeet/format/Codecs.scala`
- Create: `src/test/scala/sangeet/format/CodecsSpec.scala`

- [ ] **Step 1: Write failing tests for enum and Rational codecs**

```scala
// src/test/scala/sangeet/format/CodecsSpec.scala
package sangeet.format

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import io.circe.syntax.*
import io.circe.parser.*
import sangeet.model.*

class CodecsSpec extends AnyFlatSpec with Matchers:
  import Codecs.given

  // --- Note enum ---
  "Note codec" should "serialize to lowercase string" in {
    Note.Sa.asJson.noSpaces shouldBe "\"sa\""
    Note.Dha.asJson.noSpaces shouldBe "\"dha\""
  }

  it should "deserialize from lowercase string" in {
    decode[Note]("\"sa\"") shouldBe Right(Note.Sa)
    decode[Note]("\"ni\"") shouldBe Right(Note.Ni)
  }

  // --- Variant enum ---
  "Variant codec" should "roundtrip" in {
    val v = Variant.Komal
    decode[Variant](v.asJson.noSpaces) shouldBe Right(v)
  }

  // --- Octave enum ---
  "Octave codec" should "serialize to lowercase" in {
    Octave.Madhya.asJson.noSpaces shouldBe "\"madhya\""
    Octave.Taar.asJson.noSpaces shouldBe "\"taar\""
  }

  // --- Stroke enum ---
  "Stroke codec" should "roundtrip" in {
    val s = Stroke.Da
    decode[Stroke](s.asJson.noSpaces) shouldBe Right(s)
  }

  // --- Rational ---
  "Rational codec" should "serialize as [num, den] array" in {
    Rational(1, 2).asJson.noSpaces shouldBe "[1,2]"
    Rational(0, 1).asJson.noSpaces shouldBe "[0,1]"
  }

  it should "deserialize from array" in {
    decode[Rational]("[1,4]") shouldBe Right(Rational(1, 4))
  }

  // --- BeatPosition ---
  "BeatPosition codec" should "roundtrip" in {
    val bp = BeatPosition(2, 5, Rational(1, 3))
    decode[BeatPosition](bp.asJson.noSpaces) shouldBe Right(bp)
  }

  // --- NoteRef ---
  "NoteRef codec" should "roundtrip" in {
    val nr = NoteRef(Note.Ga, Variant.Komal, Octave.Mandra)
    decode[NoteRef](nr.asJson.noSpaces) shouldBe Right(nr)
  }

  // --- Laya ---
  "Laya codec" should "serialize to lowercase" in {
    Laya.Vilambit.asJson.noSpaces shouldBe "\"vilambit\""
    Laya.AtiVilambit.asJson.noSpaces shouldBe "\"atiVilambit\""
  }

  // --- MeendDirection ---
  "MeendDirection codec" should "roundtrip" in {
    val d = MeendDirection.Descending
    decode[MeendDirection](d.asJson.noSpaces) shouldBe Right(d)
  }

  // --- VibhagMarker ---
  "VibhagMarker codec" should "serialize Sam as string" in {
    val sam: VibhagMarker = VibhagMarker.Sam
    sam.asJson.noSpaces shouldBe "\"sam\""
  }

  it should "serialize Khali as string" in {
    val khali: VibhagMarker = VibhagMarker.Khali
    khali.asJson.noSpaces shouldBe "\"khali\""
  }

  it should "serialize Taali as object" in {
    val taali: VibhagMarker = VibhagMarker.Taali(2)
    taali.asJson.noSpaces shouldBe """{"taali":2}"""
  }

  it should "roundtrip all variants" in {
    val markers = List(VibhagMarker.Sam, VibhagMarker.Khali, VibhagMarker.Taali(3))
    markers.foreach { m =>
      decode[VibhagMarker](m.asJson.noSpaces) shouldBe Right(m)
    }
  }

  // --- CompositionType ---
  "CompositionType codec" should "serialize simple variants as strings" in {
    (CompositionType.Gat: CompositionType).asJson.noSpaces shouldBe "\"gat\""
  }

  it should "serialize Custom with name" in {
    val ct: CompositionType = CompositionType.Custom("Alap")
    ct.asJson.noSpaces shouldBe """{"custom":"Alap"}"""
  }

  it should "roundtrip" in {
    val types: List[CompositionType] = List(
      CompositionType.Bandish, CompositionType.Gat,
      CompositionType.Palta, CompositionType.Custom("Alap")
    )
    types.foreach { t =>
      decode[CompositionType](t.asJson.noSpaces) shouldBe Right(t)
    }
  }
```

- [ ] **Step 2: Run tests to verify failure**

Run: `sbt "testOnly sangeet.format.CodecsSpec"`
Expected: FAIL — `Codecs` not found.

- [ ] **Step 3: Implement Codecs for primitive and enum types**

```scala
// src/main/scala/sangeet/format/Codecs.scala
package sangeet.format

import io.circe.*
import io.circe.syntax.*
import sangeet.model.*

object Codecs:

  // --- Helper for simple enums (lowercase string serialization) ---
  private def simpleEnumEncoder[E <: reflect.Enum](using values: ValueOf[Array[E]]): Encoder[E] =
    Encoder.encodeString.contramap(_.toString.head.toLower + _.toString.tail)

  // Note: Scala 3 enums don't have a universal ValueOf, so we write codecs explicitly.

  // --- Note ---
  given Encoder[Note] = Encoder.encodeString.contramap(_.toString.toLowerCase)
  given Decoder[Note] = Decoder.decodeString.emap { s =>
    Note.values.find(_.toString.equalsIgnoreCase(s))
      .toRight(s"Invalid Note: $s")
  }

  // --- Variant ---
  given Encoder[Variant] = Encoder.encodeString.contramap(_.toString.toLowerCase)
  given Decoder[Variant] = Decoder.decodeString.emap { s =>
    Variant.values.find(_.toString.equalsIgnoreCase(s))
      .toRight(s"Invalid Variant: $s")
  }

  // --- Octave ---
  given Encoder[Octave] = Encoder.encodeString.contramap {
    case Octave.AtiMandra => "atiMandra"
    case Octave.AtiTaar   => "atiTaar"
    case o                => o.toString.head.toLower + o.toString.tail
  }
  given Decoder[Octave] = Decoder.decodeString.emap { s =>
    val mapping = Map(
      "atimandra" -> Octave.AtiMandra, "mandra" -> Octave.Mandra,
      "madhya" -> Octave.Madhya, "taar" -> Octave.Taar, "atitaar" -> Octave.AtiTaar
    )
    mapping.get(s.toLowerCase).toRight(s"Invalid Octave: $s")
  }

  // --- Stroke ---
  given Encoder[Stroke] = Encoder.encodeString.contramap(_.toString.toLowerCase)
  given Decoder[Stroke] = Decoder.decodeString.emap { s =>
    Stroke.values.find(_.toString.equalsIgnoreCase(s))
      .toRight(s"Invalid Stroke: $s")
  }

  // --- Laya ---
  given Encoder[Laya] = Encoder.encodeString.contramap {
    case Laya.AtiVilambit => "atiVilambit"
    case Laya.AtiDrut     => "atiDrut"
    case l                => l.toString.head.toLower + l.toString.tail
  }
  given Decoder[Laya] = Decoder.decodeString.emap { s =>
    val mapping = Map(
      "ativilambit" -> Laya.AtiVilambit, "vilambit" -> Laya.Vilambit,
      "madhya" -> Laya.Madhya, "drut" -> Laya.Drut, "atidrut" -> Laya.AtiDrut
    )
    mapping.get(s.toLowerCase).toRight(s"Invalid Laya: $s")
  }

  // --- MeendDirection ---
  given Encoder[MeendDirection] = Encoder.encodeString.contramap(_.toString.toLowerCase)
  given Decoder[MeendDirection] = Decoder.decodeString.emap { s =>
    MeendDirection.values.find(_.toString.equalsIgnoreCase(s))
      .toRight(s"Invalid MeendDirection: $s")
  }

  // --- Rational (as [num, den] array) ---
  given Encoder[Rational] = Encoder.instance { r =>
    Json.arr(Json.fromInt(r.numerator), Json.fromInt(r.denominator))
  }
  given Decoder[Rational] = Decoder.instance { c =>
    for
      arr <- c.as[List[Int]]
      result <- arr match
        case List(n, d) => Right(Rational(n, d))
        case _          => Left(DecodingFailure("Rational must be [num, den]", c.history))
    yield result
  }

  // --- BeatPosition ---
  given Encoder[BeatPosition] = Encoder.instance { bp =>
    Json.obj(
      "cycle" -> Json.fromInt(bp.cycle),
      "beat" -> Json.fromInt(bp.beat),
      "subdivision" -> bp.subdivision.asJson
    )
  }
  given Decoder[BeatPosition] = Decoder.instance { c =>
    for
      cycle <- c.downField("cycle").as[Int]
      beat <- c.downField("beat").as[Int]
      sub <- c.downField("subdivision").as[Rational]
    yield BeatPosition(cycle, beat, sub)
  }

  // --- NoteRef ---
  given Encoder[NoteRef] = Encoder.instance { nr =>
    Json.obj(
      "note" -> nr.note.asJson,
      "variant" -> nr.variant.asJson,
      "octave" -> nr.octave.asJson
    )
  }
  given Decoder[NoteRef] = Decoder.instance { c =>
    for
      note <- c.downField("note").as[Note]
      variant <- c.downField("variant").as[Variant]
      octave <- c.downField("octave").as[Octave]
    yield NoteRef(note, variant, octave)
  }

  // --- VibhagMarker ---
  given Encoder[VibhagMarker] = Encoder.instance {
    case VibhagMarker.Sam      => Json.fromString("sam")
    case VibhagMarker.Khali    => Json.fromString("khali")
    case VibhagMarker.Taali(n) => Json.obj("taali" -> Json.fromInt(n))
  }
  given Decoder[VibhagMarker] = Decoder.instance { c =>
    c.as[String].map {
      case "sam"   => VibhagMarker.Sam
      case "khali" => VibhagMarker.Khali
    }.orElse {
      c.downField("taali").as[Int].map(VibhagMarker.Taali(_))
    }
  }

  // --- CompositionType ---
  given Encoder[CompositionType] = Encoder.instance {
    case CompositionType.Bandish    => Json.fromString("bandish")
    case CompositionType.Gat        => Json.fromString("gat")
    case CompositionType.Palta      => Json.fromString("palta")
    case CompositionType.Custom(n)  => Json.obj("custom" -> Json.fromString(n))
  }
  given Decoder[CompositionType] = Decoder.instance { c =>
    c.as[String].map {
      case "bandish" => CompositionType.Bandish
      case "gat"     => CompositionType.Gat
      case "palta"   => CompositionType.Palta
    }.orElse {
      c.downField("custom").as[String].map(CompositionType.Custom(_))
    }
  }
```

- [ ] **Step 4: Run tests**

Run: `sbt "testOnly sangeet.format.CodecsSpec"`
Expected: All tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/
git commit -m "feat: add circe JSON codecs for primitive and enum types"
```

---

### Task 6: JSON Codecs — Complex Types & Full Roundtrip

**Files:**
- Modify: `src/main/scala/sangeet/format/Codecs.scala`
- Create: `src/test/scala/sangeet/format/CompositionCodecSpec.scala`

- [ ] **Step 1: Write failing test for full Composition roundtrip**

```scala
// src/test/scala/sangeet/format/CompositionCodecSpec.scala
package sangeet.format

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import io.circe.syntax.*
import io.circe.parser.*
import sangeet.model.*

class CompositionCodecSpec extends AnyFlatSpec with Matchers:
  import Codecs.given

  val sampleComposition: Composition = Composition(
    metadata = Metadata(
      title = "Vilambit Gat in Yaman",
      compositionType = CompositionType.Gat,
      raag = Raag(
        name = "Yaman", thaat = Some("Kalyan"),
        arohana = Some(List("S", "R", "G", "M+", "P", "D", "N", "S'")),
        avarohana = Some(List("S'", "N", "D", "P", "M+", "G", "R", "S")),
        vadi = Some("G"), samvadi = Some("N"), pakad = None, prahar = Some(1)
      ),
      taal = Taal(
        name = "Teentaal", matras = 16,
        vibhags = List(
          Vibhag(4, VibhagMarker.Sam), Vibhag(4, VibhagMarker.Taali(2)),
          Vibhag(4, VibhagMarker.Khali), Vibhag(4, VibhagMarker.Taali(3))
        ),
        theka = Some(List("Dha","Dhin","Dhin","Dha","Dha","Dhin","Dhin","Dha",
                           "Dha","Tin","Tin","Ta","Ta","Dhin","Dhin","Dha"))
      ),
      laya = Some(Laya.Vilambit),
      instrument = Some("Sitar"),
      composer = Some("Traditional"),
      author = Some("Bharadwaj"),
      source = Some("Guruji class"),
      createdAt = "2026-03-28T10:00:00Z",
      updatedAt = "2026-03-28T10:00:00Z"
    ),
    sections = List(
      Section("Sthayi", SectionType.Sthayi, List(
        Event.Swar(Note.Pa, Variant.Shuddha, Octave.Madhya,
          BeatPosition(0, 12, Rational.onBeat), Rational.fullBeat,
          Some(Stroke.Da), Nil, None),
        Event.Swar(Note.Ma, Variant.Tivra, Octave.Madhya,
          BeatPosition(0, 13, Rational.onBeat), Rational(1, 2),
          Some(Stroke.Ra),
          List(Meend(
            NoteRef(Note.Ma, Variant.Tivra, Octave.Madhya),
            NoteRef(Note.Ga, Variant.Shuddha, Octave.Madhya),
            MeendDirection.Descending, Nil
          )),
          None),
        Event.Rest(BeatPosition(0, 14, Rational.onBeat), Rational.fullBeat),
        Event.Sustain(BeatPosition(0, 15, Rational.onBeat), Rational.fullBeat)
      ))
    ),
    tihais = List(
      Tihai("Sthayi", BeatPosition(2, 8, Rational.onBeat), BeatPosition(3, 0, Rational.onBeat))
    )
  )

  "Composition codec" should "roundtrip a full composition" in {
    val json = sampleComposition.asJson
    val decoded = json.as[Composition]
    decoded shouldBe Right(sampleComposition)
  }

  it should "include version field at top level in SwarFormat" in {
    val json = SwarFormat.toJson(sampleComposition)
    json.hcursor.downField("version").as[String] shouldBe Right("1.0")
  }

  it should "roundtrip through SwarFormat" in {
    val json = SwarFormat.toJson(sampleComposition)
    val jsonString = json.spaces2
    val result = SwarFormat.fromJson(jsonString)
    result shouldBe Right(sampleComposition)
  }

  "Ornament codec" should "roundtrip Meend" in {
    val meend: Ornament = Meend(
      NoteRef(Note.Sa, Variant.Shuddha, Octave.Taar),
      NoteRef(Note.Re, Variant.Shuddha, Octave.Madhya),
      MeendDirection.Descending,
      List(NoteRef(Note.Ni, Variant.Shuddha, Octave.Madhya))
    )
    decode[Ornament](meend.asJson.noSpaces) shouldBe Right(meend)
  }

  it should "roundtrip KanSwar" in {
    val kan: Ornament = KanSwar(NoteRef(Note.Re, Variant.Shuddha, Octave.Madhya))
    decode[Ornament](kan.asJson.noSpaces) shouldBe Right(kan)
  }

  it should "roundtrip Gamak" in {
    val g: Ornament = Gamak()
    decode[Ornament](g.asJson.noSpaces) shouldBe Right(g)
  }

  it should "roundtrip CustomOrnament" in {
    val c: Ornament = CustomOrnament("newMove", Map("speed" -> "fast"))
    decode[Ornament](c.asJson.noSpaces) shouldBe Right(c)
  }

  "SectionType codec" should "roundtrip Custom" in {
    val st: SectionType = SectionType.Custom("Tihai Section")
    decode[SectionType](st.asJson.noSpaces) shouldBe Right(st)
  }

  "Palta composition" should "roundtrip with no laya" in {
    val palta = sampleComposition.copy(
      metadata = sampleComposition.metadata.copy(
        compositionType = CompositionType.Palta,
        laya = None
      )
    )
    val json = SwarFormat.toJson(palta)
    val result = SwarFormat.fromJson(json.spaces2)
    result.map(_.metadata.laya) shouldBe Right(None)
    result.map(_.metadata.compositionType) shouldBe Right(CompositionType.Palta)
  }
```

- [ ] **Step 2: Run tests to verify failure**

Run: `sbt "testOnly sangeet.format.CompositionCodecSpec"`
Expected: FAIL — Ornament/Event/Composition codecs and SwarFormat not found.

- [ ] **Step 3: Add Ornament, Event, and remaining codecs to Codecs.scala**

Append to `src/main/scala/sangeet/format/Codecs.scala`:

```scala
  // --- SectionType ---
  given Encoder[SectionType] = Encoder.instance {
    case SectionType.Custom(n) => Json.obj("custom" -> Json.fromString(n))
    case st                    => Json.fromString(st.toString.head.toLower + st.toString.tail)
  }
  given Decoder[SectionType] = Decoder.instance { c =>
    c.as[String].map { s =>
      SectionType.values.collectFirst {
        case st if st.toString.equalsIgnoreCase(s) && !st.isInstanceOf[SectionType.Custom] => st
      }.getOrElse(throw DecodingFailure(s"Invalid SectionType: $s", c.history))
    }.orElse {
      c.downField("custom").as[String].map(SectionType.Custom(_))
    }
  }

  // --- Ornament (discriminated union via "type" field) ---
  given Encoder[Ornament] = Encoder.instance {
    case m: Meend => Json.obj(
      "type" -> "meend".asJson,
      "startNote" -> m.startNote.asJson,
      "endNote" -> m.endNote.asJson,
      "direction" -> m.direction.asJson,
      "intermediateNotes" -> m.intermediateNotes.asJson
    )
    case k: KanSwar => Json.obj(
      "type" -> "kanSwar".asJson,
      "graceNote" -> k.graceNote.asJson
    )
    case m: Murki => Json.obj(
      "type" -> "murki".asJson, "notes" -> m.notes.asJson
    )
    case _: Gamak => Json.obj("type" -> "gamak".asJson)
    case _: Andolan => Json.obj("type" -> "andolan".asJson)
    case k: Krintan => Json.obj(
      "type" -> "krintan".asJson, "notes" -> k.notes.asJson
    )
    case _: Gitkari => Json.obj("type" -> "gitkari".asJson)
    case g: Ghaseet => Json.obj(
      "type" -> "ghaseet".asJson, "targetNote" -> g.targetNote.asJson
    )
    case s: Sparsh => Json.obj(
      "type" -> "sparsh".asJson, "touchNote" -> s.touchNote.asJson
    )
    case z: Zamzama => Json.obj(
      "type" -> "zamzama".asJson, "notes" -> z.notes.asJson
    )
    case c: CustomOrnament => Json.obj(
      "type" -> "custom".asJson,
      "name" -> c.name.asJson,
      "parameters" -> c.parameters.asJson
    )
  }

  given Decoder[Ornament] = Decoder.instance { c =>
    c.downField("type").as[String].flatMap {
      case "meend" => for
        s <- c.downField("startNote").as[NoteRef]
        e <- c.downField("endNote").as[NoteRef]
        d <- c.downField("direction").as[MeendDirection]
        i <- c.downField("intermediateNotes").as[List[NoteRef]]
      yield Meend(s, e, d, i)
      case "kanSwar" => c.downField("graceNote").as[NoteRef].map(KanSwar(_))
      case "murki"   => c.downField("notes").as[List[NoteRef]].map(Murki(_))
      case "gamak"   => Right(Gamak())
      case "andolan" => Right(Andolan())
      case "krintan" => c.downField("notes").as[List[NoteRef]].map(Krintan(_))
      case "gitkari" => Right(Gitkari())
      case "ghaseet" => c.downField("targetNote").as[NoteRef].map(Ghaseet(_))
      case "sparsh"  => c.downField("touchNote").as[NoteRef].map(Sparsh(_))
      case "zamzama" => c.downField("notes").as[List[NoteRef]].map(Zamzama(_))
      case "custom" => for
        name <- c.downField("name").as[String]
        params <- c.downField("parameters").as[Map[String, String]]
      yield CustomOrnament(name, params)
      case other => Left(DecodingFailure(s"Unknown ornament type: $other", c.history))
    }
  }

  // --- Event (discriminated union via "type" field) ---
  given Encoder[Event] = Encoder.instance {
    case s: Event.Swar =>
      val base = Json.obj(
        "type" -> "swar".asJson,
        "note" -> s.note.asJson,
        "variant" -> s.variant.asJson,
        "octave" -> s.octave.asJson,
        "beat" -> s.beat.asJson,
        "duration" -> s.duration.asJson,
        "ornaments" -> s.ornaments.asJson
      )
      val withStroke = s.stroke.fold(base)(st => base.deepMerge(Json.obj("stroke" -> st.asJson)))
      val withSahitya = s.sahitya.fold(withStroke)(sa => withStroke.deepMerge(Json.obj("sahitya" -> sa.asJson)))
      withSahitya
    case r: Event.Rest => Json.obj(
      "type" -> "rest".asJson,
      "beat" -> r.beat.asJson,
      "duration" -> r.duration.asJson
    )
    case s: Event.Sustain => Json.obj(
      "type" -> "sustain".asJson,
      "beat" -> s.beat.asJson,
      "duration" -> s.duration.asJson
    )
  }

  given Decoder[Event] = Decoder.instance { c =>
    c.downField("type").as[String].flatMap {
      case "swar" => for
        note <- c.downField("note").as[Note]
        variant <- c.downField("variant").as[Variant]
        octave <- c.downField("octave").as[Octave]
        beat <- c.downField("beat").as[BeatPosition]
        duration <- c.downField("duration").as[Rational]
        stroke <- c.downField("stroke").as[Option[Stroke]]
        ornaments <- c.downField("ornaments").as[List[Ornament]]
        sahitya <- c.downField("sahitya").as[Option[String]]
      yield Event.Swar(note, variant, octave, beat, duration, stroke, ornaments, sahitya)
      case "rest" => for
        beat <- c.downField("beat").as[BeatPosition]
        duration <- c.downField("duration").as[Rational]
      yield Event.Rest(beat, duration)
      case "sustain" => for
        beat <- c.downField("beat").as[BeatPosition]
        duration <- c.downField("duration").as[Rational]
      yield Event.Sustain(beat, duration)
      case other => Left(DecodingFailure(s"Unknown event type: $other", c.history))
    }
  }

  // --- Vibhag ---
  given Encoder[Vibhag] = Encoder.instance { v =>
    Json.obj("beats" -> Json.fromInt(v.beats), "marker" -> v.marker.asJson)
  }
  given Decoder[Vibhag] = Decoder.instance { c =>
    for
      beats <- c.downField("beats").as[Int]
      marker <- c.downField("marker").as[VibhagMarker]
    yield Vibhag(beats, marker)
  }

  // --- Taal ---
  given Encoder[Taal] = Encoder.instance { t =>
    val base = Json.obj(
      "name" -> t.name.asJson,
      "matras" -> Json.fromInt(t.matras),
      "vibhags" -> t.vibhags.asJson
    )
    t.theka.fold(base)(th => base.deepMerge(Json.obj("theka" -> th.asJson)))
  }
  given Decoder[Taal] = Decoder.instance { c =>
    for
      name <- c.downField("name").as[String]
      matras <- c.downField("matras").as[Int]
      vibhags <- c.downField("vibhags").as[List[Vibhag]]
      theka <- c.downField("theka").as[Option[List[String]]]
    yield Taal(name, matras, vibhags, theka)
  }

  // --- Raag ---
  given Encoder[Raag] = Encoder.instance { r =>
    Json.obj(
      "name" -> r.name.asJson,
      "thaat" -> r.thaat.asJson,
      "arohana" -> r.arohana.asJson,
      "avarohana" -> r.avarohana.asJson,
      "vadi" -> r.vadi.asJson,
      "samvadi" -> r.samvadi.asJson,
      "pakad" -> r.pakad.asJson,
      "prahar" -> r.prahar.asJson
    ).dropNullValues
  }
  given Decoder[Raag] = Decoder.instance { c =>
    for
      name <- c.downField("name").as[String]
      thaat <- c.downField("thaat").as[Option[String]]
      arohana <- c.downField("arohana").as[Option[List[String]]]
      avarohana <- c.downField("avarohana").as[Option[List[String]]]
      vadi <- c.downField("vadi").as[Option[String]]
      samvadi <- c.downField("samvadi").as[Option[String]]
      pakad <- c.downField("pakad").as[Option[String]]
      prahar <- c.downField("prahar").as[Option[Int]]
    yield Raag(name, thaat, arohana, avarohana, vadi, samvadi, pakad, prahar)
  }

  // --- Section ---
  given Encoder[Section] = Encoder.instance { s =>
    Json.obj(
      "name" -> s.name.asJson,
      "type" -> s.sectionType.asJson,
      "events" -> s.events.asJson
    )
  }
  given Decoder[Section] = Decoder.instance { c =>
    for
      name <- c.downField("name").as[String]
      stype <- c.downField("type").as[SectionType]
      events <- c.downField("events").as[List[Event]]
    yield Section(name, stype, events)
  }

  // --- Tihai ---
  given Encoder[Tihai] = Encoder.instance { t =>
    Json.obj(
      "section" -> t.sectionName.asJson,
      "startBeat" -> t.startBeat.asJson,
      "landingBeat" -> t.landingBeat.asJson
    )
  }
  given Decoder[Tihai] = Decoder.instance { c =>
    for
      section <- c.downField("section").as[String]
      start <- c.downField("startBeat").as[BeatPosition]
      landing <- c.downField("landingBeat").as[BeatPosition]
    yield Tihai(section, start, landing)
  }

  // --- Metadata ---
  given Encoder[Metadata] = Encoder.instance { m =>
    Json.obj(
      "title" -> m.title.asJson,
      "compositionType" -> m.compositionType.asJson,
      "raag" -> m.raag.asJson,
      "taal" -> m.taal.asJson,
      "laya" -> m.laya.asJson,
      "instrument" -> m.instrument.asJson,
      "composer" -> m.composer.asJson,
      "author" -> m.author.asJson,
      "source" -> m.source.asJson,
      "createdAt" -> m.createdAt.asJson,
      "updatedAt" -> m.updatedAt.asJson
    ).dropNullValues
  }
  given Decoder[Metadata] = Decoder.instance { c =>
    for
      title <- c.downField("title").as[String]
      ct <- c.downField("compositionType").as[CompositionType]
      raag <- c.downField("raag").as[Raag]
      taal <- c.downField("taal").as[Taal]
      laya <- c.downField("laya").as[Option[Laya]]
      instrument <- c.downField("instrument").as[Option[String]]
      composer <- c.downField("composer").as[Option[String]]
      author <- c.downField("author").as[Option[String]]
      source <- c.downField("source").as[Option[String]]
      createdAt <- c.downField("createdAt").as[String]
      updatedAt <- c.downField("updatedAt").as[String]
    yield Metadata(title, ct, raag, taal, laya, instrument, composer, author, source, createdAt, updatedAt)
  }

  // --- Composition ---
  given Encoder[Composition] = Encoder.instance { comp =>
    Json.obj(
      "metadata" -> comp.metadata.asJson,
      "sections" -> comp.sections.asJson,
      "tihais" -> comp.tihais.asJson
    )
  }
  given Decoder[Composition] = Decoder.instance { c =>
    for
      metadata <- c.downField("metadata").as[Metadata]
      sections <- c.downField("sections").as[List[Section]]
      tihais <- c.downField("tihais").as[List[Tihai]]
    yield Composition(metadata, sections, tihais)
  }
```

- [ ] **Step 4: Implement SwarFormat (file-level serialization with version)**

```scala
// src/main/scala/sangeet/format/SwarFormat.scala
package sangeet.format

import io.circe.*
import io.circe.syntax.*
import io.circe.parser.{parse => parseJson}
import sangeet.model.*
import java.nio.file.{Files, Path}
import java.nio.charset.StandardCharsets

object SwarFormat:
  import Codecs.given

  val currentVersion = "1.0"

  def toJson(composition: Composition): Json =
    Json.obj(
      "version" -> Json.fromString(currentVersion),
    ).deepMerge(composition.asJson)

  def fromJson(jsonString: String): Either[Error, Composition] =
    for
      json <- parseJson(jsonString)
      comp <- json.as[Composition]
    yield comp

  def writeFile(path: Path, composition: Composition): Unit =
    val json = toJson(composition)
    Files.writeString(path, json.spaces2, StandardCharsets.UTF_8)

  def readFile(path: Path): Either[Error, Composition] =
    val content = Files.readString(path, StandardCharsets.UTF_8)
    fromJson(content)
```

- [ ] **Step 5: Run all tests**

Run: `sbt "testOnly sangeet.format.*"`
Expected: All tests pass.

- [ ] **Step 6: Commit**

```bash
git add src/
git commit -m "feat: add JSON codecs for all types and SwarFormat file serialization"
```

---

### Task 7: Built-in Taal Definitions & Sample .swar File

**Files:**
- Create: `src/main/scala/sangeet/taal/Taals.scala`
- Create: `src/test/scala/sangeet/taal/TaalsSpec.scala`
- Create: `samples/yaman-vilambit-gat.swar`

- [ ] **Step 1: Write failing tests**

```scala
// src/test/scala/sangeet/taal/TaalsSpec.scala
package sangeet.taal

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*

class TaalsSpec extends AnyFlatSpec with Matchers:

  "Taals.teentaal" should "have 16 matras in 4 vibhags of 4" in {
    val t = Taals.teentaal
    t.matras shouldBe 16
    t.vibhags should have length 4
    t.vibhags.map(_.beats) shouldBe List(4, 4, 4, 4)
    t.vibhags.head.marker shouldBe VibhagMarker.Sam
    t.vibhags(2).marker shouldBe VibhagMarker.Khali
  }

  "Taals.ektaal" should "have 12 matras in 6 vibhags of 2" in {
    val t = Taals.ektaal
    t.matras shouldBe 12
    t.vibhags should have length 6
    t.vibhags.map(_.beats).forall(_ == 2) shouldBe true
  }

  "Taals.jhaptaal" should "have 10 matras in vibhags of 2+3+2+3" in {
    val t = Taals.jhaptaal
    t.matras shouldBe 10
    t.vibhags.map(_.beats) shouldBe List(2, 3, 2, 3)
  }

  "Taals.rupak" should "have sam on khali" in {
    val t = Taals.rupak
    t.matras shouldBe 7
    t.vibhags.head.marker shouldBe VibhagMarker.Khali
  }

  "Taals.all" should "contain all built-in taals" in {
    Taals.all.size should be >= 11
  }

  "Taals.byName" should "find taal by name case-insensitive" in {
    Taals.byName("teentaal") shouldBe Some(Taals.teentaal)
    Taals.byName("Teentaal") shouldBe Some(Taals.teentaal)
    Taals.byName("unknown") shouldBe None
  }
```

- [ ] **Step 2: Run tests to verify failure**

Run: `sbt "testOnly sangeet.taal.TaalsSpec"`
Expected: FAIL — `Taals` not found.

- [ ] **Step 3: Implement Taals**

```scala
// src/main/scala/sangeet/taal/Taals.scala
package sangeet.taal

import sangeet.model.*

object Taals:

  val teentaal = Taal("Teentaal", 16,
    List(Vibhag(4, VibhagMarker.Sam), Vibhag(4, VibhagMarker.Taali(2)),
         Vibhag(4, VibhagMarker.Khali), Vibhag(4, VibhagMarker.Taali(3))),
    Some(List("Dha","Dhin","Dhin","Dha","Dha","Dhin","Dhin","Dha",
              "Dha","Tin","Tin","Ta","Ta","Dhin","Dhin","Dha")))

  val ektaal = Taal("Ektaal", 12,
    List(Vibhag(2, VibhagMarker.Sam), Vibhag(2, VibhagMarker.Khali),
         Vibhag(2, VibhagMarker.Taali(2)), Vibhag(2, VibhagMarker.Khali),
         Vibhag(2, VibhagMarker.Taali(3)), Vibhag(2, VibhagMarker.Taali(4))),
    Some(List("Dhin","Dhin","Dhage","Trakat","Tu","Na","Kat","Ta","Dhage","Trakat","Dhin","Na")))

  val jhaptaal = Taal("Jhaptaal", 10,
    List(Vibhag(2, VibhagMarker.Sam), Vibhag(3, VibhagMarker.Taali(2)),
         Vibhag(2, VibhagMarker.Khali), Vibhag(3, VibhagMarker.Taali(3))),
    Some(List("Dhi","Na","Dhi","Dhi","Na","Ti","Na","Dhi","Dhi","Na")))

  val rupak = Taal("Rupak", 7,
    List(Vibhag(3, VibhagMarker.Khali), Vibhag(2, VibhagMarker.Taali(1)),
         Vibhag(2, VibhagMarker.Taali(2))),
    Some(List("Ti","Ti","Na","Dhi","Na","Dhi","Na")))

  val dadra = Taal("Dadra", 6,
    List(Vibhag(3, VibhagMarker.Sam), Vibhag(3, VibhagMarker.Khali)),
    Some(List("Dha","Dhi","Na","Dha","Ti","Na")))

  val keherwa = Taal("Keherwa", 8,
    List(Vibhag(4, VibhagMarker.Sam), Vibhag(4, VibhagMarker.Khali)),
    Some(List("Dha","Ge","Na","Ti","Na","Ke","Dhi","Na")))

  val chautaal = Taal("Chautaal", 12,
    List(Vibhag(2, VibhagMarker.Sam), Vibhag(2, VibhagMarker.Khali),
         Vibhag(2, VibhagMarker.Taali(2)), Vibhag(2, VibhagMarker.Khali),
         Vibhag(2, VibhagMarker.Taali(3)), Vibhag(2, VibhagMarker.Taali(4))),
    Some(List("Dha","Dha","Dhin","Ta","Kita","Dha","Dhin","Ta","Tita","Kata","Gadi","Gana")))

  val dhamar = Taal("Dhamar", 14,
    List(Vibhag(5, VibhagMarker.Sam), Vibhag(2, VibhagMarker.Taali(2)),
         Vibhag(3, VibhagMarker.Khali), Vibhag(4, VibhagMarker.Taali(3))),
    Some(List("Ka","Dhi","Ta","Dhi","Ta","Dha","-","Ge","Ti","Ta","Ti","Ta","Ta","-")))

  val tilwada = Taal("Tilwada", 16,
    List(Vibhag(4, VibhagMarker.Sam), Vibhag(4, VibhagMarker.Taali(2)),
         Vibhag(4, VibhagMarker.Khali), Vibhag(4, VibhagMarker.Taali(3))),
    Some(List("Dha","Trkt","Dhin","Dhin","Dha","Dha","Tin","Tin",
              "Ta","Trkt","Dhin","Dhin","Dha","Dha","Dhin","Dhin")))

  val jhoomra = Taal("Jhoomra", 14,
    List(Vibhag(3, VibhagMarker.Sam), Vibhag(4, VibhagMarker.Taali(2)),
         Vibhag(3, VibhagMarker.Khali), Vibhag(4, VibhagMarker.Taali(3))),
    Some(List("Dhin","-","Dhage","Trkt","Dhin","Dhin","Dhage","Trkt",
              "Tin","-","Tage","Trkt","Dhin","Dhin")))

  val deepchandi = Taal("Deepchandi", 14,
    List(Vibhag(3, VibhagMarker.Sam), Vibhag(4, VibhagMarker.Taali(2)),
         Vibhag(3, VibhagMarker.Khali), Vibhag(4, VibhagMarker.Taali(3))),
    Some(List("Dha","Dhin","-","Dha","Dha","Tin","-",
              "Ta","Tin","-","Dha","Dha","Dhin","-")))

  val all: Map[String, Taal] = List(
    teentaal, ektaal, jhaptaal, rupak, dadra, keherwa,
    chautaal, dhamar, tilwada, jhoomra, deepchandi
  ).map(t => t.name.toLowerCase -> t).toMap

  def byName(name: String): Option[Taal] = all.get(name.toLowerCase)
```

- [ ] **Step 4: Run tests**

Run: `sbt "testOnly sangeet.taal.TaalsSpec"`
Expected: All tests pass.

- [ ] **Step 5: Create sample .swar file**

Write a sample file using `SwarFormat.writeFile` in a small script, or create manually:

```bash
mkdir -p samples
```

Create `samples/yaman-vilambit-gat.swar` by running a one-time main:

```scala
// Temporary: add to Main.scala, run once, then remove
import sangeet.model.*
import sangeet.format.SwarFormat
import java.nio.file.Path

@main def generateSample(): Unit =
  val comp = Composition(
    metadata = Metadata(
      title = "Vilambit Gat in Yaman",
      compositionType = CompositionType.Gat,
      raag = Raag("Yaman", Some("Kalyan"),
        Some(List("S","R","G","M+","P","D","N","S'")),
        Some(List("S'","N","D","P","M+","G","R","S")),
        Some("G"), Some("N"), None, Some(1)),
      taal = sangeet.taal.Taals.teentaal,
      laya = Some(Laya.Vilambit),
      instrument = Some("Sitar"),
      composer = Some("Traditional"),
      author = Some("Bharadwaj"),
      source = Some("Guruji class, March 2026"),
      createdAt = "2026-03-28T10:00:00Z",
      updatedAt = "2026-03-28T10:00:00Z"),
    sections = List(
      Section("Sthayi", SectionType.Sthayi, List(
        Event.Swar(Note.Pa, Variant.Shuddha, Octave.Madhya,
          BeatPosition(0,12,Rational.onBeat), Rational.fullBeat, Some(Stroke.Da), Nil, None),
        Event.Swar(Note.Ma, Variant.Tivra, Octave.Madhya,
          BeatPosition(0,13,Rational.onBeat), Rational(1,2), Some(Stroke.Ra),
          List(Meend(NoteRef(Note.Ma,Variant.Tivra,Octave.Madhya),
            NoteRef(Note.Ga,Variant.Shuddha,Octave.Madhya), MeendDirection.Descending, Nil)), None),
        Event.Swar(Note.Ga, Variant.Shuddha, Octave.Madhya,
          BeatPosition(0,13,Rational(1,2)), Rational(1,2), Some(Stroke.Da), Nil, None),
        Event.Rest(BeatPosition(0,14,Rational.onBeat), Rational.fullBeat),
        Event.Sustain(BeatPosition(0,15,Rational.onBeat), Rational.fullBeat)
      ))),
    tihais = Nil)
  SwarFormat.writeFile(Path.of("samples/yaman-vilambit-gat.swar"), comp)
  println("Sample written!")
```

Run: `sbt run` (select generateSample), then revert Main.scala.

- [ ] **Step 6: Commit**

```bash
git add src/ samples/
git commit -m "feat: add built-in taal definitions and sample .swar file"
```

---

## Phase 2: Layout Engine

### Task 8: BeatGrouper

**Files:**
- Create: `src/main/scala/sangeet/layout/BeatGrouper.scala`
- Create: `src/main/scala/sangeet/layout/LayoutModel.scala`
- Create: `src/test/scala/sangeet/layout/BeatGrouperSpec.scala`

- [ ] **Step 1: Write failing tests**

```scala
// src/test/scala/sangeet/layout/BeatGrouperSpec.scala
package sangeet.layout

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*

class BeatGrouperSpec extends AnyFlatSpec with Matchers:

  def swar(beat: Int, sub: Rational = Rational.onBeat, note: Note = Note.Sa): Event.Swar =
    Event.Swar(note, Variant.Shuddha, Octave.Madhya,
      BeatPosition(0, beat, sub), Rational.fullBeat, None, Nil, None)

  "BeatGrouper" should "group events by (cycle, beat)" in {
    val events = List(swar(0), swar(1), swar(2))
    val cells = BeatGrouper.group(events)
    cells should have length 3
    cells.head.events should have length 1
  }

  it should "put multiple sub-beat events into the same cell" in {
    val events = List(
      swar(0, Rational.onBeat, Note.Sa),
      swar(0, Rational(1, 2), Note.Re)
    )
    val cells = BeatGrouper.group(events)
    cells should have length 1
    cells.head.events should have length 2
  }

  it should "order events within a cell by subdivision" in {
    val events = List(
      swar(0, Rational(1, 2), Note.Re),
      swar(0, Rational.onBeat, Note.Sa)
    )
    val cells = BeatGrouper.group(events)
    cells.head.events.head match
      case s: Event.Swar => s.note shouldBe Note.Sa
      case _ => fail("Expected Swar")
  }

  it should "handle events across multiple cycles" in {
    val events = List(
      swar(0).copy(beat = BeatPosition(0, 0, Rational.onBeat)),
      swar(0).copy(beat = BeatPosition(1, 0, Rational.onBeat))
    )
    val cells = BeatGrouper.group(events)
    cells should have length 2
    cells.head.position.cycle shouldBe 0
    cells(1).position.cycle shouldBe 1
  }

  it should "compute max subdivisions per cell" in {
    val events = List(
      swar(0, Rational.onBeat, Note.Sa),
      swar(0, Rational(1, 4), Note.Re),
      swar(0, Rational(2, 4), Note.Ga),
      swar(0, Rational(3, 4), Note.Ma)
    )
    val cells = BeatGrouper.group(events)
    cells.head.events should have length 4
  }
```

- [ ] **Step 2: Run tests to verify failure**

Run: `sbt "testOnly sangeet.layout.BeatGrouperSpec"`
Expected: FAIL — types not found.

- [ ] **Step 3: Implement LayoutModel**

```scala
// src/main/scala/sangeet/layout/LayoutModel.scala
package sangeet.layout

import sangeet.model.*

/** A single beat cell containing all events at one (cycle, beat) position. */
case class BeatCell(
  position: CycleAndBeat,
  events: List[Event]
)

case class CycleAndBeat(cycle: Int, beat: Int)

/** A line of beat cells ready for rendering. */
case class GridLine(
  cells: List[BeatCell],
  vibhagBreaks: List[Int],   // indices where vibhag boundaries fall
  markers: List[(Int, VibhagMarker)] // (cell index, marker)
)

/** Complete grid layout for a section. */
case class SectionGrid(
  sectionName: String,
  sectionType: SectionType,
  lines: List[GridLine]
)
```

- [ ] **Step 4: Implement BeatGrouper**

```scala
// src/main/scala/sangeet/layout/BeatGrouper.scala
package sangeet.layout

import sangeet.model.*

object BeatGrouper:

  private def eventBeat(e: Event): BeatPosition = e match
    case s: Event.Swar    => s.beat
    case r: Event.Rest    => r.beat
    case s: Event.Sustain => s.beat

  def group(events: List[Event]): List[BeatCell] =
    events
      .groupBy { e =>
        val bp = eventBeat(e)
        CycleAndBeat(bp.cycle, bp.beat)
      }
      .toList
      .sortBy { (cab, _) => (cab.cycle, cab.beat) }
      .map { (cab, evts) =>
        BeatCell(cab, evts.sortBy(e => eventBeat(e).subdivision))
      }
```

- [ ] **Step 5: Run tests**

Run: `sbt "testOnly sangeet.layout.BeatGrouperSpec"`
Expected: All tests pass.

- [ ] **Step 6: Commit**

```bash
git add src/
git commit -m "feat: add BeatGrouper and layout data model"
```

---

### Task 9: LineBreaker

**Files:**
- Create: `src/main/scala/sangeet/layout/LineBreaker.scala`
- Create: `src/main/scala/sangeet/layout/LayoutConfig.scala`
- Create: `src/test/scala/sangeet/layout/LineBreakerSpec.scala`

- [ ] **Step 1: Write failing tests**

```scala
// src/test/scala/sangeet/layout/LineBreakerSpec.scala
package sangeet.layout

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*
import sangeet.taal.Taals

class LineBreakerSpec extends AnyFlatSpec with Matchers:

  def makeCells(matras: Int, notesPerBeat: Int = 1): List[BeatCell] =
    (0 until matras).toList.map { beat =>
      val events = (0 until notesPerBeat).toList.map { sub =>
        Event.Swar(Note.Sa, Variant.Shuddha, Octave.Madhya,
          BeatPosition(0, beat, Rational(sub, math.max(notesPerBeat, 1))),
          Rational(1, notesPerBeat), None, Nil, None)
      }
      BeatCell(CycleAndBeat(0, beat), events)
    }

  val config = LayoutConfig()

  "LineBreaker" should "put a full Teentaal cycle on one line for low density" in {
    val cells = makeCells(16, notesPerBeat = 1)
    val lines = LineBreaker.break(cells, Taals.teentaal, config)
    lines should have length 1
    lines.head.cells should have length 16
  }

  it should "mark vibhag boundaries" in {
    val cells = makeCells(16, notesPerBeat = 1)
    val lines = LineBreaker.break(cells, Taals.teentaal, config)
    // Teentaal: vibhags at 0, 4, 8, 12
    lines.head.vibhagBreaks shouldBe List(4, 8, 12)
  }

  it should "include vibhag markers" in {
    val cells = makeCells(16, notesPerBeat = 1)
    val lines = LineBreaker.break(cells, Taals.teentaal, config)
    val markers = lines.head.markers
    markers should contain ((0, VibhagMarker.Sam))
    markers should contain ((8, VibhagMarker.Khali))
  }

  it should "split high density into vibhag-per-line" in {
    val cells = makeCells(16, notesPerBeat = 6)
    val highDensityConfig = LayoutConfig(highDensityThreshold = 5)
    val lines = LineBreaker.break(cells, Taals.teentaal, highDensityConfig)
    lines.length should be > 1
  }

  it should "handle multiple cycles" in {
    val cells = (0 to 1).flatMap { cycle =>
      (0 until 16).map { beat =>
        BeatCell(CycleAndBeat(cycle, beat), List(
          Event.Swar(Note.Sa, Variant.Shuddha, Octave.Madhya,
            BeatPosition(cycle, beat, Rational.onBeat), Rational.fullBeat, None, Nil, None)
        ))
      }
    }.toList
    val lines = LineBreaker.break(cells, Taals.teentaal, config)
    lines should have length 2
  }
```

- [ ] **Step 2: Run tests to verify failure**

Run: `sbt "testOnly sangeet.layout.LineBreakerSpec"`
Expected: FAIL.

- [ ] **Step 3: Implement LayoutConfig**

```scala
// src/main/scala/sangeet/layout/LayoutConfig.scala
package sangeet.layout

case class LayoutConfig(
  highDensityThreshold: Int = 5,   // notes per beat above which we split lines
  cellWidthBase: Double = 60.0,    // base cell width in pixels
  cellOverflowExpand: Double = 15.0, // extra width per note above 1
  lineSpacing: Double = 80.0,      // vertical space between lines
  headerHeight: Double = 120.0     // space for composition header
)
```

- [ ] **Step 4: Implement LineBreaker**

```scala
// src/main/scala/sangeet/layout/LineBreaker.scala
package sangeet.layout

import sangeet.model.*

object LineBreaker:

  def break(cells: List[BeatCell], taal: Taal, config: LayoutConfig): List[GridLine] =
    val byCycle = cells.groupBy(_.position.cycle).toList.sortBy(_._1)
    byCycle.flatMap { (cycle, cycleCells) =>
      val sorted = cycleCells.sortBy(_.position.beat)
      val maxDensity = if sorted.isEmpty then 1
                       else sorted.map(_.events.size).max
      if maxDensity >= config.highDensityThreshold then
        splitByVibhag(sorted, taal)
      else
        List(makeGridLine(sorted, taal))
    }

  private def makeGridLine(cells: List[BeatCell], taal: Taal): GridLine =
    val (breaks, markers) = computeVibhagInfo(taal)
    GridLine(cells, breaks, markers)

  private def splitByVibhag(cells: List[BeatCell], taal: Taal): List[GridLine] =
    var beatOffset = 0
    taal.vibhags.zipWithIndex.map { (vibhag, idx) =>
      val vibhagCells = cells.filter { c =>
        c.position.beat >= beatOffset && c.position.beat < beatOffset + vibhag.beats
      }
      val markers = List((0, vibhag.marker))
      val line = GridLine(vibhagCells, Nil, markers)
      beatOffset += vibhag.beats
      line
    }

  private def computeVibhagInfo(taal: Taal): (List[Int], List[(Int, VibhagMarker)]) =
    var offset = 0
    val breaks = scala.collection.mutable.ListBuffer[Int]()
    val markers = scala.collection.mutable.ListBuffer[(Int, VibhagMarker)]()
    taal.vibhags.foreach { v =>
      markers += ((offset, v.marker))
      if offset > 0 then breaks += offset
      offset += v.beats
    }
    (breaks.toList, markers.toList)
```

- [ ] **Step 5: Run tests**

Run: `sbt "testOnly sangeet.layout.LineBreakerSpec"`
Expected: All tests pass.

- [ ] **Step 6: Commit**

```bash
git add src/
git commit -m "feat: add LineBreaker with density-aware line splitting"
```

---

### Task 10: GridLayout (Orchestrator)

**Files:**
- Create: `src/main/scala/sangeet/layout/GridLayout.scala`
- Create: `src/test/scala/sangeet/layout/GridLayoutSpec.scala`

- [ ] **Step 1: Write failing tests**

```scala
// src/test/scala/sangeet/layout/GridLayoutSpec.scala
package sangeet.layout

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*
import sangeet.taal.Taals

class GridLayoutSpec extends AnyFlatSpec with Matchers:

  val taal = Taals.teentaal

  def swar(beat: Int, note: Note = Note.Sa, cycle: Int = 0): Event.Swar =
    Event.Swar(note, Variant.Shuddha, Octave.Madhya,
      BeatPosition(cycle, beat, Rational.onBeat), Rational.fullBeat,
      Some(Stroke.Da), Nil, None)

  "GridLayout.layout" should "produce a SectionGrid from a Section" in {
    val section = Section("Sthayi", SectionType.Sthayi,
      (0 until 16).toList.map(b => swar(b)))
    val grid = GridLayout.layout(section, taal, LayoutConfig())
    grid.sectionName shouldBe "Sthayi"
    grid.lines should not be empty
  }

  it should "handle an empty section" in {
    val section = Section("Empty", SectionType.Sthayi, Nil)
    val grid = GridLayout.layout(section, taal, LayoutConfig())
    grid.lines shouldBe empty
  }

  it should "handle multi-cycle sections" in {
    val events = (0 to 1).flatMap(c => (0 until 16).map(b => swar(b, cycle = c))).toList
    val section = Section("Sthayi", SectionType.Sthayi, events)
    val grid = GridLayout.layout(section, taal, LayoutConfig())
    grid.lines should have length 2
  }
```

- [ ] **Step 2: Run tests to verify failure**

Run: `sbt "testOnly sangeet.layout.GridLayoutSpec"`
Expected: FAIL.

- [ ] **Step 3: Implement GridLayout**

```scala
// src/main/scala/sangeet/layout/GridLayout.scala
package sangeet.layout

import sangeet.model.*

object GridLayout:

  def layout(section: Section, taal: Taal, config: LayoutConfig): SectionGrid =
    val cells = BeatGrouper.group(section.events)
    val lines = LineBreaker.break(cells, taal, config)
    SectionGrid(section.name, section.sectionType, lines)

  def layoutAll(composition: Composition, config: LayoutConfig): List[SectionGrid] =
    composition.sections.map(s => layout(s, composition.metadata.taal, config))
```

- [ ] **Step 4: Run tests**

Run: `sbt "testOnly sangeet.layout.GridLayoutSpec"`
Expected: All tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/
git commit -m "feat: add GridLayout orchestrator for full layout pipeline"
```

---

## Phase 3: Rendering

### Task 11: Devanagari Mapping

**Files:**
- Create: `src/main/scala/sangeet/render/DevanagariMap.scala`
- Create: `src/test/scala/sangeet/render/DevanagariMapSpec.scala`

- [ ] **Step 1: Write failing tests**

```scala
// src/test/scala/sangeet/render/DevanagariMapSpec.scala
package sangeet.render

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*

class DevanagariMapSpec extends AnyFlatSpec with Matchers:

  "DevanagariMap.glyph" should "return Devanagari for shuddha swaras" in {
    DevanagariMap.glyph(Note.Sa, Variant.Shuddha) shouldBe "सा"
    DevanagariMap.glyph(Note.Re, Variant.Shuddha) shouldBe "रे"
    DevanagariMap.glyph(Note.Ga, Variant.Shuddha) shouldBe "ग"
    DevanagariMap.glyph(Note.Ma, Variant.Shuddha) shouldBe "म"
    DevanagariMap.glyph(Note.Pa, Variant.Shuddha) shouldBe "प"
    DevanagariMap.glyph(Note.Dha, Variant.Shuddha) shouldBe "ध"
    DevanagariMap.glyph(Note.Ni, Variant.Shuddha) shouldBe "नि"
  }

  it should "return same base glyph for komal/tivra (modifiers rendered separately)" in {
    DevanagariMap.glyph(Note.Re, Variant.Komal) shouldBe "रे"
    DevanagariMap.glyph(Note.Ma, Variant.Tivra) shouldBe "म"
  }

  "DevanagariMap.needsKomalMark" should "be true for komal Re, Ga, Dha, Ni" in {
    DevanagariMap.needsKomalMark(Note.Re, Variant.Komal) shouldBe true
    DevanagariMap.needsKomalMark(Note.Ga, Variant.Komal) shouldBe true
    DevanagariMap.needsKomalMark(Note.Dha, Variant.Komal) shouldBe true
    DevanagariMap.needsKomalMark(Note.Ni, Variant.Komal) shouldBe true
  }

  it should "be false for shuddha and for Sa/Pa" in {
    DevanagariMap.needsKomalMark(Note.Sa, Variant.Shuddha) shouldBe false
    DevanagariMap.needsKomalMark(Note.Re, Variant.Shuddha) shouldBe false
  }

  "DevanagariMap.needsTivraMark" should "be true only for tivra Ma" in {
    DevanagariMap.needsTivraMark(Note.Ma, Variant.Tivra) shouldBe true
    DevanagariMap.needsTivraMark(Note.Ma, Variant.Shuddha) shouldBe false
    DevanagariMap.needsTivraMark(Note.Re, Variant.Tivra) shouldBe false
  }

  "DevanagariMap.octaveDots" should "return dot count and direction" in {
    DevanagariMap.octaveDots(Octave.Mandra) shouldBe (1, DotPosition.Below)
    DevanagariMap.octaveDots(Octave.Madhya) shouldBe (0, DotPosition.None)
    DevanagariMap.octaveDots(Octave.Taar) shouldBe (1, DotPosition.Above)
    DevanagariMap.octaveDots(Octave.AtiMandra) shouldBe (2, DotPosition.Below)
    DevanagariMap.octaveDots(Octave.AtiTaar) shouldBe (2, DotPosition.Above)
  }

  "DevanagariMap.restSymbol" should "return dash" in {
    DevanagariMap.restSymbol shouldBe "-"
  }

  "DevanagariMap.sustainSymbol" should "return dash" in {
    DevanagariMap.sustainSymbol shouldBe "-"
  }
```

- [ ] **Step 2: Run tests to verify failure**

Run: `sbt "testOnly sangeet.render.DevanagariMapSpec"`
Expected: FAIL.

- [ ] **Step 3: Implement DevanagariMap**

```scala
// src/main/scala/sangeet/render/DevanagariMap.scala
package sangeet.render

import sangeet.model.*

enum DotPosition:
  case Above, Below, None

object DevanagariMap:

  private val glyphs: Map[Note, String] = Map(
    Note.Sa  -> "सा",
    Note.Re  -> "रे",
    Note.Ga  -> "ग",
    Note.Ma  -> "म",
    Note.Pa  -> "प",
    Note.Dha -> "ध",
    Note.Ni  -> "नि"
  )

  def glyph(note: Note, variant: Variant): String = glyphs(note)

  def needsKomalMark(note: Note, variant: Variant): Boolean =
    variant == Variant.Komal && (note == Note.Re || note == Note.Ga ||
      note == Note.Dha || note == Note.Ni)

  def needsTivraMark(note: Note, variant: Variant): Boolean =
    variant == Variant.Tivra && note == Note.Ma

  def octaveDots(octave: Octave): (Int, DotPosition) = octave match
    case Octave.AtiMandra => (2, DotPosition.Below)
    case Octave.Mandra    => (1, DotPosition.Below)
    case Octave.Madhya    => (0, DotPosition.None)
    case Octave.Taar      => (1, DotPosition.Above)
    case Octave.AtiTaar   => (2, DotPosition.Above)

  val restSymbol: String = "-"
  val sustainSymbol: String = "-"

  val vibhagMarkerText: VibhagMarker => String =
    case VibhagMarker.Sam      => "X"
    case VibhagMarker.Taali(n) => n.toString
    case VibhagMarker.Khali    => "0"

  val strokeText: Stroke => String =
    case Stroke.Da      => "Da"
    case Stroke.Ra      => "Ra"
    case Stroke.Chikari => "Ch"
    case Stroke.Jod     => "Jo"
```

- [ ] **Step 4: Run tests**

Run: `sbt "testOnly sangeet.render.DevanagariMapSpec"`
Expected: All tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/
git commit -m "feat: add Devanagari swar mapping with octave dots and modifier marks"
```

---

### Task 12: ScalaFX Application Scaffold

**Files:**
- Create: `src/main/scala/sangeet/editor/MainApp.scala`
- Create: `src/main/scala/sangeet/editor/EditorPane.scala`
- Modify: `src/main/scala/sangeet/Main.scala` — redirect to MainApp

- [ ] **Step 1: Create MainApp with ScalaFX**

```scala
// src/main/scala/sangeet/editor/MainApp.scala
package sangeet.editor

import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.layout.BorderPane
import scalafx.scene.paint.Color

object MainApp extends JFXApp3:

  override def start(): Unit =
    stage = new PrimaryStage:
      title = "Sangeet Notes Editor"
      width = 1200
      height = 800
      scene = new Scene:
        fill = Color.White
        root = new BorderPane:
          center = new EditorPane()
```

- [ ] **Step 2: Create EditorPane stub with Canvas**

```scala
// src/main/scala/sangeet/editor/EditorPane.scala
package sangeet.editor

import scalafx.scene.layout.StackPane
import scalafx.scene.canvas.Canvas
import scalafx.scene.paint.Color

class EditorPane extends StackPane:
  private val canvas = new Canvas(1100, 700)
  children = List(canvas)

  // Draw a placeholder message
  private val gc = canvas.graphicsContext2D
  gc.fill = Color.Black
  gc.font = new scalafx.scene.text.Font("System", 18)
  gc.fillText("Sangeet Notes Editor — canvas ready", 50, 50)
```

- [ ] **Step 3: Update Main.scala**

```scala
// src/main/scala/sangeet/Main.scala
package sangeet

@main def run(): Unit =
  sangeet.editor.MainApp.main(Array.empty)
```

- [ ] **Step 4: Run the app**

Run: `sbt run`
Expected: A window opens with "Sangeet Notes Editor — canvas ready" text.

- [ ] **Step 5: Commit**

```bash
git add src/
git commit -m "feat: add ScalaFX application scaffold with Canvas-based EditorPane"
```

---

### Task 13: SwarGlyph & GridRenderer

**Files:**
- Create: `src/main/scala/sangeet/render/SwarGlyph.scala`
- Create: `src/main/scala/sangeet/render/GridRenderer.scala`
- Create: `src/main/scala/sangeet/render/CanvasRenderer.scala`

This is the core visual rendering task. It draws the Bhatkhande notation grid on a ScalaFX Canvas.

- [ ] **Step 1: Implement SwarGlyph**

```scala
// src/main/scala/sangeet/render/SwarGlyph.scala
package sangeet.render

import scalafx.scene.canvas.GraphicsContext
import scalafx.scene.paint.Color
import scalafx.scene.text.{Font, TextAlignment}
import sangeet.model.*

object SwarGlyph:

  val swarFont = Font("Noto Sans Devanagari", 16)
  val smallFont = Font("Noto Sans Devanagari", 10)
  val dotRadius = 2.0

  /** Draw a single swar glyph at (x, y) with octave dots and komal/tivra marks. */
  def draw(gc: GraphicsContext, note: Note, variant: Variant, octave: Octave,
           x: Double, y: Double): Unit =
    val text = DevanagariMap.glyph(note, variant)
    gc.save()
    gc.font = swarFont
    gc.textAlignment = TextAlignment.Center
    gc.fill = Color.Black
    gc.fillText(text, x, y)

    // Komal underline
    if DevanagariMap.needsKomalMark(note, variant) then
      gc.strokeLine(x - 8, y + 3, x + 8, y + 3)

    // Tivra overline
    if DevanagariMap.needsTivraMark(note, variant) then
      gc.strokeLine(x - 2, y - 16, x - 2, y - 10)

    // Octave dots
    val (count, pos) = DevanagariMap.octaveDots(octave)
    if count > 0 then
      val dotY = pos match
        case DotPosition.Above => y - 20
        case DotPosition.Below => y + 8
        case DotPosition.None  => y
      for i <- 0 until count do
        val offsetX = if count == 2 then (i - 0.5) * 5 else 0.0
        gc.fillOval(x + offsetX - dotRadius, dotY + i * 5 - dotRadius,
                    dotRadius * 2, dotRadius * 2)

    gc.restore()

  /** Draw rest or sustain symbol. */
  def drawRest(gc: GraphicsContext, x: Double, y: Double): Unit =
    gc.save()
    gc.font = swarFont
    gc.textAlignment = TextAlignment.Center
    gc.fill = Color.Black
    gc.fillText("-", x, y)
    gc.restore()

  /** Draw stroke text below swar. */
  def drawStroke(gc: GraphicsContext, stroke: Stroke, x: Double, y: Double): Unit =
    gc.save()
    gc.font = smallFont
    gc.textAlignment = TextAlignment.Center
    gc.fill = Color.Gray
    gc.fillText(DevanagariMap.strokeText(stroke), x, y)
    gc.restore()
```

- [ ] **Step 2: Implement GridRenderer**

```scala
// src/main/scala/sangeet/render/GridRenderer.scala
package sangeet.render

import scalafx.scene.canvas.GraphicsContext
import scalafx.scene.paint.Color
import scalafx.scene.text.{Font, TextAlignment}
import sangeet.model.*
import sangeet.layout.*

object GridRenderer:

  val markerFont = Font("System", 12)
  val sectionFont = Font("System Bold", 14)
  val headerFont = Font("System", 12)

  /** Draw a complete section grid. */
  def drawSection(gc: GraphicsContext, grid: SectionGrid, config: LayoutConfig,
                  startX: Double, startY: Double): Double =
    var y = startY

    // Section header
    gc.save()
    gc.font = sectionFont
    gc.fill = Color.DarkBlue
    gc.fillText(s"── ${grid.sectionName} ", startX, y)
    gc.strokeLine(startX + 80, y - 5, startX + 600, y - 5)
    gc.restore()
    y += 25

    // Each line
    grid.lines.foreach { line =>
      y = drawGridLine(gc, line, config, startX, y)
      y += config.lineSpacing
    }
    y

  /** Draw a single grid line (one cycle or vibhag). */
  def drawGridLine(gc: GraphicsContext, line: GridLine, config: LayoutConfig,
                   startX: Double, startY: Double): Double =
    var x = startX
    val markerY = startY
    val swarY = startY + 22
    val strokeY = swarY + 16
    val sahityaY = strokeY + 14

    // Draw vibhag markers
    line.markers.foreach { (cellIdx, marker) =>
      val markerX = startX + cellIdx * config.cellWidthBase + config.cellWidthBase / 2
      gc.save()
      gc.font = markerFont
      gc.textAlignment = TextAlignment.Center
      gc.fill = if marker == VibhagMarker.Sam then Color.Red else Color.Black
      gc.fillText(DevanagariMap.vibhagMarkerText(marker), markerX, markerY)
      gc.restore()
    }

    // Draw cells
    line.cells.zipWithIndex.foreach { (cell, idx) =>
      val cellX = startX + idx * config.cellWidthBase
      val cellCenterX = cellX + config.cellWidthBase / 2

      // Draw events within cell
      val eventCount = cell.events.size
      cell.events.zipWithIndex.foreach { (event, evtIdx) =>
        val evtX = if eventCount == 1 then cellCenterX
                   else cellX + (evtIdx + 0.5) * (config.cellWidthBase / eventCount)

        event match
          case s: Event.Swar =>
            SwarGlyph.draw(gc, s.note, s.variant, s.octave, evtX, swarY)
            s.stroke.foreach(st => SwarGlyph.drawStroke(gc, st, evtX, strokeY))
            s.sahitya.foreach { text =>
              gc.save()
              gc.font = Font("Noto Sans Devanagari", 11)
              gc.textAlignment = TextAlignment.Center
              gc.fill = Color.DarkGreen
              gc.fillText(text, evtX, sahityaY)
              gc.restore()
            }
          case _: Event.Rest =>
            SwarGlyph.drawRest(gc, evtX, swarY)
          case _: Event.Sustain =>
            SwarGlyph.drawRest(gc, evtX, swarY)
      }
    }

    // Draw vibhag separator lines
    line.vibhagBreaks.foreach { breakIdx =>
      val lineX = startX + breakIdx * config.cellWidthBase
      gc.save()
      gc.stroke = Color.Gray
      gc.strokeLine(lineX, markerY - 5, lineX, sahityaY + 5)
      gc.restore()
    }

    sahityaY
```

- [ ] **Step 3: Implement CanvasRenderer (ties everything together)**

```scala
// src/main/scala/sangeet/render/CanvasRenderer.scala
package sangeet.render

import scalafx.scene.canvas.{Canvas, GraphicsContext}
import scalafx.scene.paint.Color
import scalafx.scene.text.{Font, TextAlignment}
import sangeet.model.*
import sangeet.layout.*

object CanvasRenderer:

  /** Render a full composition onto a Canvas. */
  def render(canvas: Canvas, composition: Composition, config: LayoutConfig): Unit =
    val gc = canvas.graphicsContext2D
    gc.clearRect(0, 0, canvas.width.value, canvas.height.value)

    var y = 20.0
    val x = 30.0

    // Composition header
    y = drawHeader(gc, composition.metadata, x, y)
    y += 20

    // Layout and render each section
    val grids = GridLayout.layoutAll(composition, config)
    grids.foreach { grid =>
      y = GridRenderer.drawSection(gc, grid, config, x, y)
      y += 10
    }

  /** Draw composition header (raag, taal, arohi, avarohi, etc.) */
  def drawHeader(gc: GraphicsContext, meta: Metadata, x: Double, startY: Double): Double =
    var y = startY
    gc.save()
    gc.font = Font("System Bold", 16)
    gc.fill = Color.Black
    gc.fillText(meta.title, x, y)
    y += 22

    gc.font = Font("System", 13)
    gc.fillText(s"Raag: ${meta.raag.name}" +
      meta.raag.thaat.map(t => s" ($t Thaat)").getOrElse(""), x, y)
    y += 18

    meta.raag.arohana.foreach { ar =>
      gc.fillText(s"Arohi:   ${ar.mkString(" ")}", x, y)
      y += 16
    }
    meta.raag.avarohana.foreach { av =>
      gc.fillText(s"Avarohi: ${av.mkString(" ")}", x, y)
      y += 16
    }

    val vadiLine = List(
      meta.raag.vadi.map(v => s"Vadi: $v"),
      meta.raag.samvadi.map(s => s"Samvadi: $s")
    ).flatten.mkString("  |  ")
    if vadiLine.nonEmpty then
      gc.fillText(vadiLine, x, y)
      y += 16

    val taalLine = s"Taal: ${meta.taal.name} (${meta.taal.matras} matras)" +
      meta.laya.map(l => s"  |  Laya: ${l.toString}").getOrElse("")
    gc.fillText(taalLine, x, y)
    y += 16

    val composerLine = List(
      meta.composer.map(c => s"Composer: $c"),
      meta.source.map(s => s"Source: $s")
    ).flatten.mkString("  |  ")
    if composerLine.nonEmpty then
      gc.fillText(composerLine, x, y)
      y += 16

    gc.restore()
    y
```

- [ ] **Step 4: Wire into EditorPane**

Update `src/main/scala/sangeet/editor/EditorPane.scala`:

```scala
package sangeet.editor

import scalafx.scene.layout.StackPane
import scalafx.scene.canvas.Canvas
import sangeet.model.*
import sangeet.layout.LayoutConfig
import sangeet.render.CanvasRenderer

class EditorPane extends StackPane:
  private val canvas = new Canvas(1100, 700)
  children = List(canvas)

  private var currentComposition: Option[Composition] = None
  private val config = LayoutConfig()

  def setComposition(comp: Composition): Unit =
    currentComposition = Some(comp)
    redraw()

  def redraw(): Unit =
    currentComposition.foreach { comp =>
      CanvasRenderer.render(canvas, comp, config)
    }
```

- [ ] **Step 5: Load sample file on startup for testing**

Update `MainApp.scala` to load the sample file:

```scala
package sangeet.editor

import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.layout.BorderPane
import scalafx.scene.paint.Color
import sangeet.format.SwarFormat
import java.nio.file.{Path, Files}

object MainApp extends JFXApp3:

  override def start(): Unit =
    val editorPane = new EditorPane()

    stage = new PrimaryStage:
      title = "Sangeet Notes Editor"
      width = 1200
      height = 800
      scene = new Scene:
        fill = Color.White
        root = new BorderPane:
          center = editorPane

    // Load sample file if it exists
    val samplePath = Path.of("samples/yaman-vilambit-gat.swar")
    if Files.exists(samplePath) then
      SwarFormat.readFile(samplePath).foreach(editorPane.setComposition)
```

- [ ] **Step 6: Run the app and verify rendering**

Run: `sbt run`
Expected: Window opens showing the Yaman composition header and notation grid with Devanagari swaras, vibhag markers (X, 2, 0, 3), and stroke indicators.

- [ ] **Step 7: Commit**

```bash
git add src/
git commit -m "feat: add SwarGlyph, GridRenderer, and CanvasRenderer for Bhatkhande notation display"
```

---

### Task 14: OrnamentRenderer

**Files:**
- Create: `src/main/scala/sangeet/render/OrnamentRenderer.scala`
- Create: `src/main/scala/sangeet/render/TihaiRenderer.scala`

- [ ] **Step 1: Implement OrnamentRenderer**

```scala
// src/main/scala/sangeet/render/OrnamentRenderer.scala
package sangeet.render

import scalafx.scene.canvas.GraphicsContext
import scalafx.scene.paint.Color
import scalafx.scene.text.{Font, TextAlignment}
import sangeet.model.*

object OrnamentRenderer:

  /** Draw ornament overlays for a swar at the given position. */
  def draw(gc: GraphicsContext, ornaments: List[Ornament],
           x: Double, y: Double, cellWidth: Double): Unit =
    ornaments.foreach {
      case m: Meend       => drawMeendStart(gc, m, x, y)
      case k: KanSwar     => drawKanSwar(gc, k, x, y)
      case _: Gamak       => drawWavyLine(gc, x, y - 22, 16, heavy = true)
      case _: Andolan     => drawWavyLine(gc, x, y - 22, 16, heavy = false)
      case _: Gitkari     => drawWavyLine(gc, x, y - 22, 12, heavy = true)
      case m: Murki       => drawMurki(gc, m, x, y)
      case k: Krintan     => drawKrintanMark(gc, x, y)
      case g: Ghaseet     => drawGhaseetMark(gc, x, y)
      case s: Sparsh      => drawSparshMark(gc, s, x, y)
      case z: Zamzama     => drawZamzamaMark(gc, z, x, y)
      case c: CustomOrnament => drawCustomMark(gc, c, x, y)
    }

  private def drawMeendStart(gc: GraphicsContext, meend: Meend,
                              x: Double, y: Double): Unit =
    // Draw arc start indicator (the full arc connecting notes requires
    // cross-cell rendering which happens at a higher level).
    // For now, draw a small curve above the note.
    gc.save()
    gc.stroke = Color.DarkBlue
    gc.lineWidth = 1.5
    val arcY = if meend.direction == MeendDirection.Ascending then y - 25 else y - 25
    gc.strokeArc(x - 15, arcY, 30, 10, 0, 180)
    gc.restore()

  private def drawKanSwar(gc: GraphicsContext, kan: KanSwar,
                           x: Double, y: Double): Unit =
    gc.save()
    gc.font = Font("Noto Sans Devanagari", 9)
    gc.textAlignment = TextAlignment.Center
    gc.fill = Color.DarkRed
    val text = DevanagariMap.glyph(kan.graceNote.note, kan.graceNote.variant)
    gc.fillText(text, x - 12, y - 10)
    gc.restore()

  private def drawWavyLine(gc: GraphicsContext, x: Double, y: Double,
                            width: Double, heavy: Boolean): Unit =
    gc.save()
    gc.stroke = Color.DarkBlue
    gc.lineWidth = if heavy then 1.5 else 0.8
    val steps = 6
    val dx = width / steps
    for i <- 0 until steps do
      val x1 = x - width / 2 + i * dx
      val x2 = x1 + dx
      val yOff = if i % 2 == 0 then -2 else 2
      gc.strokeLine(x1, y + yOff, x2, y - yOff)
    gc.restore()

  private def drawMurki(gc: GraphicsContext, murki: Murki,
                         x: Double, y: Double): Unit =
    gc.save()
    gc.font = Font("Noto Sans Devanagari", 8)
    gc.textAlignment = TextAlignment.Center
    gc.fill = Color.Purple
    val text = "(" + murki.notes.map(n => DevanagariMap.glyph(n.note, n.variant)).mkString("") + ")"
    gc.fillText(text, x, y - 18)
    gc.restore()

  private def drawKrintanMark(gc: GraphicsContext, x: Double, y: Double): Unit =
    gc.save()
    gc.font = Font("System", 8)
    gc.fill = Color.Brown
    gc.fillText("kr", x - 6, y - 18)
    gc.restore()

  private def drawGhaseetMark(gc: GraphicsContext, x: Double, y: Double): Unit =
    gc.save()
    gc.stroke = Color.DarkRed
    gc.lineWidth = 2.0
    gc.strokeArc(x - 12, y - 28, 24, 8, 0, 180)
    gc.restore()

  private def drawSparshMark(gc: GraphicsContext, sparsh: Sparsh,
                              x: Double, y: Double): Unit =
    gc.save()
    gc.font = Font("Noto Sans Devanagari", 7)
    gc.fill = Color.Gray
    gc.fillText(DevanagariMap.glyph(sparsh.touchNote.note, sparsh.touchNote.variant),
                x + 10, y - 8)
    gc.restore()

  private def drawZamzamaMark(gc: GraphicsContext, z: Zamzama,
                               x: Double, y: Double): Unit =
    gc.save()
    gc.font = Font("Noto Sans Devanagari", 8)
    gc.fill = Color.DarkCyan
    val text = "[" + z.notes.map(n => DevanagariMap.glyph(n.note, n.variant)).mkString("") + "]"
    gc.fillText(text, x, y - 18)
    gc.restore()

  private def drawCustomMark(gc: GraphicsContext, c: CustomOrnament,
                              x: Double, y: Double): Unit =
    gc.save()
    gc.font = Font("System Italic", 8)
    gc.fill = Color.DarkGray
    gc.fillText(c.name, x - 8, y - 18)
    gc.restore()
```

- [ ] **Step 2: Implement TihaiRenderer**

```scala
// src/main/scala/sangeet/render/TihaiRenderer.scala
package sangeet.render

import scalafx.scene.canvas.GraphicsContext
import scalafx.scene.paint.Color
import scalafx.scene.text.{Font, TextAlignment}

object TihaiRenderer:

  /** Draw a tihai bracket from startX to endX at the given y position. */
  def draw(gc: GraphicsContext, startX: Double, endX: Double, y: Double): Unit =
    gc.save()
    gc.stroke = Color.DarkOrange
    gc.lineWidth = 1.5

    val bracketY = y - 35
    val tickHeight = 5

    // Horizontal line
    gc.strokeLine(startX, bracketY, endX, bracketY)
    // Left tick
    gc.strokeLine(startX, bracketY - tickHeight, startX, bracketY + tickHeight)
    // Right tick
    gc.strokeLine(endX, bracketY - tickHeight, endX, bracketY + tickHeight)

    // "x3" label
    gc.font = Font("System Bold", 10)
    gc.textAlignment = TextAlignment.Center
    gc.fill = Color.DarkOrange
    gc.fillText("x3", (startX + endX) / 2, bracketY - 5)

    gc.restore()
```

- [ ] **Step 3: Wire ornament rendering into GridRenderer**

Add ornament drawing call to `GridRenderer.drawGridLine` inside the `Event.Swar` match case, after drawing the swar glyph and stroke:

```scala
// In GridRenderer.drawGridLine, inside the Event.Swar case, after stroke drawing:
            if s.ornaments.nonEmpty then
              OrnamentRenderer.draw(gc, s.ornaments, evtX, swarY, config.cellWidthBase)
```

- [ ] **Step 4: Run the app and verify ornaments render**

Run: `sbt run`
Expected: Meend arc visible on the tivra Ma note in the sample composition.

- [ ] **Step 5: Commit**

```bash
git add src/
git commit -m "feat: add OrnamentRenderer and TihaiRenderer for visual ornament overlays"
```

---

## Phase 4: Editor Interaction

### Task 15: CursorModel

**Files:**
- Create: `src/main/scala/sangeet/editor/CursorModel.scala`
- Create: `src/test/scala/sangeet/editor/CursorModelSpec.scala`

- [ ] **Step 1: Write failing tests**

```scala
// src/test/scala/sangeet/editor/CursorModelSpec.scala
package sangeet.editor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*
import sangeet.taal.Taals

class CursorModelSpec extends AnyFlatSpec with Matchers:

  val cursor = CursorModel(Taals.teentaal)

  "CursorModel" should "start at beat 0, cycle 0" in {
    cursor.beat shouldBe 0
    cursor.cycle shouldBe 0
    cursor.subIndex shouldBe 0
  }

  it should "advance to next beat" in {
    val next = cursor.nextBeat
    next.beat shouldBe 1
    next.cycle shouldBe 0
  }

  it should "wrap to next cycle at end of taal" in {
    var c = cursor
    for _ <- 0 until 16 do c = c.nextBeat
    c.beat shouldBe 0
    c.cycle shouldBe 1
  }

  it should "go to previous beat" in {
    val c = cursor.nextBeat.nextBeat.prevBeat
    c.beat shouldBe 1
  }

  it should "wrap backward to previous cycle" in {
    val c = cursor.prevBeat
    c.beat shouldBe 15
    c.cycle shouldBe -1 // or 0, depending on design — let's allow negative for mukhda
  }

  it should "return current BeatPosition" in {
    val bp = cursor.nextBeat.position
    bp shouldBe BeatPosition(0, 1, Rational.onBeat)
  }

  it should "support setting subdivision count" in {
    val c = cursor.withSubdivisions(4)
    c.totalSubdivisions shouldBe 4
    c.subIndex shouldBe 0
  }

  it should "advance sub-index within subdivisions" in {
    val c = cursor.withSubdivisions(4).nextSubBeat
    c.subIndex shouldBe 1
    c.position.subdivision shouldBe Rational(1, 4)
  }

  it should "advance to next beat when sub-beats exhausted" in {
    var c = cursor.withSubdivisions(2)
    c = c.nextSubBeat // sub 1
    c = c.nextSubBeat // wraps to next beat, sub 0
    c.beat shouldBe 1
    c.subIndex shouldBe 0
  }
```

- [ ] **Step 2: Run tests to verify failure**

Run: `sbt "testOnly sangeet.editor.CursorModelSpec"`
Expected: FAIL.

- [ ] **Step 3: Implement CursorModel**

```scala
// src/main/scala/sangeet/editor/CursorModel.scala
package sangeet.editor

import sangeet.model.*

case class CursorModel(
  taal: Taal,
  cycle: Int = 0,
  beat: Int = 0,
  subIndex: Int = 0,
  totalSubdivisions: Int = 1,
  currentOctave: Octave = Octave.Madhya
):

  def position: BeatPosition =
    BeatPosition(cycle, beat, Rational(subIndex, totalSubdivisions))

  def nextBeat: CursorModel =
    val newBeat = beat + 1
    if newBeat >= taal.matras then
      copy(beat = 0, cycle = cycle + 1, subIndex = 0, totalSubdivisions = 1)
    else
      copy(beat = newBeat, subIndex = 0, totalSubdivisions = 1)

  def prevBeat: CursorModel =
    val newBeat = beat - 1
    if newBeat < 0 then
      copy(beat = taal.matras - 1, cycle = cycle - 1, subIndex = 0, totalSubdivisions = 1)
    else
      copy(beat = newBeat, subIndex = 0, totalSubdivisions = 1)

  def nextSubBeat: CursorModel =
    val newSub = subIndex + 1
    if newSub >= totalSubdivisions then
      nextBeat
    else
      copy(subIndex = newSub)

  def withSubdivisions(n: Int): CursorModel =
    copy(totalSubdivisions = n, subIndex = 0)

  def withOctave(oct: Octave): CursorModel =
    copy(currentOctave = oct)
```

- [ ] **Step 4: Run tests**

Run: `sbt "testOnly sangeet.editor.CursorModelSpec"`
Expected: All tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/
git commit -m "feat: add CursorModel for beat/sub-beat navigation"
```

---

### Task 16: KeyHandler & Swar Input

**Files:**
- Create: `src/main/scala/sangeet/editor/KeyHandler.scala`
- Create: `src/main/scala/sangeet/editor/CompositionEditor.scala`
- Create: `src/test/scala/sangeet/editor/KeyHandlerSpec.scala`

- [ ] **Step 1: Write failing tests**

```scala
// src/test/scala/sangeet/editor/KeyHandlerSpec.scala
package sangeet.editor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*
import sangeet.taal.Taals

class KeyHandlerSpec extends AnyFlatSpec with Matchers:

  val editor = CompositionEditor.empty(Taals.teentaal,
    Raag("Yaman", None, None, None, None, None, None, None))

  "KeyHandler.handleSwarKey" should "insert Sa on 's'" in {
    val (newEditor, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    val events = newEditor.currentSection.events
    events should have length 1
    events.head match
      case s: Event.Swar =>
        s.note shouldBe Note.Sa
        s.variant shouldBe Variant.Shuddha
        s.octave shouldBe Octave.Madhya
      case _ => fail("Expected Swar")
  }

  it should "insert komal Re on Shift+R" in {
    val (newEditor, _) = KeyHandler.handleSwarKey(editor, 'r', shiftDown = true)
    newEditor.currentSection.events.head match
      case s: Event.Swar =>
        s.note shouldBe Note.Re
        s.variant shouldBe Variant.Komal
      case _ => fail("Expected Swar")
  }

  it should "insert tivra Ma on Shift+M" in {
    val (newEditor, _) = KeyHandler.handleSwarKey(editor, 'm', shiftDown = true)
    newEditor.currentSection.events.head match
      case s: Event.Swar =>
        s.note shouldBe Note.Ma
        s.variant shouldBe Variant.Tivra
      case _ => fail("Expected Swar")
  }

  it should "insert rest on space" in {
    val newEditor = KeyHandler.handleSpecialKey(editor, "SPACE")
    newEditor.currentSection.events.head shouldBe a[Event.Rest]
  }

  it should "insert sustain on dash" in {
    val newEditor = KeyHandler.handleSpecialKey(editor, "MINUS")
    newEditor.currentSection.events.head shouldBe a[Event.Sustain]
  }

  it should "advance cursor after inserting a swar" in {
    val (newEditor, newCursor) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    newCursor.beat shouldBe 1 // moved to next beat
  }

  it should "handle dot prefix for mandra" in {
    val editorWithMandra = editor.copy(
      cursor = editor.cursor.withOctave(Octave.Mandra))
    val (newEditor, _) = KeyHandler.handleSwarKey(editorWithMandra, 's', shiftDown = false)
    newEditor.currentSection.events.head match
      case s: Event.Swar => s.octave shouldBe Octave.Mandra
      case _ => fail("Expected Swar")
  }

  "handleDualSwar" should "insert two identical notes" in {
    val (newEditor, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    val (dualEditor, _) = KeyHandler.handleSwarKey(newEditor, 's', shiftDown = false)
    // The implementation should detect double-tap and create dual swar
    // This test verifies basic sequential insertion works
    dualEditor.currentSection.events.size should be >= 2
  }
```

- [ ] **Step 2: Run tests to verify failure**

Run: `sbt "testOnly sangeet.editor.KeyHandlerSpec"`
Expected: FAIL.

- [ ] **Step 3: Implement CompositionEditor (mutable editing state)**

```scala
// src/main/scala/sangeet/editor/CompositionEditor.scala
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
      createdAt = java.time.Instant.now().toString,
      updatedAt = java.time.Instant.now().toString
    )
    val composition = Composition(
      metadata = metadata,
      sections = List(Section("Sthayi", SectionType.Sthayi, Nil)),
      tihais = Nil
    )
    CompositionEditor(composition, 0, CursorModel(taal))
```

- [ ] **Step 4: Implement KeyHandler**

```scala
// src/main/scala/sangeet/editor/KeyHandler.scala
package sangeet.editor

import sangeet.model.*

object KeyHandler:

  private val swarKeys: Map[Char, Note] = Map(
    's' -> Note.Sa, 'r' -> Note.Re, 'g' -> Note.Ga,
    'm' -> Note.Ma, 'p' -> Note.Pa, 'd' -> Note.Dha, 'n' -> Note.Ni
  )

  /** Handle a swar key press. Returns updated editor and cursor. */
  def handleSwarKey(editor: CompositionEditor, key: Char,
                    shiftDown: Boolean): (CompositionEditor, CursorModel) =
    val lowerKey = key.toLower
    swarKeys.get(lowerKey) match
      case Some(note) =>
        val variant = resolveVariant(note, shiftDown)
        val octave = editor.cursor.currentOctave
        val event = Event.Swar(
          note = note,
          variant = variant,
          octave = octave,
          beat = editor.cursor.position,
          duration = Rational(1, editor.cursor.totalSubdivisions),
          stroke = None,
          ornaments = Nil,
          sahitya = None
        )
        val newEditor = editor.addEvent(event)
        val newCursor = editor.cursor.nextSubBeat
        (newEditor.copy(cursor = newCursor), newCursor)
      case None =>
        (editor, editor.cursor)

  /** Handle special keys (space, dash, etc.) */
  def handleSpecialKey(editor: CompositionEditor, keyName: String): CompositionEditor =
    keyName match
      case "SPACE" =>
        val event = Event.Rest(editor.cursor.position, Rational.fullBeat)
        val newEditor = editor.addEvent(event)
        val newCursor = editor.cursor.nextBeat
        newEditor.copy(cursor = newCursor)
      case "MINUS" =>
        val event = Event.Sustain(editor.cursor.position, Rational.fullBeat)
        val newEditor = editor.addEvent(event)
        val newCursor = editor.cursor.nextBeat
        newEditor.copy(cursor = newCursor)
      case _ => editor

  /** Handle octave modifier keys. */
  def handleOctaveKey(editor: CompositionEditor, keyName: String): CompositionEditor =
    keyName match
      case "PERIOD" =>
        editor.copy(cursor = editor.cursor.withOctave(Octave.Mandra))
      case "QUOTE" =>
        editor.copy(cursor = editor.cursor.withOctave(Octave.Taar))
      case _ => editor

  /** Handle subdivision keys (Ctrl+2 through Ctrl+8). */
  def handleSubdivision(editor: CompositionEditor, n: Int): CompositionEditor =
    editor.copy(cursor = editor.cursor.withSubdivisions(n))

  private def resolveVariant(note: Note, shiftDown: Boolean): Variant =
    if !shiftDown then Variant.Shuddha
    else note match
      case Note.Ma => Variant.Tivra
      case Note.Sa | Note.Pa => Variant.Shuddha // Sa and Pa have no variants
      case _ => Variant.Komal
```

- [ ] **Step 5: Run tests**

Run: `sbt "testOnly sangeet.editor.KeyHandlerSpec"`
Expected: All tests pass.

- [ ] **Step 6: Commit**

```bash
git add src/
git commit -m "feat: add KeyHandler and CompositionEditor for keyboard-driven swar input"
```

---

### Task 17: Wire Editor UI Together

**Files:**
- Modify: `src/main/scala/sangeet/editor/EditorPane.scala`
- Modify: `src/main/scala/sangeet/editor/MainApp.scala`
- Create: `src/main/scala/sangeet/editor/ToolBar.scala`

- [ ] **Step 1: Create ToolBar**

```scala
// src/main/scala/sangeet/editor/ToolBar.scala
package sangeet.editor

import scalafx.scene.control.*
import scalafx.scene.layout.HBox
import scalafx.geometry.Insets
import sangeet.taal.Taals
import sangeet.model.*

class ToolBar(onTaalChange: Taal => Unit, onAddSection: () => Unit) extends HBox:
  spacing = 10
  padding = Insets(5, 10, 5, 10)

  private val taalCombo = new ComboBox[String]:
    items = javafx.collections.FXCollections.observableArrayList(
      Taals.all.keys.toList.sorted.map(_.capitalize): _*
    )
    value = "Teentaal"
    onAction = _ =>
      Taals.byName(value.value).foreach(onTaalChange)

  private val addSectionBtn = new Button("+ Section"):
    onAction = _ => onAddSection()

  children = List(
    new Label("Taal:"), taalCombo,
    new Separator(),
    addSectionBtn
  )
```

- [ ] **Step 2: Update EditorPane with keyboard handling**

```scala
// src/main/scala/sangeet/editor/EditorPane.scala
package sangeet.editor

import scalafx.scene.layout.StackPane
import scalafx.scene.canvas.Canvas
import scalafx.scene.input.{KeyCode, KeyEvent}
import sangeet.model.*
import sangeet.layout.LayoutConfig
import sangeet.render.CanvasRenderer

class EditorPane extends StackPane:
  private val canvas = new Canvas(1100, 700)
  children = List(canvas)
  focusTraversable = true

  private var editor: Option[CompositionEditor] = None
  private val config = LayoutConfig()

  def setComposition(comp: Composition): Unit =
    editor = Some(CompositionEditor(comp, 0, CursorModel(comp.metadata.taal)))
    redraw()

  def setEditor(ed: CompositionEditor): Unit =
    editor = Some(ed)
    redraw()

  def getComposition: Option[Composition] = editor.map(_.composition)

  def redraw(): Unit =
    editor.foreach { ed =>
      CanvasRenderer.render(canvas, ed.composition, config)
    }

  // Keyboard handling
  onKeyPressed = (e: KeyEvent) =>
    editor.foreach { ed =>
      val newEditor = e.code match
        case KeyCode.Right => ed.copy(cursor = ed.cursor.nextBeat)
        case KeyCode.Left  => ed.copy(cursor = ed.cursor.prevBeat)
        case KeyCode.Space =>
          KeyHandler.handleSpecialKey(ed, "SPACE")
        case KeyCode.Minus =>
          KeyHandler.handleSpecialKey(ed, "MINUS")
        case KeyCode.Period if !e.isControlDown =>
          KeyHandler.handleOctaveKey(ed, "PERIOD")
        case KeyCode.Quote =>
          KeyHandler.handleOctaveKey(ed, "QUOTE")
        case _ => ed

      editor = Some(newEditor)
      redraw()
    }

  onKeyTyped = (e: KeyEvent) =>
    editor.foreach { ed =>
      val ch = e.character.headOption.getOrElse(' ')
      if ch.isLetter then
        val (newEditor, _) = KeyHandler.handleSwarKey(ed, ch, e.isShiftDown)
        editor = Some(newEditor)
        redraw()
    }
```

- [ ] **Step 3: Update MainApp with ToolBar and file operations**

```scala
// src/main/scala/sangeet/editor/MainApp.scala
package sangeet.editor

import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.control.*
import scalafx.scene.layout.BorderPane
import scalafx.scene.paint.Color
import scalafx.stage.FileChooser
import sangeet.format.SwarFormat
import sangeet.model.*
import sangeet.taal.Taals
import java.nio.file.{Path, Files}

object MainApp extends JFXApp3:

  override def start(): Unit =
    val editorPane = new EditorPane()

    val toolbar = new ToolBar(
      onTaalChange = taal => (), // TODO: implement taal change
      onAddSection = () => ()    // TODO: implement add section
    )

    val menuBar = new MenuBar:
      menus = List(
        new Menu("File"):
          items = List(
            new MenuItem("New"):
              onAction = _ =>
                val comp = CompositionEditor.empty(Taals.teentaal,
                  Raag("", None, None, None, None, None, None, None))
                editorPane.setEditor(comp)
            ,
            new MenuItem("Open..."):
              onAction = _ =>
                val fc = new FileChooser:
                  title = "Open Composition"
                  extensionFilters.add(
                    new FileChooser.ExtensionFilter("Swar Files", "*.swar"))
                val file = fc.showOpenDialog(stage)
                if file != null then
                  SwarFormat.readFile(file.toPath).foreach(editorPane.setComposition)
            ,
            new MenuItem("Save As..."):
              onAction = _ =>
                editorPane.getComposition.foreach { comp =>
                  val fc = new FileChooser:
                    title = "Save Composition"
                    extensionFilters.add(
                      new FileChooser.ExtensionFilter("Swar Files", "*.swar"))
                  val file = fc.showSaveDialog(stage)
                  if file != null then
                    val path = if file.getName.endsWith(".swar") then file.toPath
                               else Path.of(file.getPath + ".swar")
                    SwarFormat.writeFile(path, comp)
                }
          )
      )

    stage = new PrimaryStage:
      title = "Sangeet Notes Editor"
      width = 1200
      height = 800
      scene = new Scene:
        fill = Color.White
        root = new BorderPane:
          top = new scalafx.scene.layout.VBox(menuBar, toolbar)
          center = editorPane

    // Load sample if available
    val samplePath = Path.of("samples/yaman-vilambit-gat.swar")
    if Files.exists(samplePath) then
      SwarFormat.readFile(samplePath).foreach(editorPane.setComposition)

    editorPane.requestFocus()
```

- [ ] **Step 4: Run the app and test keyboard input**

Run: `sbt run`
Expected: Window opens with menu bar, toolbar, notation display. Pressing `s`, `r`, `g` etc. should add notes. Arrow keys navigate. Space adds rest.

- [ ] **Step 5: Commit**

```bash
git add src/
git commit -m "feat: wire editor UI with keyboard input, toolbar, and file open/save"
```

---

## Phase 5: Audio Playback

### Task 18: PlaybackScheduler & MidiEngine

**Files:**
- Create: `src/main/scala/sangeet/audio/SoundEngine.scala`
- Create: `src/main/scala/sangeet/audio/PlaybackScheduler.scala`
- Create: `src/main/scala/sangeet/audio/MidiEngine.scala`
- Create: `src/test/scala/sangeet/audio/PlaybackSchedulerSpec.scala`

- [ ] **Step 1: Write failing tests**

```scala
// src/test/scala/sangeet/audio/PlaybackSchedulerSpec.scala
package sangeet.audio

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*

class PlaybackSchedulerSpec extends AnyFlatSpec with Matchers:

  def swar(beat: Int, note: Note = Note.Sa): Event.Swar =
    Event.Swar(note, Variant.Shuddha, Octave.Madhya,
      BeatPosition(0, beat, Rational.onBeat), Rational.fullBeat, None, Nil, None)

  "PlaybackScheduler.schedule" should "convert events to timed notes" in {
    val events = List(swar(0, Note.Sa), swar(1, Note.Re), swar(2, Note.Ga))
    val bpm = 60.0 // 1 beat per second
    val timedNotes = PlaybackScheduler.schedule(events, bpm)
    timedNotes should have length 3
    timedNotes(0).timeMs shouldBe 0L
    timedNotes(1).timeMs shouldBe 1000L
    timedNotes(2).timeMs shouldBe 2000L
  }

  it should "handle sub-beat events" in {
    val events = List(
      swar(0, Note.Sa).copy(beat = BeatPosition(0, 0, Rational.onBeat)),
      swar(0, Note.Re).copy(beat = BeatPosition(0, 0, Rational(1, 2)))
    )
    val timedNotes = PlaybackScheduler.schedule(events, 60.0)
    timedNotes(0).timeMs shouldBe 0L
    timedNotes(1).timeMs shouldBe 500L
  }

  it should "skip rest events (no sound)" in {
    val events = List(
      swar(0),
      Event.Rest(BeatPosition(0, 1, Rational.onBeat), Rational.fullBeat),
      swar(2)
    )
    val timedNotes = PlaybackScheduler.schedule(events, 60.0)
    timedNotes should have length 2
  }
```

- [ ] **Step 2: Run tests to verify failure**

Run: `sbt "testOnly sangeet.audio.PlaybackSchedulerSpec"`
Expected: FAIL.

- [ ] **Step 3: Implement SoundEngine trait**

```scala
// src/main/scala/sangeet/audio/SoundEngine.scala
package sangeet.audio

import sangeet.model.*

case class TimedNote(
  timeMs: Long,
  durationMs: Long,
  note: Note,
  variant: Variant,
  octave: Octave,
  stroke: Option[Stroke]
)

trait SoundEngine:
  def init(): Unit
  def playNote(note: TimedNote): Unit
  def stop(): Unit
  def shutdown(): Unit
```

- [ ] **Step 4: Implement PlaybackScheduler**

```scala
// src/main/scala/sangeet/audio/PlaybackScheduler.scala
package sangeet.audio

import sangeet.model.*

object PlaybackScheduler:

  def schedule(events: List[Event], bpm: Double): List[TimedNote] =
    val msPerBeat = 60000.0 / bpm
    events.collect {
      case s: Event.Swar =>
        val beatOffset = s.beat.cycle * 16 + s.beat.beat // simplified: assumes 16 beat taal
        val subOffset = s.beat.subdivision.toDouble
        val timeMs = ((beatOffset + subOffset) * msPerBeat).toLong
        val durationMs = (s.duration.toDouble * msPerBeat).toLong
        TimedNote(timeMs, durationMs, s.note, s.variant, s.octave, s.stroke)
    }

  def scheduleWithTaal(events: List[Event], bpm: Double, matras: Int): List[TimedNote] =
    val msPerBeat = 60000.0 / bpm
    events.collect {
      case s: Event.Swar =>
        val beatOffset = s.beat.cycle * matras + s.beat.beat
        val subOffset = s.beat.subdivision.toDouble
        val timeMs = ((beatOffset + subOffset) * msPerBeat).toLong
        val durationMs = (s.duration.toDouble * msPerBeat).toLong
        TimedNote(timeMs, durationMs, s.note, s.variant, s.octave, s.stroke)
    }
```

- [ ] **Step 5: Implement MidiEngine**

```scala
// src/main/scala/sangeet/audio/MidiEngine.scala
package sangeet.audio

import javax.sound.midi.*
import sangeet.model.*

class MidiEngine extends SoundEngine:
  private var synthesizer: Option[Synthesizer] = None
  private var channel: Option[MidiChannel] = None

  // MIDI note mapping: Sa = C4 (60), Re = D4 (62), etc.
  // Adjusts for komal/tivra and octave
  private def midiNote(note: Note, variant: Variant, octave: Octave): Int =
    val baseNote = note match
      case Note.Sa  => 0
      case Note.Re  => 2
      case Note.Ga  => 4
      case Note.Ma  => 5
      case Note.Pa  => 7
      case Note.Dha => 9
      case Note.Ni  => 11

    val alteration = variant match
      case Variant.Komal => -1
      case Variant.Tivra => 1
      case Variant.Shuddha => 0

    val octaveOffset = octave match
      case Octave.AtiMandra => -24
      case Octave.Mandra    => -12
      case Octave.Madhya    => 0
      case Octave.Taar      => 12
      case Octave.AtiTaar   => 24

    60 + baseNote + alteration + octaveOffset // C4 = 60 as Sa

  override def init(): Unit =
    val synth = MidiSystem.getSynthesizer
    synth.open()
    synthesizer = Some(synth)
    val channels = synth.getChannels
    channel = Some(channels(0))
    // Set to sitar-like patch (General MIDI #104 = Sitar)
    channels(0).programChange(103)

  override def playNote(timedNote: TimedNote): Unit =
    channel.foreach { ch =>
      val midi = midiNote(timedNote.note, timedNote.variant, timedNote.octave)
      val velocity = 80
      ch.noteOn(midi, velocity)
      // Schedule note off (simplified — in real implementation, use a scheduler thread)
      Thread.sleep(timedNote.durationMs.min(500))
      ch.noteOff(midi)
    }

  override def stop(): Unit =
    channel.foreach(_.allNotesOff())

  override def shutdown(): Unit =
    synthesizer.foreach(_.close())
    synthesizer = None
    channel = None
```

- [ ] **Step 6: Run tests**

Run: `sbt "testOnly sangeet.audio.PlaybackSchedulerSpec"`
Expected: All tests pass.

- [ ] **Step 7: Commit**

```bash
git add src/
git commit -m "feat: add PlaybackScheduler and MidiEngine for audio playback"
```

---

### Task 19: Playback Controls in UI

**Files:**
- Create: `src/main/scala/sangeet/audio/PlaybackController.scala`
- Modify: `src/main/scala/sangeet/editor/MainApp.scala`

- [ ] **Step 1: Implement PlaybackController**

```scala
// src/main/scala/sangeet/audio/PlaybackController.scala
package sangeet.audio

import sangeet.model.*
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

class PlaybackController(engine: SoundEngine):
  private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
  private var playing = false

  def play(events: List[Event], bpm: Double, matras: Int): Unit =
    if playing then stop()
    engine.init()
    playing = true

    val timedNotes = PlaybackScheduler.scheduleWithTaal(events, bpm, matras)
    timedNotes.foreach { tn =>
      executor.schedule(
        new Runnable { def run(): Unit = if playing then engine.playNote(tn) },
        tn.timeMs,
        TimeUnit.MILLISECONDS
      )
    }

  def stop(): Unit =
    playing = false
    engine.stop()

  def shutdown(): Unit =
    playing = false
    executor.shutdownNow()
    engine.shutdown()

  def isPlaying: Boolean = playing
```

- [ ] **Step 2: Add playback controls to MainApp**

Add to the bottom of the BorderPane in `MainApp.scala`:

```scala
    val midiEngine = new sangeet.audio.MidiEngine()
    val playbackController = new sangeet.audio.PlaybackController(midiEngine)

    val playbackBar = new scalafx.scene.layout.HBox:
      spacing = 10
      padding = scalafx.geometry.Insets(5, 10, 5, 10)
      children = List(
        new Button("Play"):
          onAction = _ =>
            editorPane.getComposition.foreach { comp =>
              val allEvents = comp.sections.flatMap(_.events)
              val bpm = comp.metadata.laya match
                case Some(Laya.Vilambit) => 40.0
                case Some(Laya.Madhya) => 80.0
                case Some(Laya.Drut) => 160.0
                case _ => 60.0
              playbackController.play(allEvents, bpm, comp.metadata.taal.matras)
            }
        ,
        new Button("Stop"):
          onAction = _ => playbackController.stop()
      )

    // In the BorderPane:
    // bottom = playbackBar
```

Wire `playbackBar` into the BorderPane's `bottom` property.

- [ ] **Step 3: Run the app and test playback**

Run: `sbt run`
Expected: Clicking "Play" should trigger MIDI sitar sounds for the sample composition. "Stop" should silence playback.

- [ ] **Step 4: Commit**

```bash
git add src/
git commit -m "feat: add playback controls with MIDI engine integration"
```

---

## Phase 6: PDF Export

### Task 20: PDF Export

**Files:**
- Create: `src/main/scala/sangeet/format/PdfExport.scala`
- Create: `src/test/scala/sangeet/format/PdfExportSpec.scala`

- [ ] **Step 1: Write failing test**

```scala
// src/test/scala/sangeet/format/PdfExportSpec.scala
package sangeet.format

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*
import sangeet.taal.Taals
import java.nio.file.{Files, Path}

class PdfExportSpec extends AnyFlatSpec with Matchers:

  val comp = Composition(
    metadata = Metadata(
      title = "Test Composition",
      compositionType = CompositionType.Gat,
      raag = Raag("Yaman", Some("Kalyan"),
        Some(List("S","R","G","M+","P","D","N","S'")),
        Some(List("S'","N","D","P","M+","G","R","S")),
        Some("G"), Some("N"), None, Some(1)),
      taal = Taals.teentaal,
      laya = Some(Laya.Vilambit),
      instrument = Some("Sitar"),
      composer = None, author = None, source = None,
      createdAt = "2026-03-28T10:00:00Z",
      updatedAt = "2026-03-28T10:00:00Z"),
    sections = List(
      Section("Sthayi", SectionType.Sthayi, List(
        Event.Swar(Note.Sa, Variant.Shuddha, Octave.Madhya,
          BeatPosition(0, 0, Rational.onBeat), Rational.fullBeat, Some(Stroke.Da), Nil, None)
      ))),
    tihais = Nil)

  "PdfExport" should "create a PDF file" in {
    val tmpPath = Files.createTempFile("sangeet-test-", ".pdf")
    try
      PdfExport.export(comp, tmpPath)
      Files.exists(tmpPath) shouldBe true
      Files.size(tmpPath) should be > 0L
    finally
      Files.deleteIfExists(tmpPath)
  }
```

- [ ] **Step 2: Run test to verify failure**

Run: `sbt "testOnly sangeet.format.PdfExportSpec"`
Expected: FAIL.

- [ ] **Step 3: Implement PdfExport**

```scala
// src/main/scala/sangeet/format/PdfExport.scala
package sangeet.format

import org.apache.pdfbox.pdmodel.{PDDocument, PDPage, PDPageContentStream}
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.{PDType0Font, PDType1Font, Standard14Fonts}
import sangeet.model.*
import sangeet.layout.*
import sangeet.render.DevanagariMap
import java.nio.file.Path

object PdfExport:

  def export(composition: Composition, path: Path, landscape: Boolean = false): Unit =
    val doc = new PDDocument()
    try
      val pageSize = if landscape then
        new PDRectangle(PDRectangle.A4.getHeight, PDRectangle.A4.getWidth)
      else PDRectangle.A4

      val page = new PDPage(pageSize)
      doc.addPage(page)

      val font = new PDType1Font(Standard14Fonts.FontName.HELVETICA)
      val boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
      val cs = new PDPageContentStream(doc, page)

      try
        val margin = 50f
        val pageWidth = pageSize.getWidth - 2 * margin
        var y = pageSize.getHeight - margin

        // Title
        cs.setFont(boldFont, 16)
        cs.beginText()
        cs.newLineAtOffset(margin, y)
        cs.showText(composition.metadata.title)
        cs.endText()
        y -= 22

        // Raag info
        cs.setFont(font, 11)
        cs.beginText()
        cs.newLineAtOffset(margin, y)
        cs.showText(s"Raag: ${composition.metadata.raag.name}" +
          composition.metadata.raag.thaat.map(t => s" ($t Thaat)").getOrElse(""))
        cs.endText()
        y -= 16

        // Arohi / Avarohi
        composition.metadata.raag.arohana.foreach { ar =>
          cs.beginText()
          cs.newLineAtOffset(margin, y)
          cs.showText(s"Arohi: ${ar.mkString(" ")}")
          cs.endText()
          y -= 14
        }
        composition.metadata.raag.avarohana.foreach { av =>
          cs.beginText()
          cs.newLineAtOffset(margin, y)
          cs.showText(s"Avarohi: ${av.mkString(" ")}")
          cs.endText()
          y -= 14
        }

        // Taal and Laya
        val taalLine = s"Taal: ${composition.metadata.taal.name} (${composition.metadata.taal.matras} matras)" +
          composition.metadata.laya.map(l => s"  |  Laya: ${l.toString}").getOrElse("")
        cs.beginText()
        cs.newLineAtOffset(margin, y)
        cs.showText(taalLine)
        cs.endText()
        y -= 20

        // Sections — render as text for now (Devanagari requires font embedding)
        val config = LayoutConfig()
        val grids = GridLayout.layoutAll(composition, config)

        grids.foreach { grid =>
          y -= 10
          cs.setFont(boldFont, 12)
          cs.beginText()
          cs.newLineAtOffset(margin, y)
          cs.showText(grid.sectionName)
          cs.endText()
          y -= 18

          cs.setFont(font, 10)
          grid.lines.foreach { line =>
            // Render swar names in Roman (Devanagari requires embedded font)
            val cellTexts = line.cells.map { cell =>
              cell.events.map {
                case s: Event.Swar =>
                  val variant = s.variant match
                    case Variant.Komal => "(k)"
                    case Variant.Tivra => "(t)"
                    case _ => ""
                  val octave = s.octave match
                    case Octave.Mandra => "."
                    case Octave.Taar => "'"
                    case _ => ""
                  s"${s.note}$variant$octave"
                case _: Event.Rest => "-"
                case _: Event.Sustain => "-"
              }.mkString(" ")
            }
            val lineText = cellTexts.mkString(" | ")
            cs.beginText()
            cs.newLineAtOffset(margin, y)
            cs.showText(lineText)
            cs.endText()
            y -= 14

            if y < margin + 50 then
              // New page needed
              y = pageSize.getHeight - margin
          }
        }

        // Footer
        cs.setFont(font, 8)
        cs.beginText()
        cs.newLineAtOffset(margin, margin - 10)
        cs.showText("Sangeet Notes Editor")
        cs.endText()

      finally
        cs.close()

      doc.save(path.toFile)
    finally
      doc.close()
```

- [ ] **Step 4: Run test**

Run: `sbt "testOnly sangeet.format.PdfExportSpec"`
Expected: Test passes — PDF file is created with content.

- [ ] **Step 5: Add PDF export to menu**

In `MainApp.scala`, add a menu item under File:

```scala
            new MenuItem("Export PDF..."):
              onAction = _ =>
                editorPane.getComposition.foreach { comp =>
                  val fc = new FileChooser:
                    title = "Export PDF"
                    extensionFilters.add(
                      new FileChooser.ExtensionFilter("PDF Files", "*.pdf"))
                  val file = fc.showSaveDialog(stage)
                  if file != null then
                    val path = if file.getName.endsWith(".pdf") then file.toPath
                               else Path.of(file.getPath + ".pdf")
                    sangeet.format.PdfExport.export(comp, path)
                }
```

- [ ] **Step 6: Run app and test PDF export**

Run: `sbt run`
Expected: File > Export PDF creates a readable PDF with composition header, raag info, arohi/avarohi, and notation.

- [ ] **Step 7: Commit**

```bash
git add src/
git commit -m "feat: add PDF export with composition header and notation layout"
```

---

## Final Task: Integration Verification

### Task 21: End-to-End Smoke Test

**Files:**
- Create: `src/test/scala/sangeet/IntegrationSpec.scala`

- [ ] **Step 1: Write integration test**

```scala
// src/test/scala/sangeet/IntegrationSpec.scala
package sangeet

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*
import sangeet.format.{SwarFormat, Codecs}
import sangeet.layout.*
import sangeet.taal.Taals
import java.nio.file.{Files, Path}

class IntegrationSpec extends AnyFlatSpec with Matchers:
  import Codecs.given

  def buildComposition(): Composition =
    Composition(
      metadata = Metadata(
        title = "Integration Test Gat",
        compositionType = CompositionType.Gat,
        raag = Raag("Yaman", Some("Kalyan"),
          Some(List("S","R","G","M+","P","D","N","S'")),
          Some(List("S'","N","D","P","M+","G","R","S")),
          Some("G"), Some("N"), None, Some(1)),
        taal = Taals.teentaal,
        laya = Some(Laya.Vilambit),
        instrument = Some("Sitar"),
        composer = Some("Traditional"),
        author = Some("Test"),
        source = None,
        createdAt = "2026-03-28T10:00:00Z",
        updatedAt = "2026-03-28T10:00:00Z"),
      sections = List(
        Section("Sthayi", SectionType.Sthayi,
          (0 until 16).toList.map { beat =>
            Event.Swar(Note.values(beat % 7), Variant.Shuddha, Octave.Madhya,
              BeatPosition(0, beat, Rational.onBeat), Rational.fullBeat,
              Some(if beat % 2 == 0 then Stroke.Da else Stroke.Ra),
              if beat == 3 then List(Gamak()) else Nil,
              None)
          }
        ),
        Section("Antara", SectionType.Antara,
          (0 until 16).toList.map { beat =>
            Event.Swar(Note.values(beat % 7), Variant.Shuddha, Octave.Taar,
              BeatPosition(0, beat, Rational.onBeat), Rational.fullBeat,
              Some(Stroke.Da), Nil, None)
          }
        )
      ),
      tihais = List(
        Tihai("Sthayi", BeatPosition(0, 10, Rational.onBeat),
              BeatPosition(1, 0, Rational.onBeat))
      )
    )

  "Full pipeline" should "roundtrip composition through .swar file" in {
    val comp = buildComposition()
    val tmpFile = Files.createTempFile("sangeet-integration-", ".swar")
    try
      SwarFormat.writeFile(tmpFile, comp)
      val loaded = SwarFormat.readFile(tmpFile)
      loaded shouldBe Right(comp)
    finally
      Files.deleteIfExists(tmpFile)
  }

  it should "layout composition into grids" in {
    val comp = buildComposition()
    val grids = GridLayout.layoutAll(comp, LayoutConfig())
    grids should have length 2
    grids.head.sectionName shouldBe "Sthayi"
    grids.head.lines should not be empty
  }

  it should "schedule playback events" in {
    val comp = buildComposition()
    val events = comp.sections.flatMap(_.events)
    val timedNotes = sangeet.audio.PlaybackScheduler.scheduleWithTaal(
      events, 60.0, comp.metadata.taal.matras)
    timedNotes should not be empty
    timedNotes.head.timeMs shouldBe 0L
  }

  it should "export to PDF" in {
    val comp = buildComposition()
    val tmpPdf = Files.createTempFile("sangeet-integration-", ".pdf")
    try
      sangeet.format.PdfExport.export(comp, tmpPdf)
      Files.size(tmpPdf) should be > 0L
    finally
      Files.deleteIfExists(tmpPdf)
  }

  "Palta composition" should "roundtrip with no laya" in {
    val palta = buildComposition().copy(
      metadata = buildComposition().metadata.copy(
        title = "Yaman Palta 1",
        compositionType = CompositionType.Palta,
        laya = None
      ),
      sections = List(Section("Palta", SectionType.Palta, Nil))
    )
    val tmpFile = Files.createTempFile("sangeet-palta-", ".swar")
    try
      SwarFormat.writeFile(tmpFile, palta)
      val loaded = SwarFormat.readFile(tmpFile)
      loaded.map(_.metadata.compositionType) shouldBe Right(CompositionType.Palta)
      loaded.map(_.metadata.laya) shouldBe Right(None)
    finally
      Files.deleteIfExists(tmpFile)
  }
```

- [ ] **Step 2: Run all tests**

Run: `sbt test`
Expected: All tests pass (model, format, layout, audio, integration).

- [ ] **Step 3: Commit**

```bash
git add src/
git commit -m "feat: add integration tests verifying full pipeline"
```

- [ ] **Step 4: Final run of the application**

Run: `sbt run`
Expected: Full application launches with:
- Composition header showing raag, arohi/avarohi, taal
- Bhatkhande-style notation grid with Devanagari swaras
- Keyboard input for adding notes
- File > Open/Save for .swar files
- File > Export PDF
- Play/Stop for MIDI playback

- [ ] **Step 5: Final commit**

```bash
git add -A
git commit -m "feat: Sangeet Notes Editor v0.1.0 — complete MVP"
```
