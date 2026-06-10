package com.varpas.sangeet.desktop

import scala.io.Source
import scala.util.Using

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.{Label, ListView, SplitPane}
import scalafx.scene.layout.BorderPane
import scalafx.scene.web.WebView
import scalafx.stage.Stage

object UserGuideViewer:

  private val files = List(
    "01-getting-started.md"       -> "Getting Started",
    "02-creating-compositions.md" -> "Creating Compositions",
    "03-entering-notes.md"        -> "Entering Notes",
    "04-ornaments-strokes.md"     -> "Ornaments & Strokes",
    "05-sections-navigation.md"   -> "Sections & Navigation",
    "06-editing-clipboard.md"     -> "Editing & Clipboard",
    "07-file-operations.md"       -> "File Operations",
    "08-keyboard-reference.md"    -> "Keyboard Reference",
    "09-taals-raags.md"           -> "Taals & Raags",
    "10-starting-beat.md"         -> "Starting Beat"
  )

  private lazy val parser   = Parser.builder(new MutableDataSet()).build()
  private lazy val renderer = HtmlRenderer.builder(new MutableDataSet()).build()

  private def readResource(name: String): Option[String] =
    Option(getClass.getResourceAsStream(s"/user-guide/$name")).map { stream =>
      Using.resource(Source.fromInputStream(stream, "UTF-8"))(_.mkString)
    }

  private def renderHtml(markdown: String): String =
    renderer.render(parser.parse(markdown))

  private val css = """
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
           font-size: 14px; line-height: 1.6; color: #2D2926; padding: 16px 28px; max-width: 760px;
           background: #FDF6EC; }
    h1 { color: #8B1A1A; border-bottom: 2px solid #E8A317; padding-bottom: 6px; margin-top: 0; }
    h2 { color: #5A2828; margin-top: 24px; }
    h3 { color: #4A2F2F; margin-top: 18px; }
    code { background: #F5EDE3; padding: 2px 5px; border-radius: 3px; font-size: 13px;
           font-family: 'SF Mono', Menlo, Consolas, monospace; }
    pre  { background: #FFF8F0; border: 1px solid #E8A317; padding: 10px; border-radius: 4px;
           overflow-x: auto; }
    pre code { background: transparent; padding: 0; }
    table { border-collapse: collapse; margin: 12px 0; }
    th, td { border: 1px solid #D4C8B8; padding: 6px 10px; text-align: left; }
    th { background: #F5EDE3; color: #5A2828; }
    blockquote { border-left: 4px solid #E8A317; margin: 8px 0; padding: 4px 12px;
                 background: #FFF8F0; color: #4A2F2F; }
    a { color: #8B1A1A; }
  """

  private def wrapHtml(body: String, title: String): String =
    s"<!DOCTYPE html><html><head><meta charset='utf-8'><title>$title</title><style>$css</style></head><body>$body</body></html>"

  /** Show the user guide in a new window. */
  def show(owner: javafx.stage.Stage): Unit =
    val toc = new ListView[String](files.map(_._2)):
      prefWidth = 220
    toc.selectionModel.value.selectFirst()

    val webView = new WebView

    def load(idx: Int): Unit =
      if idx >= 0 && idx < files.length then
        val (file, title) = files(idx)
        val body = readResource(file)
          .map(renderHtml)
          .getOrElse(s"<p>User guide file <code>$file</code> not found in resources.</p>")
        webView.engine.loadContent(wrapHtml(body, title))

    load(0)

    toc.selectionModel.value.selectedIndexProperty.addListener { (_, _, newIdx) =>
      load(newIdx.intValue)
    }

    val split = new SplitPane:
      items.addAll(toc, webView)
    split.setDividerPosition(0, 0.22)

    val rootPane = new BorderPane:
      center = split
      top = new Label("Sangeet Notes Editor — User Guide"):
        style = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8 12 8 12; -fx-background-color: #F5EDE3;"
      padding = Insets(0)

    val viewerStage = new Stage:
      width = 950
      height = 650
      scene = new Scene:
        root = rootPane
    viewerStage.title = "User Guide — Sangeet Notes Editor"
    viewerStage.initOwner(owner)
    viewerStage.show()

  /** Render the entire user guide as a single HTML document — for export or future re-use. */
  def renderAllHtml: String =
    val body = files
      .flatMap { (file, title) =>
        readResource(file).map(md => s"<h1 id='${file.stripSuffix(".md")}'>$title</h1>\n${renderHtml(md)}")
      }
      .mkString("<hr/>")
    wrapHtml(body, "Sangeet Notes Editor — User Guide")
