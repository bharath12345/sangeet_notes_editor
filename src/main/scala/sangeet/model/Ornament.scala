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
