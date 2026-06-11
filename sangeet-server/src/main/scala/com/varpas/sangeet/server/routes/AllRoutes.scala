package com.varpas.sangeet.server.routes

import cats.effect.IO
import sttp.tapir.server.ServerEndpoint

object AllRoutes:

  val all: List[ServerEndpoint[Any, IO]] =
    ReferenceRoutes.all ++
      CompositionRoutes.all ++
      EditorRoutes.all ++
      CursorRoutes.all ++
      SectionRoutes.all ++
      OrnamentRoutes.all ++
      StrokeRoutes.all ++
      LayoutRoutes.all ++
      ExportRoutes.all ++
      RenderingRoutes.all ++
      BugReportRoutes.all
