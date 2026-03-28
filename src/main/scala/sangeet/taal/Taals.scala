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
