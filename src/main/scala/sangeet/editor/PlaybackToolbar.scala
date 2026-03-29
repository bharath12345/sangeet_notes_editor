package sangeet.editor

import scalafx.scene.control.{Button, CheckBox, Label, Slider, Separator, ToolBar}
import scalafx.scene.layout.{HBox, Priority}
import scalafx.geometry.{Insets, Pos}
import sangeet.model.Laya

class PlaybackToolbar extends ToolBar:
  private val playBtn = new Button("Play"):
    style = "-fx-font-size: 12px;"
  private val pauseBtn = new Button("Pause"):
    style = "-fx-font-size: 12px;"
    disable = true
  private val stopBtn = new Button("Stop"):
    style = "-fx-font-size: 12px;"
    disable = true

  private val loopCheck = new CheckBox("Loop"):
    style = "-fx-font-size: 11px;"

  private val bpmLabel = new Label("BPM:"):
    style = "-fx-font-size: 11px;"
  private val bpmSlider = new Slider(10, 300, 60):
    prefWidth = 150
    showTickMarks = true
    showTickLabels = true
    majorTickUnit = 50
    blockIncrement = 5
  private val bpmValue = new Label("60"):
    style = "-fx-font-size: 11px; -fx-min-width: 30;"

  bpmSlider.value.addListener { (_, _, newVal) =>
    bpmValue.text = newVal.intValue.toString
  }

  items = List(playBtn, pauseBtn, stopBtn, new Separator(),
    loopCheck, new Separator(), bpmLabel, bpmSlider, bpmValue)

  private var onPlay: () => Unit = () => ()
  private var onPause: () => Unit = () => ()
  private var onStop: () => Unit = () => ()

  playBtn.onAction = _ => onPlay()
  pauseBtn.onAction = _ => onPause()
  stopBtn.onAction = _ => onStop()

  def setOnPlay(f: () => Unit): Unit = onPlay = f
  def setOnPause(f: () => Unit): Unit = onPause = f
  def setOnStop(f: () => Unit): Unit = onStop = f

  def bpm: Double = bpmSlider.value.value
  def isLoop: Boolean = loopCheck.selected.value

  def setBpm(value: Double): Unit =
    bpmSlider.value = value
    bpmValue.text = value.toInt.toString

  def setBpmForLaya(laya: Option[Laya]): Unit =
    val bpmVal = laya match
      case Some(Laya.AtiVilambit) => 30.0
      case Some(Laya.Vilambit)    => 40.0
      case Some(Laya.Madhya)      => 80.0
      case Some(Laya.Drut)        => 160.0
      case Some(Laya.AtiDrut)     => 250.0
      case None                   => 60.0
    setBpm(bpmVal)

  def setPlaying(playing: Boolean): Unit =
    playBtn.disable = playing
    pauseBtn.disable = !playing
    stopBtn.disable = !playing

  def setPaused(paused: Boolean): Unit =
    playBtn.disable = !paused
    pauseBtn.disable = paused
