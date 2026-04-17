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

  test("parseSwarText handles Whisper artifacts like trailing punctuation"):
    assert(SwarRecognizer.parseSwarText("sa.") == Some(Note.Sa))
    assert(SwarRecognizer.parseSwarText("re,") == Some(Note.Re))

  test("parseSwarText handles Whisper leading space tokens"):
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
