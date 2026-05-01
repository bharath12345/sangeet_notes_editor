package com.varpas.sangeet.core.api

import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.format.{PdfExport, HtmlExport}
import java.nio.file.Path
import scala.util.{Try, Success, Failure}

object ExportApi:

  /** Export composition to PDF file. */
  def exportPdf(
    composition: Composition,
    outputPath: Path,
    script: SwarScript,
    landscape: Boolean = false
  ): Either[ApiError, Unit] =
    Try(PdfExport.exportPdf(composition, outputPath, script, landscape)) match
      case Success(_) => Right(())
      case Failure(e) => Left(ApiError.ExportError(s"PDF export failed: ${e.getMessage}"))

  /** Export composition to HTML file. */
  def exportHtml(
    composition: Composition,
    outputPath: Path,
    script: SwarScript
  ): Either[ApiError, Unit] =
    Try(HtmlExport.exportHtml(composition, outputPath, script)) match
      case Success(_) => Right(())
      case Failure(e) => Left(ApiError.ExportError(s"HTML export failed: ${e.getMessage}"))
