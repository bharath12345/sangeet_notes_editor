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
    SampleRate,
    SampleSizeInBits,
    Channels,
    true,   // signed
    false   // little-endian
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
