package com.varpas.sangeet.server.routes

import cats.effect.IO
import io.circe.Json
import io.circe.syntax._
import sttp.tapir.server.ServerEndpoint

import com.varpas.sangeet.core.api.LayoutApi
import com.varpas.sangeet.core.format.Codecs.given
import com.varpas.sangeet.core.layout.{BeatCell, GridLine, LayoutConfig, SectionGrid}
import com.varpas.sangeet.core.model._
import com.varpas.sangeet.server.endpoints.LayoutEndpoints
import com.varpas.sangeet.server.routes.JsonParsing._
import com.varpas.sangeet.server.routes.RouteHelper._

object LayoutRoutes:

  private def encodeBeatCell(cell: BeatCell): Json =
    Json.obj(
      "cycle"  -> Json.fromInt(cell.position.cycle),
      "beat"   -> Json.fromInt(cell.position.beat),
      "events" -> cell.events.asJson
    )

  private def encodeGridLine(line: GridLine): Json =
    Json.obj(
      "cells"        -> Json.arr(line.cells.map(encodeBeatCell)*),
      "vibhagBreaks" -> Json.arr(line.vibhagBreaks.map(Json.fromInt)*),
      "markers" -> Json.arr(line.markers.map { case (idx, marker) =>
        Json.obj(
          "cellIndex" -> Json.fromInt(idx),
          "marker"    -> marker.asJson
        )
      }*)
    )

  private def encodeSectionGrid(grid: SectionGrid): Json =
    Json.obj(
      "sectionName" -> Json.fromString(grid.sectionName),
      "sectionType" -> grid.sectionType.asJson,
      "lines"       -> Json.arr(grid.lines.map(encodeGridLine)*)
    )

  val compute: ServerEndpoint[Any, IO] =
    LayoutEndpoints.compute.serverLogic { body =>
      val c = body.hcursor
      handleResult(for
        composition          <- parseField[Composition](c, "composition")
        highDensityThreshold <- parseFieldOr(c, "highDensityThreshold", 5)
        cellWidthBase        <- parseFieldOr(c, "cellWidthBase", 60.0)
        cellOverflowExpand   <- parseFieldOr(c, "cellOverflowExpand", 15.0)
        lineSpacing          <- parseFieldOr(c, "lineSpacing", 40.0)
        headerHeight         <- parseFieldOr(c, "headerHeight", 120.0)
      yield
        val config = LayoutConfig(highDensityThreshold, cellWidthBase, cellOverflowExpand, lineSpacing, headerHeight)
        val grids  = LayoutApi.computeLayout(composition, config)
        Json.arr(grids.map(encodeSectionGrid)*)
      )(identity)
    }

  val all: List[ServerEndpoint[Any, IO]] = List(compute)
