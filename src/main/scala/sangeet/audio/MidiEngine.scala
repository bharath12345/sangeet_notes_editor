package sangeet.audio

import javax.sound.midi.*
import sangeet.model.*

class MidiEngine extends SoundEngine:
  private var synthesizer: Option[Synthesizer] = None
  private var channel: Option[MidiChannel] = None

  private def midiNote(note: Note, variant: Variant, octave: Octave): Int =
    val baseNote = note match
      case Note.Sa  => 0
      case Note.Re  => 2
      case Note.Ga  => 4
      case Note.Ma  => 5
      case Note.Pa  => 7
      case Note.Dha => 9
      case Note.Ni  => 11

    val alteration = variant match
      case Variant.Komal => -1
      case Variant.Tivra => 1
      case Variant.Shuddha => 0

    val octaveOffset = octave match
      case Octave.AtiMandra => -24
      case Octave.Mandra    => -12
      case Octave.Madhya    => 0
      case Octave.Taar      => 12
      case Octave.AtiTaar   => 24

    60 + baseNote + alteration + octaveOffset

  override def init(): Unit =
    val synth = MidiSystem.getSynthesizer
    synth.open()
    synthesizer = Some(synth)
    val channels = synth.getChannels
    channel = Some(channels(0))
    channels(0).programChange(103) // Sitar

  override def playNote(timedNote: TimedNote): Unit =
    channel.foreach { ch =>
      val midi = midiNote(timedNote.note, timedNote.variant, timedNote.octave)
      val velocity = 80
      ch.noteOn(midi, velocity)
      Thread.sleep(timedNote.durationMs.min(500))
      ch.noteOff(midi)
    }

  override def stop(): Unit =
    channel.foreach(_.allNotesOff())

  override def shutdown(): Unit =
    synthesizer.foreach(_.close())
    synthesizer = None
    channel = None
