package com.varpas.sangeet.core.model

case class Section(
    name: String,
    sectionType: SectionType,
    events: List[Event],
    tihai: Option[Tihai] = None,
    startingBeat: Int = 1
)

enum SectionType:
  case Sthayi, Antara, Sanchari, Abhog
  case Taan, Toda, Jhala
  case Palta, Arohi, Avarohi
  case Custom(name: String)
