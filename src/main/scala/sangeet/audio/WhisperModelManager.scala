package sangeet.audio

import java.nio.file.{Path, Files}
import java.net.URI

/** Manages the Whisper model file on disk.
  * Stores model in a platform-specific app data directory.
  * Downloads from HuggingFace on first use. */
object WhisperModelManager:

  val ModelFileName = "ggml-tiny.bin"

  val downloadUrl: String =
    "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin"

  def modelDir: Path =
    val os = System.getProperty("os.name", "").toLowerCase
    val home = Path.of(System.getProperty("user.home"))
    if os.contains("mac") then
      home.resolve("Library").resolve("Application Support").resolve("SangeetNotesEditor").resolve("models")
    else if os.contains("win") then
      val appData = System.getenv("APPDATA")
      if appData != null then Path.of(appData).resolve("SangeetNotesEditor").resolve("models")
      else home.resolve(".sangeet-notes-editor").resolve("models")
    else
      home.resolve(".local").resolve("share").resolve("sangeet-notes-editor").resolve("models")

  def modelPath: Path = modelDir.resolve(ModelFileName)

  def isModelAvailable: Boolean = Files.exists(modelPath)

  /** Download the model file. Calls onProgress(bytesDownloaded, totalBytes) during download.
    * totalBytes is -1 if Content-Length is unknown.
    * Returns true on success, false on failure. */
  def downloadModel(onProgress: (Long, Long) => Unit = (_, _) => ()): Boolean =
    try
      Files.createDirectories(modelDir)
      val url = URI.create(downloadUrl).toURL
      val connection = url.openConnection()
      val totalBytes = connection.getContentLengthLong
      val input = connection.getInputStream
      val tmpFile = modelDir.resolve(ModelFileName + ".tmp")
      val output = Files.newOutputStream(tmpFile)
      try
        val buf = new Array[Byte](65536)
        var downloaded = 0L
        var n = input.read(buf)
        while n != -1 do
          output.write(buf, 0, n)
          downloaded += n
          onProgress(downloaded, totalBytes)
          n = input.read(buf)
      finally
        output.close()
        input.close()
      Files.move(tmpFile, modelPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
      true
    catch
      case e: Exception =>
        e.printStackTrace()
        false
