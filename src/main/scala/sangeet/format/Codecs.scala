package sangeet.format

import io.circe.*
import sangeet.model.*

/** Facade re-exporting all codecs for backward compatibility.
  * Use `import Codecs.given` to get all encoder/decoder instances.
  * Implementation is split into ModelCodecs, OrnamentCodecs, and CompositionCodecs. */
object Codecs:

  // Re-export all model codecs
  export ModelCodecs.given

  // Re-export ornament codecs
  export OrnamentCodecs.given

  // Re-export composition codecs
  export CompositionCodecs.given
