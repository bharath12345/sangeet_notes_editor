package sangeet.model

case class BeatPosition(
  cycle: Int,
  beat: Int,
  subdivision: Rational
) extends Ordered[BeatPosition]:

  def compare(that: BeatPosition): Int =
    val c = this.cycle compare that.cycle
    if c != 0 then c
    else
      val b = this.beat compare that.beat
      if b != 0 then b
      else this.subdivision compare that.subdivision
