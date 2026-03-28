package sangeet.model

case class Rational private (numerator: Int, denominator: Int) extends Ordered[Rational]:

  def toDouble: Double = numerator.toDouble / denominator.toDouble

  def +(other: Rational): Rational =
    Rational(
      numerator * other.denominator + other.numerator * denominator,
      denominator * other.denominator
    )

  def compare(that: Rational): Int =
    (this.numerator * that.denominator) compare (that.numerator * this.denominator)

  override def equals(obj: Any): Boolean = obj match
    case r: Rational => numerator * r.denominator == r.numerator * denominator
    case _           => false

  override def hashCode(): Int =
    val n = Rational.normalize(numerator, denominator)
    (n._1, n._2).hashCode()

object Rational:
  def apply(num: Int, den: Int): Rational =
    require(den != 0, "Denominator cannot be zero")
    val (n, d) = normalize(num, den)
    new Rational(n, d)

  val onBeat: Rational = Rational(0, 1)
  val fullBeat: Rational = Rational(1, 1)

  private def normalize(num: Int, den: Int): (Int, Int) =
    if num == 0 then (0, 1)
    else
      val g = gcd(math.abs(num), math.abs(den))
      val sign = if den < 0 then -1 else 1
      (sign * num / g, sign * den / g)

  private def gcd(a: Int, b: Int): Int =
    if b == 0 then a else gcd(b, a % b)
