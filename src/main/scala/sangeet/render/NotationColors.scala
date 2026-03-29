package sangeet.render

/** Shared color palette for notation rows across canvas, PDF, and HTML renderers.
  * All colors are chosen to be clearly visible on white background. */
object NotationColors:
  // Taal markers (Sam X, Khali 0, Taali 2/3)
  val taalMarker    = "#B71C1C"  // dark red
  val taalMarkerSam = "#D32F2F"  // brighter red for Sam (X)

  // Swar (note glyphs)
  val swar          = "#1A237E"  // dark indigo

  // Octave dots (taar/mandra saptak indicators)
  val octaveDot     = "#E65100"  // deep orange

  // Ornaments (meend, gamak, kan swar, etc.)
  val ornament      = "#4A148C"  // deep purple

  // Da/Ra stroke indicators
  val stroke        = "#00695C"  // teal

  // Sahitya (lyrics)
  val sahitya       = "#2E7D32"  // dark green

  // Rest and sustain
  val rest          = "#616161"  // grey
  val sustain       = "#9E9E9E"  // lighter grey

  // Komal/Tivra variant marks
  val komalMark     = "#1A237E"  // same as swar
  val tivraMark     = "#1A237E"  // same as swar
