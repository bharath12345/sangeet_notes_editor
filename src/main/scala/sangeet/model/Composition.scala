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
