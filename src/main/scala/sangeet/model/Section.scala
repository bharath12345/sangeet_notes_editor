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
