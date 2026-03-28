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
