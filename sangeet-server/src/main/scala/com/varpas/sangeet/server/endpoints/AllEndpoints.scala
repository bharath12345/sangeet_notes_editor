package com.varpas.sangeet.server.endpoints

import sttp.tapir.*

object AllEndpoints:

  val all: List[AnyEndpoint] =
    ReferenceEndpoints.all ++
    CompositionEndpoints.all ++
    EditorEndpoints.all ++
    CursorEndpoints.all ++
    SectionEndpoints.all ++
    OrnamentEndpoints.all ++
    StrokeEndpoints.all ++
    LayoutEndpoints.all ++
    ExportEndpoints.all ++
    GlyphEndpoints.all
