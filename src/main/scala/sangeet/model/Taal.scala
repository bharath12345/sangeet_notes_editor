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
