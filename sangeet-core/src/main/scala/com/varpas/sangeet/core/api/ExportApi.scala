package com.varpas.sangeet.core.api

import java.nio.file.Path

import scala.util.{Failure, Success, Try}

import com.varpas.sangeet.core.format.HtmlExport
import com.varpas.sangeet.core.model._

object ExportApi:

  /** Export composition to HTML file. */
  def exportHtml(
      composition: Composition,
      outputPath: Path,
      script: SwarScript
  ): Either[ApiError, Unit] =
    Try(HtmlExport.exportHtml(composition, outputPath, script)) match
      case Success(_) => Right(())
      case Failure(e) => Left(ApiError.ExportError(s"HTML export failed: ${e.getMessage}"))
