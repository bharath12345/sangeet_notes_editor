package com.varpas.sangeet.desktop.diagnostics

import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO

import javafx.embed.swing.SwingFXUtils
import javafx.scene.{Scene => JFXScene}

/** Captures the current Scene as a PNG and returns it base64-encoded for inclusion in the bug-report JSON payload.
  *
  * Why base64 not multipart: the existing `/api/v1/bug-reports` endpoint accepts a single JSON body (Phase 5a chose
  * "any JSON" to keep the schema open). A few hundred KB of base64 inside the JSON is cheap to handle on both sides and
  * avoids a second endpoint or content-type-detection logic on the server.
  *
  * Must be called on the JavaFX Application Thread — `Scene.snapshot` requires it. Callers wrap with
  * `Platform.runLater` if invoked from a background thread.
  */
object ScreenshotCapture:

  /** Returns the PNG bytes as a base64 string, or `Left` with a short diagnostic if anything failed. Errors are
    * returned rather than thrown so the bug-report submit path can still proceed with a missing screenshot — the report
    * is more useful with no screenshot than not sent at all.
    */
  def capturePngBase64(scene: JFXScene): Either[String, String] =
    try
      val writableImage = scene.snapshot(null)
      val bufferedImage = SwingFXUtils.fromFXImage(writableImage, null)
      if bufferedImage == null then Left("snapshot returned null BufferedImage")
      else
        val baos = new ByteArrayOutputStream()
        val ok   = ImageIO.write(bufferedImage, "png", baos)
        if !ok then Left("ImageIO had no writer for png — JRE missing imageio-png?")
        else Right(Base64.getEncoder.encodeToString(baos.toByteArray))
    catch case t: Throwable => Left(s"${t.getClass.getSimpleName}: ${t.getMessage}")
