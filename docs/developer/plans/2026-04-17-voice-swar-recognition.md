# Voice Swar Recognition Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add push-to-talk voice input — user holds a key, speaks a swar name (Sa/Re/Ga/Ma/Pa/Dha/Ni), releases, and the recognized note is inserted at the cursor.

**Architecture:** A `SwarRecognizer` wraps whisper-jni for on-device speech recognition with GBNF grammar constraining output to 7 swar names. `MicCapture` handles audio capture via `javax.sound.sampled`. `EditorPane` coordinates push-to-talk flow: key-press starts capture, key-release triggers inference, result maps to `Event.Swar` and inserts at cursor. The Whisper model (ggml-tiny.bin, ~77MB) ships as a resource or is downloaded on first run.

**Tech Stack:** whisper-jni 1.7.1, Whisper tiny multilingual model, javax.sound.sampled, Scala 3

---

### File Structure

```
src/main/scala/sangeet/audio/
  SwarRecognizer.scala     — Whisper model loading, inference, result parsing (NEW)
  MicCapture.scala         — Microphone audio capture, byte→float conversion (NEW)

src/main/resources/
  swar-grammar.gbnf        — GBNF grammar restricting output to 7 swar names (NEW)

src/test/scala/sangeet/audio/
  SwarRecognizerSpec.scala — Tests for text→Note mapping and result parsing (NEW)
  MicCaptureSpec.scala     — Tests for byte→float audio conversion (NEW)

Modified files:
  build.sbt                — Add whisper-jni dependency
  EditorPane.scala         — Push-to-talk key handling, visual indicator, voice mode toggle
  MainApp.scala            — Voice Input menu item to toggle, model path config
  KeyboardLegend.scala     — Document voice input shortcut
```

---

### Task 1: Add whisper-jni Dependency

**Files:**
- Modify: `build.sbt`

- [ ] **Step 1: Add whisper-jni to libraryDependencies**

In `build.sbt`, add to the `libraryDependencies` list after the `pdfbox` line:

```scala
      "io.github.givimad"   % "whisper-jni"  % "1.7.1",
```

- [ ] **Step 2: Verify dependency resolves**

Run: `sbt update`
Expected: Dependencies resolve successfully. whisper-jni and its transitive dependency (JNA) download.

- [ ] **Step 3: Verify compilation still works**

Run: `sbt compile`
Expected: Clean compilation with zero errors.

- [ ] **Step 4: Commit**

```bash
git add build.sbt
git commit -m "feat: add whisper-jni dependency for voice swar recognition"
```

---

### Task 2: Create GBNF Grammar Resource

**Files:**
- Create: `src/main/resources/swar-grammar.gbnf`

- [ ] **Step 1: Create the grammar file**

Create `src/main/resources/swar-grammar.gbnf`:

```gbnf
root ::= swar
swar ::= " sa" | " re" | " ga" | " ma" | " pa" | " dha" | " ni"
```

Note: Leading spaces are required — Whisper tokens typically include leading whitespace. The grammar forces the model to output exactly one of the 7 swar names.

- [ ] **Step 2: Verify resource is accessible from classpath**

Run: `sbt console` then:
```scala
val is = getClass.getResourceAsStream("/swar-grammar.gbnf")
println(is != null) // true
println(new String(is.readAllBytes()))
is.close()
```

Expected: Grammar content prints correctly.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/swar-grammar.gbnf
git commit -m "feat: add GBNF grammar for 7-swar vocabulary constraint"
```

---

### Task 3: MicCapture — Audio Capture Utility

**Files:**
- Create: `src/main/scala/sangeet/audio/MicCapture.scala`
- Create: `src/test/scala/sangeet/audio/MicCaptureSpec.scala`

- [ ] **Step 1: Write tests for byte-to-float audio conversion**

Create `src/test/scala/sangeet/audio/MicCaptureSpec.scala`:

```scala
package sangeet.audio

import org.scalatest.funsuite.AnyFunSuite

class MicCaptureSpec extends AnyFunSuite:

  test("bytesToFloats converts 16-bit signed little-endian PCM to [-1.0, 1.0] floats"):
    // Silence: two zero bytes = one sample of 0.0
    val silence = Array[Byte](0, 0)
    val result = MicCapture.bytesToFloats(silence, 2)
    assert(result.length == 1)
    assert(result(0) == 0.0f)

  test("bytesToFloats converts max positive sample"):
    // Max positive 16-bit signed = 0x7FFF = 32767
    // Little-endian: low byte first
    val maxPos = Array[Byte](0xFF.toByte, 0x7F.toByte)
    val result = MicCapture.bytesToFloats(maxPos, 2)
    assert(result.length == 1)
    assert(math.abs(result(0) - 1.0f) < 0.001f)

  test("bytesToFloats converts max negative sample"):
    // Max negative 16-bit signed = 0x8000 = -32768
    val maxNeg = Array[Byte](0x00.toByte, 0x80.toByte)
    val result = MicCapture.bytesToFloats(maxNeg, 2)
    assert(result.length == 1)
    assert(result(0) == -1.0f)

  test("bytesToFloats handles multiple samples"):
    // Two samples: 0x0100 (256) and 0x00FF (-256 in two's complement? No: 0xFF00 = -256)
    // Sample 1: little-endian 0x00, 0x01 = 256
    // Sample 2: little-endian 0x00, 0xFF = -256
    val data = Array[Byte](0x00, 0x01, 0x00, 0xFF.toByte)
    val result = MicCapture.bytesToFloats(data, 4)
    assert(result.length == 2)
    assert(math.abs(result(0) - (256.0f / 32768.0f)) < 0.001f)
    assert(math.abs(result(1) - (-256.0f / 32768.0f)) < 0.001f)

  test("bytesToFloats with odd byte count drops last incomplete sample"):
    val data = Array[Byte](0, 0, 0) // 1.5 samples — last byte dropped
    val result = MicCapture.bytesToFloats(data, 3)
    assert(result.length == 1)

  test("bytesToFloats with zero length returns empty"):
    val data = Array[Byte](0, 0)
    val result = MicCapture.bytesToFloats(data, 0)
    assert(result.isEmpty)
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `sbt "testOnly sangeet.audio.MicCaptureSpec"`
Expected: Compilation error — `MicCapture` does not exist.

- [ ] **Step 3: Implement MicCapture**

Create `src/main/scala/sangeet/audio/MicCapture.scala`:

```scala
package sangeet.audio

import javax.sound.sampled.*

/** Captures audio from the system microphone.
  * Audio format: 16kHz, 16-bit signed, mono, little-endian — as required by Whisper. */
class MicCapture extends AutoCloseable:
  import MicCapture.*

  private var line: Option[TargetDataLine] = None
  @volatile private var capturing = false
  private var buffer = new java.io.ByteArrayOutputStream()

  /** Open the microphone and start capturing audio.
    * Returns false if no microphone is available. */
  def start(): Boolean =
    try
      val info = new DataLine.Info(classOf[TargetDataLine], audioFormat)
      val mic = AudioSystem.getLine(info).asInstanceOf[TargetDataLine]
      mic.open(audioFormat)
      mic.start()
      line = Some(mic)
      capturing = true
      buffer.reset()

      val captureThread = new Thread(() => captureLoop(mic), "mic-capture")
      captureThread.setDaemon(true)
      captureThread.start()
      true
    catch
      case _: LineUnavailableException => false
      case _: IllegalArgumentException => false

  /** Stop capturing and return the captured audio as float samples in [-1.0, 1.0]. */
  def stop(): Array[Float] =
    capturing = false
    line.foreach { mic =>
      mic.stop()
      mic.close()
    }
    line = None
    val bytes = buffer.toByteArray
    buffer.reset()
    bytesToFloats(bytes, bytes.length)

  def isCapturing: Boolean = capturing

  def close(): Unit =
    capturing = false
    line.foreach { mic =>
      try { mic.stop(); mic.close() } catch case _: Exception => ()
    }
    line = None

  private def captureLoop(mic: TargetDataLine): Unit =
    val buf = new Array[Byte](ChunkSize)
    while capturing do
      val n = mic.read(buf, 0, buf.length)
      if n > 0 then
        buffer.synchronized { buffer.write(buf, 0, n) }

object MicCapture:
  val SampleRate: Float = 16000f
  val SampleSizeInBits: Int = 16
  val Channels: Int = 1
  private val ChunkSize = 3200 // 100ms of audio at 16kHz, 16-bit mono

  val audioFormat: AudioFormat = new AudioFormat(
    SampleRate,       // sample rate
    SampleSizeInBits, // sample size in bits
    Channels,         // channels (mono)
    true,             // signed
    false             // little-endian
  )

  /** Convert 16-bit signed little-endian PCM bytes to float samples in [-1.0, 1.0]. */
  def bytesToFloats(bytes: Array[Byte], length: Int): Array[Float] =
    val sampleCount = length / 2
    val floats = new Array[Float](sampleCount)
    var i = 0
    while i < sampleCount do
      val byteIdx = i * 2
      val sample = (bytes(byteIdx) & 0xFF) | (bytes(byteIdx + 1) << 8)
      floats(i) = sample.toShort.toFloat / 32768.0f
      i += 1
    floats
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `sbt "testOnly sangeet.audio.MicCaptureSpec"`
Expected: All 6 tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/sangeet/audio/MicCapture.scala src/test/scala/sangeet/audio/MicCaptureSpec.scala
git commit -m "feat: add MicCapture for microphone audio capture with byte-to-float conversion"
```

---

### Task 4: SwarRecognizer — Whisper Inference and Result Mapping

**Files:**
- Create: `src/main/scala/sangeet/audio/SwarRecognizer.scala`
- Create: `src/test/scala/sangeet/audio/SwarRecognizerSpec.scala`

- [ ] **Step 1: Write tests for swar text mapping**

Create `src/test/scala/sangeet/audio/SwarRecognizerSpec.scala`:

```scala
package sangeet.audio

import org.scalatest.funsuite.AnyFunSuite
import sangeet.model.Note

class SwarRecognizerSpec extends AnyFunSuite:

  test("parseSwarText maps lowercase swar names to Note"):
    assert(SwarRecognizer.parseSwarText("sa") == Some(Note.Sa))
    assert(SwarRecognizer.parseSwarText("re") == Some(Note.Re))
    assert(SwarRecognizer.parseSwarText("ga") == Some(Note.Ga))
    assert(SwarRecognizer.parseSwarText("ma") == Some(Note.Ma))
    assert(SwarRecognizer.parseSwarText("pa") == Some(Note.Pa))
    assert(SwarRecognizer.parseSwarText("dha") == Some(Note.Dha))
    assert(SwarRecognizer.parseSwarText("ni") == Some(Note.Ni))

  test("parseSwarText handles whitespace and case variations"):
    assert(SwarRecognizer.parseSwarText(" sa ") == Some(Note.Sa))
    assert(SwarRecognizer.parseSwarText("  dha  ") == Some(Note.Dha))
    assert(SwarRecognizer.parseSwarText("SA") == Some(Note.Sa))
    assert(SwarRecognizer.parseSwarText("Re") == Some(Note.Re))
    assert(SwarRecognizer.parseSwarText(" Ni ") == Some(Note.Ni))

  test("parseSwarText returns None for empty or unrecognized text"):
    assert(SwarRecognizer.parseSwarText("") == None)
    assert(SwarRecognizer.parseSwarText("   ") == None)
    assert(SwarRecognizer.parseSwarText("hello") == None)
    assert(SwarRecognizer.parseSwarText("[unk]") == None)

  test("parseSwarText handles Whisper JSON result format"):
    // Whisper sometimes outputs with surrounding artifacts
    assert(SwarRecognizer.parseSwarText("sa.") == Some(Note.Sa))
    assert(SwarRecognizer.parseSwarText("re,") == Some(Note.Re))

  test("parseWhisperResult extracts text from segment"):
    // Simulates what whisper-jni returns: just the text from fullGetSegmentText
    assert(SwarRecognizer.parseSwarText(" sa") == Some(Note.Sa))
    assert(SwarRecognizer.parseSwarText(" dha") == Some(Note.Dha))

  test("RecognitionResult sealed hierarchy"):
    val recognized: SwarRecognizer.RecognitionResult = SwarRecognizer.RecognitionResult.Recognized(Note.Sa)
    val unrecognized: SwarRecognizer.RecognitionResult = SwarRecognizer.RecognitionResult.Unrecognized("xyz")
    val noSpeech: SwarRecognizer.RecognitionResult = SwarRecognizer.RecognitionResult.NoSpeech
    val notReady: SwarRecognizer.RecognitionResult = SwarRecognizer.RecognitionResult.NotReady

    recognized match
      case SwarRecognizer.RecognitionResult.Recognized(note) => assert(note == Note.Sa)
      case _ => fail("Expected Recognized")
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `sbt "testOnly sangeet.audio.SwarRecognizerSpec"`
Expected: Compilation error — `SwarRecognizer` does not exist.

- [ ] **Step 3: Implement SwarRecognizer**

Create `src/main/scala/sangeet/audio/SwarRecognizer.scala`:

```scala
package sangeet.audio

import io.github.givimad.whisperjni.{WhisperJNI, WhisperContext, WhisperFullParams, WhisperGrammar}
import sangeet.model.Note
import java.nio.file.{Path, Files}

/** Wraps whisper-jni for recognizing spoken swar names.
  * Call `initialize(modelPath)` once, then `recognize(samples)` per utterance. */
class SwarRecognizer extends AutoCloseable:
  import SwarRecognizer.*

  private var ctx: WhisperContext = null
  private var grammar: WhisperGrammar = null
  private var initialized = false

  /** Load the Whisper model and GBNF grammar. Call once at startup.
    * Returns false if initialization fails. */
  def initialize(modelPath: Path): Boolean =
    try
      WhisperJNI.loadLibrary()
      val jni = new WhisperJNI()
      ctx = jni.init(modelPath)
      if ctx == null then return false

      // Load grammar from classpath resource
      val grammarStream = getClass.getResourceAsStream("/swar-grammar.gbnf")
      if grammarStream != null then
        val grammarText = new String(grammarStream.readAllBytes(), "UTF-8")
        grammarStream.close()
        // Write to temp file for whisper-jni's parseGrammar which takes a Path
        val tmpGrammar = Files.createTempFile("swar-grammar", ".gbnf")
        Files.writeString(tmpGrammar, grammarText)
        grammar = jni.parseGrammar(tmpGrammar)
        Files.deleteIfExists(tmpGrammar)

      initialized = true
      true
    catch
      case e: Exception =>
        e.printStackTrace()
        false

  /** Recognize a swar from audio samples. Samples must be 16kHz mono float PCM. */
  def recognize(samples: Array[Float]): RecognitionResult =
    if !initialized || ctx == null then return RecognitionResult.NotReady
    if samples.length < 1600 then return RecognitionResult.NoSpeech // < 100ms of audio

    try
      val jni = new WhisperJNI()
      val params = new WhisperFullParams()
      params.language = "hi"
      params.initialPrompt = "sa re ga ma pa dha ni"
      params.singleSegment = true
      params.noTimestamps = true
      params.suppressBlank = true
      params.suppressNonSpeechTokens = true
      params.temperature = 0.0f
      if grammar != null then
        params.grammar = grammar
        params.grammarPenalty = 100.0f

      val result = jni.full(ctx, params, samples, samples.length)
      if result != 0 then return RecognitionResult.NoSpeech

      val nSegments = jni.fullNSegments(ctx)
      if nSegments == 0 then return RecognitionResult.NoSpeech

      val text = jni.fullGetSegmentText(ctx, 0)
      parseSwarText(text) match
        case Some(note) => RecognitionResult.Recognized(note)
        case None       => RecognitionResult.Unrecognized(text)
    catch
      case e: Exception =>
        e.printStackTrace()
        RecognitionResult.NoSpeech

  def isReady: Boolean = initialized

  def close(): Unit =
    if ctx != null then
      ctx.close()
      ctx = null
    initialized = false

object SwarRecognizer:
  enum RecognitionResult:
    case Recognized(note: Note)
    case Unrecognized(rawText: String)
    case NoSpeech
    case NotReady

  private val swarMap: Map[String, Note] = Map(
    "sa"  -> Note.Sa,
    "re"  -> Note.Re,
    "ga"  -> Note.Ga,
    "ma"  -> Note.Ma,
    "pa"  -> Note.Pa,
    "dha" -> Note.Dha,
    "ni"  -> Note.Ni
  )

  /** Parse recognized text to a Note. Handles whitespace, case, trailing punctuation. */
  def parseSwarText(text: String): Option[Note] =
    if text == null then return None
    val cleaned = text.trim.toLowerCase.replaceAll("[^a-z]", "")
    swarMap.get(cleaned)
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `sbt "testOnly sangeet.audio.SwarRecognizerSpec"`
Expected: All 6 tests pass. (These tests only exercise the pure `parseSwarText` method and the `RecognitionResult` ADT — they don't need a Whisper model.)

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/sangeet/audio/SwarRecognizer.scala src/test/scala/sangeet/audio/SwarRecognizerSpec.scala
git commit -m "feat: add SwarRecognizer with whisper-jni inference and swar text mapping"
```

---

### Task 5: Download Whisper Model on First Run

**Files:**
- Create: `src/main/scala/sangeet/audio/WhisperModelManager.scala`
- Create: `src/test/scala/sangeet/audio/WhisperModelManagerSpec.scala`

The ggml-tiny.bin model is ~77MB — too large to bundle in the JAR. Instead, store it in a platform-specific app data directory and download on first use.

- [ ] **Step 1: Write tests for model path resolution**

Create `src/test/scala/sangeet/audio/WhisperModelManagerSpec.scala`:

```scala
package sangeet.audio

import org.scalatest.funsuite.AnyFunSuite

class WhisperModelManagerSpec extends AnyFunSuite:

  test("modelDir returns a path under user home"):
    val dir = WhisperModelManager.modelDir
    assert(dir.toString.contains("sangeet"))

  test("modelPath points to ggml-tiny.bin inside modelDir"):
    val path = WhisperModelManager.modelPath
    assert(path.getFileName.toString == "ggml-tiny.bin")
    assert(path.getParent == WhisperModelManager.modelDir)

  test("downloadUrl points to HuggingFace ggml-tiny.bin"):
    val url = WhisperModelManager.downloadUrl
    assert(url.contains("ggml-tiny.bin"))
    assert(url.contains("huggingface"))
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `sbt "testOnly sangeet.audio.WhisperModelManagerSpec"`
Expected: Compilation error — `WhisperModelManager` does not exist.

- [ ] **Step 3: Implement WhisperModelManager**

Create `src/main/scala/sangeet/audio/WhisperModelManager.scala`:

```scala
package sangeet.audio

import java.nio.file.{Path, Files}
import java.net.{URI, URL}

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

  /** Download the model file. Calls `onProgress(bytesDownloaded, totalBytes)` during download.
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
      // Atomic move from tmp to final path
      Files.move(tmpFile, modelPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
      true
    catch
      case e: Exception =>
        e.printStackTrace()
        false
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `sbt "testOnly sangeet.audio.WhisperModelManagerSpec"`
Expected: All 3 tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/sangeet/audio/WhisperModelManager.scala src/test/scala/sangeet/audio/WhisperModelManagerSpec.scala
git commit -m "feat: add WhisperModelManager for model download and path resolution"
```

---

### Task 6: Integrate Push-to-Talk into EditorPane

**Files:**
- Modify: `src/main/scala/sangeet/editor/EditorPane.scala`

This is the core integration. Space is currently used for Rest input. We need a **voice mode toggle** — when voice mode is off (default), Space inserts Rest as before. When voice mode is on, Space triggers push-to-talk.

- [ ] **Step 1: Add voice mode state and recognizer fields to EditorPane**

In `EditorPane.scala`, add these imports at the top (after the existing imports):

```scala
import sangeet.audio.{SwarRecognizer, MicCapture, WhisperModelManager}
```

Add these fields after the `private val saveTimerScheduler` line (around line 74):

```scala
  // Voice input mode
  private var voiceMode: Boolean = false
  private var voiceRecognizer: Option[SwarRecognizer] = None
  private var micCapture: Option[MicCapture] = None
  @volatile private var voiceCapturing = false
```

- [ ] **Step 2: Add voice mode initialization method**

Add this method after the `autoSave()` method (around line 159):

```scala
  /** Initialize voice recognition. Downloads model if needed.
    * Returns a status message. */
  def initializeVoice(onProgress: (Long, Long) => Unit = (_, _) => ()): String =
    if voiceRecognizer.exists(_.isReady) then
      voiceMode = true
      return "Voice input ready — hold Space to speak a swar"

    if !WhisperModelManager.isModelAvailable then
      val downloaded = WhisperModelManager.downloadModel(onProgress)
      if !downloaded then
        return "✗ Failed to download Whisper model"

    val recognizer = new SwarRecognizer()
    val success = recognizer.initialize(WhisperModelManager.modelPath)
    if success then
      voiceRecognizer = Some(recognizer)
      micCapture = Some(new MicCapture())
      voiceMode = true
      "Voice input ready — hold Space to speak a swar"
    else
      "✗ Failed to initialize voice recognition"

  def disableVoice(): Unit =
    voiceMode = false
    micCapture.foreach(_.close())
    statusBar.log("Voice input disabled — Space inserts rest")

  def isVoiceMode: Boolean = voiceMode
```

- [ ] **Step 3: Add push-to-talk key handlers**

In the `setOnKeyPressed` handler, find the block that handles `KeyCode.Space` (the line `case KeyCode.Space =>`). Replace this block:

```scala
            case KeyCode.Space =>
              e.consume()
              val (ne, m) = KeyHandler.handleSpecialKey(ed, "SPACE")
              EditAction.ContentChange(ne, m)
```

With:

```scala
            case KeyCode.Space =>
              e.consume()
              if voiceMode && !voiceCapturing then
                // Start push-to-talk capture
                micCapture.foreach { mic =>
                  if mic.start() then
                    voiceCapturing = true
                    EditAction.CursorMove(ed, "🎤 Listening... (release Space when done)")
                  else
                    EditAction.CursorMove(ed, "✗ Microphone not available")
                }
                EditAction.CursorMove(ed, "✗ Voice not initialized")
              else if !voiceMode then
                val (ne, m) = KeyHandler.handleSpecialKey(ed, "SPACE")
                EditAction.ContentChange(ne, m)
              else
                EditAction.NoOp  // Space held down, already capturing
```

- [ ] **Step 4: Add key-release handler for voice recognition**

Add a new `setOnKeyReleased` handler after the `setOnKeyTyped` handler (after line 560):

```scala
  scrollPane.delegate.setOnKeyReleased { (e: javafx.scene.input.KeyEvent) =>
    if voiceCapturing && KeyCode.jfxEnum2sfx(e.getCode) == KeyCode.Space then
      e.consume()
      voiceCapturing = false

      // Get captured audio and run recognition on background thread
      val samples = micCapture.map(_.stop()).getOrElse(Array.empty[Float])

      editor.foreach { ed =>
        if samples.length < 1600 then
          // Less than 100ms of audio — too short
          statusBar.log("✗ Too short — hold Space longer while speaking")
        else
          // Run inference on a background thread to avoid blocking UI
          val recognizer = voiceRecognizer.getOrElse(return)
          val capturedEditor = ed
          val capturedOctave = ed.cursor.currentOctave

          new Thread(() =>
            val result = recognizer.recognize(samples)
            javafx.application.Platform.runLater(() =>
              // Re-read editor state — it may have changed while inference ran
              editor.foreach { currentEd =>
                result match
                  case SwarRecognizer.RecognitionResult.Recognized(note) =>
                    val event = Event.Swar(
                      note = note,
                      variant = Variant.Shuddha,
                      octave = capturedOctave,
                      beat = currentEd.cursor.position,
                      duration = Rational(1, currentEd.cursor.totalSubdivisions),
                      stroke = None,
                      ornaments = Nil,
                      sahitya = None
                    )
                    val newEditor = currentEd.addEvent(event)
                    val newCursor = currentEd.cursor.nextSubBeat.withOctave(Octave.Madhya)
                    statusBar.log(s"🎤 ${note}")
                    pushEditor(newEditor.copy(cursor = newCursor))
                    resetBlink()
                    redraw()
                  case SwarRecognizer.RecognitionResult.Unrecognized(raw) =>
                    statusBar.log(s"✗ Could not recognize: '$raw' — try again")
                  case SwarRecognizer.RecognitionResult.NoSpeech =>
                    statusBar.log("✗ No speech detected — speak clearly into mic")
                  case SwarRecognizer.RecognitionResult.NotReady =>
                    statusBar.log("✗ Voice recognition not ready")
              }
            )
          , "whisper-inference").start()
      }
  }
```

- [ ] **Step 5: Add visual indicator for voice mode and active capture**

In the `redraw()` method, add a voice mode indicator after the canvas rendering. Find the `redraw()` method and add at the end, inside the `editor.foreach` block, after the height adjustment:

```scala
      // Draw voice mode indicator
      if voiceMode then
        val gc = canvas.graphicsContext2D
        gc.save()
        if voiceCapturing then
          gc.fill = scalafx.scene.paint.Color.Red
          gc.fillOval(canvas.width.value - 30, 10, 16, 16)
        else
          gc.fill = scalafx.scene.paint.Color.DarkGray
          gc.fillOval(canvas.width.value - 30, 10, 12, 12)
        gc.restore()
```

- [ ] **Step 6: Run full test suite**

Run: `sbt test`
Expected: All existing tests pass (the new voice code paths are not exercised by existing tests — they require UI interaction and a real microphone).

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/sangeet/editor/EditorPane.scala
git commit -m "feat: integrate push-to-talk voice swar input into EditorPane"
```

---

### Task 7: Add Voice Input Menu Item to MainApp

**Files:**
- Modify: `src/main/scala/sangeet/editor/MainApp.scala`

- [ ] **Step 1: Add Voice Input toggle to the Composition menu**

In `MainApp.scala`, find the `new Menu("Composition")` block. Add a new menu item after the "Change Script..." item, before the closing `)` of the Composition menu's `items` list:

```scala
            ,
            new SeparatorMenuItem(),
            new MenuItem("Enable Voice Input"):
              onAction = _ =>
                if editorPane.isVoiceMode then
                  editorPane.disableVoice()
                  this.text = "Enable Voice Input"
                else
                  statusBar.log("Initializing voice recognition...")
                  // Run model download/init on background thread
                  val menuItem = this
                  new Thread(() =>
                    val msg = editorPane.initializeVoice { (downloaded, total) =>
                      val pct = if total > 0 then (downloaded * 100 / total).toInt else -1
                      javafx.application.Platform.runLater(() =>
                        if pct >= 0 then statusBar.log(s"Downloading Whisper model... ${pct}%")
                        else statusBar.log(s"Downloading Whisper model... ${downloaded / 1024}KB")
                      )
                    }
                    javafx.application.Platform.runLater(() =>
                      statusBar.log(msg)
                      if editorPane.isVoiceMode then
                        menuItem.text = "Disable Voice Input"
                    )
                  , "voice-init").start()
                editorPane.requestFocus()
```

- [ ] **Step 2: Clean up voice resources on app close**

In `MainApp.scala`, find the `stage.delegate.setOnCloseRequest` line. Add voice cleanup:

```scala
    stage.delegate.setOnCloseRequest { _ =>
      playbackController.shutdown()
      // Voice recognizer cleanup happens via EditorPane.disableVoice()
      editorPane.disableVoice()
    }
```

- [ ] **Step 3: Run full test suite**

Run: `sbt test`
Expected: All tests pass.

- [ ] **Step 4: Commit**

```bash
git add src/main/scala/sangeet/editor/MainApp.scala
git commit -m "feat: add Voice Input toggle menu item with model download progress"
```

---

### Task 8: Update Keyboard Legend with Voice Input

**Files:**
- Modify: `src/main/scala/sangeet/editor/KeyboardLegend.scala`

- [ ] **Step 1: Read the current KeyboardLegend file**

Read `src/main/scala/sangeet/editor/KeyboardLegend.scala` to understand the existing structure.

- [ ] **Step 2: Add voice input section to the legend**

Find the last section in the keyboard legend and add a new section for voice input. The exact code depends on the file structure, but add a section like:

```
Voice Input (Composition > Enable Voice Input):
  Space (hold) — Speak a swar name
  Release — Insert recognized note
  Octave: set with . ' ` before speaking
```

Add this to whatever data structure or string the legend uses to display shortcuts.

- [ ] **Step 3: Run full test suite**

Run: `sbt test`
Expected: All tests pass.

- [ ] **Step 4: Commit**

```bash
git add src/main/scala/sangeet/editor/KeyboardLegend.scala
git commit -m "feat: add voice input shortcuts to keyboard legend"
```

---

### Task 9: End-to-End Manual Test and Assembly Verification

- [ ] **Step 1: Run full test suite**

Run: `sbt test`
Expected: All tests pass (existing 284+ tests plus new tests from Tasks 3, 4, 5).

- [ ] **Step 2: Verify assembly builds**

Run: `sbt assembly`
Expected: Fat JAR builds successfully. whisper-jni native libraries are included in the JAR.

- [ ] **Step 3: Launch and test voice input**

Run: `sbt run`

Manual test checklist:
1. App launches with sample composition
2. File > New to create a new composition
3. Composition > Enable Voice Input
   - First time: "Downloading Whisper model..." progress in status bar
   - Download completes: "Voice input ready" message
4. Hold Space → speak "Sa" → release Space
   - Status bar shows "🎤 Sa"
   - Sa note appears at cursor position
5. Set octave with `.` (mandra), then hold Space → speak "Re"
   - Re in mandra octave inserted
6. Toggle off: Composition > Disable Voice Input
   - Space now inserts Rest again
7. Normal keyboard input still works (s/r/g/m/p/d/n)
8. Undo/redo works for voice-inserted notes

- [ ] **Step 4: Commit any fixes from manual testing**

```bash
git add -A
git commit -m "fix: address issues found during voice input manual testing"
```

---

### Task 10: Update CLAUDE.md with Voice Input Documentation

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: Update the "What's Built" section**

Add to the "What's Built" section in CLAUDE.md:

```markdown
- Voice swar recognition via push-to-talk (whisper-jni + Whisper tiny model)
```

- [ ] **Step 2: Update the module layout**

Add to the module layout in CLAUDE.md under `audio/`:

```markdown
  audio/        — Playback: PlaybackScheduler, MidiEngine, PlaybackController
                  Voice: SwarRecognizer, MicCapture, WhisperModelManager
```

- [ ] **Step 3: Add voice input to domain knowledge or key design decisions if relevant**

Add a brief note under Key Design Decisions:

```markdown
- Voice input uses push-to-talk with GBNF grammar constraint (not continuous listening)
- Whisper tiny model (~77MB) downloaded on first use, stored in platform app data directory
```

- [ ] **Step 4: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: update CLAUDE.md with voice swar recognition details"
```

---

## Summary

| Task | Component | Tests |
|------|-----------|-------|
| 1 | build.sbt dependency | compile check |
| 2 | GBNF grammar resource | classpath check |
| 3 | MicCapture | 6 unit tests (byte→float conversion) |
| 4 | SwarRecognizer | 6 unit tests (text→Note mapping) |
| 5 | WhisperModelManager | 3 unit tests (path resolution) |
| 6 | EditorPane integration | manual (requires mic + model) |
| 7 | MainApp menu item | manual |
| 8 | KeyboardLegend update | visual |
| 9 | E2E manual test | manual checklist |
| 10 | CLAUDE.md docs | — |

**Total new tests:** 15 automated + manual E2E checklist
**New files:** 6 (3 main + 3 test)
**Modified files:** 4 (build.sbt, EditorPane, MainApp, KeyboardLegend, CLAUDE.md)
