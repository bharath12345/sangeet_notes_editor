package sangeet.layout

case class LayoutConfig(
  highDensityThreshold: Int = 5,    // notes per beat above which we split lines
  cellWidthBase: Double = 60.0,     // base cell width in pixels
  cellOverflowExpand: Double = 15.0, // extra width per note above 1
  lineSpacing: Double = 40.0,       // vertical space between lines
  headerHeight: Double = 120.0      // space for composition header
)
