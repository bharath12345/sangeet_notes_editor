package sangeet.render

import sangeet.model.*

/** Shared ornament label mappings used by HTML, PDF, and other exporters. */
object OrnamentLabels:

  /** Full descriptive labels (used by HTML export) */
  def full(o: Ornament): String = o match
    case _: Meend          => "meend"
    case _: KanSwar        => "kan"
    case _: Gamak          => "gamak"
    case _: Andolan        => "andolan"
    case _: Gitkari        => "gitkari"
    case _: Murki          => "murki"
    case _: Krintan        => "krintan"
    case _: Ghaseet        => "ghaseet"
    case _: Sparsh         => "sparsh"
    case _: Zamzama        => "zamzama"
    case c: CustomOrnament => c.name

  /** Abbreviated labels (used by PDF export where space is tight) */
  def abbreviated(o: Ornament): String = o match
    case _: Meend          => "~"
    case _: KanSwar        => "k"
    case _: Gamak          => "G"
    case _: Andolan        => "A"
    case _: Gitkari        => "tr"
    case _: Murki          => "m"
    case _: Krintan        => "kr"
    case _: Ghaseet        => "gh"
    case _: Sparsh         => "sp"
    case _: Zamzama        => "zz"
    case c: CustomOrnament => c.name
