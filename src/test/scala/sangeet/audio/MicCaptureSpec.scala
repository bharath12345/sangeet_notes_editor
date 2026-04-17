package sangeet.audio

import org.scalatest.funsuite.AnyFunSuite

class MicCaptureSpec extends AnyFunSuite:

  test("bytesToFloats converts 16-bit signed little-endian PCM to [-1.0, 1.0] floats"):
    val silence = Array[Byte](0, 0)
    val result = MicCapture.bytesToFloats(silence, 2)
    assert(result.length == 1)
    assert(result(0) == 0.0f)

  test("bytesToFloats converts max positive sample"):
    val maxPos = Array[Byte](0xFF.toByte, 0x7F.toByte)
    val result = MicCapture.bytesToFloats(maxPos, 2)
    assert(result.length == 1)
    assert(math.abs(result(0) - 1.0f) < 0.001f)

  test("bytesToFloats converts max negative sample"):
    val maxNeg = Array[Byte](0x00.toByte, 0x80.toByte)
    val result = MicCapture.bytesToFloats(maxNeg, 2)
    assert(result.length == 1)
    assert(result(0) == -1.0f)

  test("bytesToFloats handles multiple samples"):
    val data = Array[Byte](0x00, 0x01, 0x00, 0xFF.toByte)
    val result = MicCapture.bytesToFloats(data, 4)
    assert(result.length == 2)
    assert(math.abs(result(0) - (256.0f / 32768.0f)) < 0.001f)
    assert(math.abs(result(1) - (-256.0f / 32768.0f)) < 0.001f)

  test("bytesToFloats with odd byte count drops last incomplete sample"):
    val data = Array[Byte](0, 0, 0)
    val result = MicCapture.bytesToFloats(data, 3)
    assert(result.length == 1)

  test("bytesToFloats with zero length returns empty"):
    val data = Array[Byte](0, 0)
    val result = MicCapture.bytesToFloats(data, 0)
    assert(result.isEmpty)
