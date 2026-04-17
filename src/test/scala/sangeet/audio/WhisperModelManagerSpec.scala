package sangeet.audio

import org.scalatest.funsuite.AnyFunSuite

class WhisperModelManagerSpec extends AnyFunSuite:

  test("modelDir returns a path under user home"):
    val dir = WhisperModelManager.modelDir
    assert(dir.toString.contains("sangeet") || dir.toString.contains("Sangeet"))

  test("modelPath points to ggml-tiny.bin inside modelDir"):
    val path = WhisperModelManager.modelPath
    assert(path.getFileName.toString == "ggml-tiny.bin")
    assert(path.getParent == WhisperModelManager.modelDir)

  test("downloadUrl points to HuggingFace ggml-tiny.bin"):
    val url = WhisperModelManager.downloadUrl
    assert(url.contains("ggml-tiny.bin"))
    assert(url.contains("huggingface"))
