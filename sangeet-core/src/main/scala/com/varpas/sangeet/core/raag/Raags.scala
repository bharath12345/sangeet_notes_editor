package com.varpas.sangeet.core.raag

import com.varpas.sangeet.core.model.Raag

object Raags:

  val yaman = Raag(
    "Yaman",
    Some("Kalyan"),
    Some(List("Sa", "Re", "Ga", "Ma♯", "Pa", "Dha", "Ni", "Sa'")),
    Some(List("Sa'", "Ni", "Dha", "Pa", "Ma♯", "Ga", "Re", "Sa")),
    Some("Ga"),
    Some("Ni"),
    Some("Ni Re Ga, Re Sa"),
    Some(1)
  )

  val bhairav = Raag(
    "Bhairav",
    Some("Bhairav"),
    Some(List("Sa", "Re♭", "Ga", "Ma", "Pa", "Dha♭", "Ni", "Sa'")),
    Some(List("Sa'", "Ni", "Dha♭", "Pa", "Ma", "Ga", "Re♭", "Sa")),
    Some("Dha"),
    Some("Re"),
    Some("Ga Ma Dha♭ Pa, Ma Ga Re♭ Sa"),
    Some(1)
  )

  val durga = Raag(
    "Durga",
    Some("Bilawal"),
    Some(List("Sa", "Re", "Ma", "Pa", "Dha", "Sa'")),
    Some(List("Sa'", "Dha", "Pa", "Ma", "Re", "Sa")),
    Some("Ma"),
    Some("Sa"),
    Some("Ma Pa Dha, Ma Re Sa"),
    None
  )

  val bhupali = Raag(
    "Bhupali",
    Some("Kalyan"),
    Some(List("Sa", "Re", "Ga", "Pa", "Dha", "Sa'")),
    Some(List("Sa'", "Dha", "Pa", "Ga", "Re", "Sa")),
    Some("Ga"),
    Some("Dha"),
    Some("Ga Re Pa Ga, Dha Pa Ga Re Sa"),
    Some(1)
  )

  val malkauns = Raag(
    "Malkauns",
    Some("Bhairavi"),
    Some(List("Sa", "Ga♭", "Ma", "Dha♭", "Ni♭", "Sa'")),
    Some(List("Sa'", "Ni♭", "Dha♭", "Ma", "Ga♭", "Sa")),
    Some("Ma"),
    Some("Sa"),
    Some("Ma Dha♭ Ni♭ Dha♭ Ma, Ga♭ Ma Ga♭ Sa"),
    Some(3)
  )

  val bageshree = Raag(
    "Bageshree",
    Some("Kafi"),
    Some(List("Sa", "Ga♭", "Ma", "Dha", "Ni♭", "Sa'")),
    Some(List("Sa'", "Ni♭", "Dha", "Ma", "Ga♭", "Re", "Sa")),
    Some("Ma"),
    Some("Sa"),
    Some("Dha Ni♭ Dha Ma, Ga♭ Ma Ga♭ Re Sa"),
    Some(2)
  )

  val desh = Raag(
    "Desh",
    Some("Khamaj"),
    Some(List("Sa", "Re", "Ma", "Pa", "Ni♭", "Sa'")),
    Some(List("Sa'", "Ni♭", "Dha", "Pa", "Ma", "Ga", "Re", "Sa")),
    Some("Re"),
    Some("Pa"),
    Some("Re Ma Pa, Ni♭ Dha Pa"),
    Some(2)
  )

  val kafi = Raag(
    "Kafi",
    Some("Kafi"),
    Some(List("Sa", "Re", "Ga♭", "Ma", "Pa", "Dha", "Ni♭", "Sa'")),
    Some(List("Sa'", "Ni♭", "Dha", "Pa", "Ma", "Ga♭", "Re", "Sa")),
    Some("Pa"),
    Some("Sa"),
    Some("Sa Re Ga♭ Ma Pa, Dha Ni♭ Dha Pa"),
    Some(3)
  )

  val bihag = Raag(
    "Bihag",
    Some("Bilawal"),
    Some(List("Sa", "Ga", "Ma", "Pa", "Ni", "Sa'")),
    Some(List("Sa'", "Ni", "Dha", "Pa", "Ma", "Ga", "Re", "Sa")),
    Some("Ga"),
    Some("Ni"),
    Some("Ga Ma Pa, Ni Dha Pa Ma Ga Re Sa"),
    Some(3)
  )

  val kedar = Raag(
    "Kedar",
    Some("Kalyan"),
    Some(List("Sa", "Ma", "Ma♯", "Pa", "Dha", "Ni", "Sa'")),
    Some(List("Sa'", "Ni", "Dha", "Pa", "Ma♯", "Ma", "Ga", "Re", "Sa")),
    Some("Ma"),
    Some("Sa"),
    Some("Sa Ma Ma♯ Pa, Dha Ma Ga Re Sa"),
    Some(2)
  )

  val hansadhwani = Raag(
    "Hansadhwani",
    Some("Bilawal"),
    Some(List("Sa", "Re", "Ga", "Pa", "Ni", "Sa'")),
    Some(List("Sa'", "Ni", "Pa", "Ga", "Re", "Sa")),
    Some("Ga"),
    Some("Ni"),
    Some("Ga Re Pa Ga, Ni Pa Ga Re Sa"),
    None
  )

  val jaunpuri = Raag(
    "Jaunpuri",
    Some("Asavari"),
    Some(List("Sa", "Re", "Ga♭", "Ma", "Pa", "Dha♭", "Ni♭", "Sa'")),
    Some(List("Sa'", "Ni♭", "Dha♭", "Pa", "Ma", "Ga♭", "Re", "Sa")),
    Some("Dha"),
    Some("Ga"),
    Some("Re Ma Pa, Dha♭ Ma Pa Ga♭ Ma Re Sa"),
    Some(2)
  )

  val todi = Raag(
    "Todi",
    Some("Todi"),
    Some(List("Sa", "Re♭", "Ga♭", "Ma♯", "Pa", "Dha♭", "Ni", "Sa'")),
    Some(List("Sa'", "Ni", "Dha♭", "Pa", "Ma♯", "Ga♭", "Re♭", "Sa")),
    Some("Dha"),
    Some("Ga"),
    Some("Dha♭ Ni Sa' Re♭' Sa', Ni Dha♭ Pa Ma♯ Ga♭ Re♭ Sa"),
    Some(1)
  )

  val marwa = Raag(
    "Marwa",
    Some("Marwa"),
    Some(List("Sa", "Re♭", "Ga", "Ma♯", "Dha", "Ni", "Sa'")),
    Some(List("Sa'", "Ni", "Dha", "Ma♯", "Ga", "Re♭", "Sa")),
    Some("Dha"),
    Some("Re"),
    Some("Re♭ Ga, Dha Ni Dha Ma♯ Ga Re♭ Sa"),
    Some(1)
  )

  val puriya = Raag(
    "Puriya",
    Some("Marwa"),
    Some(List("Sa", "Re♭", "Ga", "Ma♯", "Dha", "Ni", "Sa'")),
    Some(List("Sa'", "Ni", "Dha", "Ma♯", "Ga", "Re♭", "Sa")),
    Some("Ga"),
    Some("Ni"),
    Some("Ni Re♭ Ga, Ma♯ Ga Re♭ Sa, Ni Dha Ni Sa"),
    Some(1)
  )

  val shree = Raag(
    "Shree",
    Some("Purvi"),
    Some(List("Sa", "Re", "Ga", "Ma♯", "Pa", "Dha♭", "Ni", "Sa'")),
    Some(List("Sa'", "Ni", "Dha♭", "Pa", "Ma♯", "Ga", "Re", "Sa")),
    Some("Pa"),
    Some("Re"),
    Some("Pa Dha♭ Ni Sa' Re' Sa' Ni Dha♭ Pa"),
    Some(1)
  )

  val miyanKiMalhar = Raag(
    "Miyan ki Malhar",
    Some("Kafi"),
    Some(List("Sa", "Re", "Ma", "Pa", "Ni♭", "Sa'")),
    Some(List("Sa'", "Ni♭", "Dha", "Pa", "Ma", "Re", "Sa")),
    Some("Ma"),
    Some("Sa"),
    Some("Sa Re Ma Pa, Ni♭ Dha Pa Ma Re Sa"),
    Some(3)
  )

  val megh = Raag(
    "Megh",
    Some("Kafi"),
    Some(List("Sa", "Re", "Ma", "Pa", "Ni♭", "Sa'")),
    Some(List("Sa'", "Ni♭", "Pa", "Ma", "Re", "Sa")),
    Some("Ma"),
    Some("Sa"),
    Some("Re Ma Pa, Ni♭ Pa Ma Re Sa"),
    Some(3)
  )

  val pilu = Raag(
    "Pilu",
    Some("Kafi"),
    Some(List("Sa", "Re", "Ga", "Ma", "Pa", "Dha", "Ni", "Sa'")),
    Some(List("Sa'", "Ni", "Dha", "Pa", "Ma", "Ga", "Re", "Sa")),
    Some("Ga"),
    Some("Ni"),
    None,
    None
  )

  val khamaj = Raag(
    "Khamaj",
    Some("Khamaj"),
    Some(List("Sa", "Ga", "Ma", "Pa", "Dha", "Ni♭", "Sa'")),
    Some(List("Sa'", "Ni", "Dha", "Pa", "Ma", "Ga", "Re", "Sa")),
    Some("Ga"),
    Some("Ni"),
    Some("Ga Ma Pa Dha Ni♭, Dha Pa Ma Ga Re Sa"),
    Some(2)
  )

  val bilawal = Raag(
    "Bilawal",
    Some("Bilawal"),
    Some(List("Sa", "Re", "Ga", "Ma", "Pa", "Dha", "Ni", "Sa'")),
    Some(List("Sa'", "Ni", "Dha", "Pa", "Ma", "Ga", "Re", "Sa")),
    Some("Dha"),
    Some("Ga"),
    Some("Ga Re Ga Pa, Dha Pa Ga Ma Re Sa"),
    Some(1)
  )

  val bhairavi = Raag(
    "Bhairavi",
    Some("Bhairavi"),
    Some(List("Sa", "Re♭", "Ga♭", "Ma", "Pa", "Dha♭", "Ni♭", "Sa'")),
    Some(List("Sa'", "Ni♭", "Dha♭", "Pa", "Ma", "Ga♭", "Re♭", "Sa")),
    Some("Ma"),
    Some("Sa"),
    Some("Sa Re♭ Ga♭ Ma Pa, Dha♭ Ma Pa Ga♭ Re♭ Sa"),
    None
  )

  val asavari = Raag(
    "Asavari",
    Some("Asavari"),
    Some(List("Sa", "Re", "Ma", "Pa", "Dha♭", "Sa'")),
    Some(List("Sa'", "Ni♭", "Dha♭", "Pa", "Ma", "Ga♭", "Re", "Sa")),
    Some("Dha"),
    Some("Ga"),
    Some("Ma Pa Dha♭ Sa' Ni♭ Dha♭ Pa, Ma Pa Ga♭ Re Sa"),
    Some(2)
  )

  val ahirBhairav = Raag(
    "Ahir Bhairav",
    Some("Bhairav"),
    Some(List("Sa", "Re♭", "Ga", "Ma", "Pa", "Dha", "Ni♭", "Sa'")),
    Some(List("Sa'", "Ni♭", "Dha", "Pa", "Ma", "Ga", "Re♭", "Sa")),
    Some("Ma"),
    Some("Sa"),
    Some("Ga Ma Dha, Ni♭ Dha Pa Ma Ga Re♭ Sa"),
    Some(1)
  )

  val hindol = Raag(
    "Hindol",
    Some("Kalyan"),
    Some(List("Sa", "Ga", "Ma♯", "Dha", "Ni", "Sa'")),
    Some(List("Sa'", "Ni", "Dha", "Ma♯", "Ga", "Sa")),
    Some("Dha"),
    Some("Ga"),
    Some("Sa Ga Ma♯ Dha, Ni Dha Ma♯ Ga Sa"),
    Some(1)
  )

  val madmadSarang = Raag(
    "Madmad Sarang",
    Some("Kafi"),
    Some(List("Sa", "Re", "Ma", "Pa", "Ni♭", "Sa'")),
    Some(List("Sa'", "Ni♭", "Dha", "Pa", "Ma", "Re", "Sa")),
    Some("Pa"),
    Some("Re"),
    Some("Ma Re Pa, Ni♭ Dha Pa Ma Re Sa"),
    Some(2)
  )

  val all: Map[String, Raag] = List(
    yaman,
    bhairav,
    durga,
    bhupali,
    malkauns,
    bageshree,
    desh,
    kafi,
    bihag,
    kedar,
    hansadhwani,
    jaunpuri,
    todi,
    marwa,
    puriya,
    shree,
    miyanKiMalhar,
    megh,
    pilu,
    khamaj,
    bilawal,
    bhairavi,
    asavari,
    ahirBhairav,
    hindol,
    madmadSarang
  ).map(r => r.name.toLowerCase -> r).toMap

  def byName(name: String): Option[Raag] = all.get(name.trim.toLowerCase)
