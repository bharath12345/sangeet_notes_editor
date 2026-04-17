package sangeet.audio

import io.github.givimad.whisperjni.{WhisperJNI, WhisperContext, WhisperFullParams}
import sangeet.model.Note
import java.nio.file.{Path, Files}

/** Wraps whisper-jni for recognizing spoken swar names.
  * Call initialize(modelPath) once, then recognize(samples) per utterance. */
class SwarRecognizer extends AutoCloseable:
  import SwarRecognizer.*

  private var jni: WhisperJNI = null
  private var ctx: WhisperContext = null
  private var initialized = false

  /** Load the Whisper model and GBNF grammar. Call once at startup.
    * Returns false if initialization fails. */
  def initialize(modelPath: Path): Boolean =
    try
      WhisperJNI.loadLibrary()
      jni = new WhisperJNI()
      ctx = jni.init(modelPath)
      if ctx == null then return false
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
      val params = new WhisperFullParams()
      params.language = "hi"
      params.initialPrompt = "sa re ga ma pa dha ni"
      params.singleSegment = true
      params.noTimestamps = true
      params.suppressBlank = true
      params.suppressNonSpeechTokens = true
      params.temperature = 0.0f

      // Load and apply GBNF grammar if available
      val grammarStream = getClass.getResourceAsStream("/swar-grammar.gbnf")
      if grammarStream != null then
        val grammarText = new String(grammarStream.readAllBytes(), "UTF-8")
        grammarStream.close()
        val tmpGrammar = Files.createTempFile("swar-grammar", ".gbnf")
        Files.writeString(tmpGrammar, grammarText)
        try
          val grammar = jni.parseGrammar(tmpGrammar)
          if grammar != null then
            params.grammar = grammar
            params.grammarPenalty = 100.0f
        finally
          Files.deleteIfExists(tmpGrammar)

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
