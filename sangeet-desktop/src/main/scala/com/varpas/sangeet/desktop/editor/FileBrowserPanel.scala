package com.varpas.sangeet.desktop.editor

import java.nio.file.{Files, Path}

import scala.collection.mutable.ListBuffer

import javafx.scene.control.{TreeItem => JTreeItem}
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control._
import scalafx.scene.input.InputIncludes.jfxMouseEvent2sfx
import scalafx.scene.layout.{HBox, Priority, Region, VBox}

import com.varpas.sangeet.core.config.BookmarkEntry

class FileBrowserPanel(tabManager: TabManager, statusBar: StatusBar):

  private val rootDirs        = ListBuffer.empty[Path]
  private val bookmarkedPaths = ListBuffer.empty[Path]

  private val treeRoot = new TreeItem[FileTreeItem](DirectoryItem(Path.of("/"), "Files", isBookmarked = false)):
    expanded = true

  private val treeView = new TreeView[FileTreeItem](treeRoot):
    showRoot = false
    cellFactory = (tv: TreeView[FileTreeItem]) =>
      new TreeCell[FileTreeItem]:
        item.onChange { (_, _, newItem) =>
          if newItem == null then
            text = ""
            graphic = null
            contextMenu = null
          else
            val icon = newItem match
              case _: DirectoryItem => "📁 "
              case _: SwarFileItem  => "🎵 "
              case _: HtmlFileItem  => "🌐 "
            val star = if newItem.isBookmarked then " ★" else ""
            text = icon + newItem.name + star
            graphic = null
            contextMenu = buildContextMenu(newItem)
        }

  treeView.onMouseClicked = event =>
    if event.clickCount == 2 then
      val sel = treeView.selectionModel.value.getSelectedItem
      if sel != null then
        sel.getValue match
          case f: SwarFileItem => tabManager.openFile(f.path)
          case f: HtmlFileItem => tabManager.openHtml(f.path)
          case _               => ()

  private val headerLabel = new Label("FILES"):
    style = "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 8 4 8;"

  private val addFolderBtn = new Button("+"):
    style = "-fx-font-size: 10px; -fx-padding: 2 6 2 6;"
    tooltip = new Tooltip("Add a folder")
    onAction = _ =>
      val dc = new scalafx.stage.DirectoryChooser:
        title = "Add Folder"
      val ownerWindow = panel.scene.value.getWindow
      val dir         = dc.showDialog(ownerWindow)
      if dir != null then addDirectory(dir.toPath)

  private val headerSpacer = new Region()
  HBox.setHgrow(headerSpacer, Priority.Always)

  private val headerBox = new HBox(4):
    alignment = Pos.CenterLeft
    padding = Insets(2, 4, 2, 4)
    children = Seq(headerLabel, headerSpacer, addFolderBtn)

  VBox.setVgrow(treeView, Priority.Always)

  def setCollapseButton(btn: Button): Unit =
    headerBox.children = Seq(headerLabel, headerSpacer, addFolderBtn, btn)

  val panel: VBox = new VBox:
    children = Seq(headerBox, treeView)
    minWidth = 100
    prefWidth = 250

  def addDirectory(dir: Path): Unit =
    if !Files.isDirectory(dir) then
      statusBar.log(s"Not a directory: $dir")
      return
    if rootDirs.contains(dir) then
      statusBar.log(s"Folder already open: ${dir.getFileName}")
      return
    rootDirs += dir
    val dirItem = buildTreeItem(dir, isRoot = true)
    treeRoot.children.add(dirItem)
    AppLogger.info(s"Added directory to browser: $dir")
    statusBar.log(s"Added folder: ${dir.getFileName}")

  def removeDirectory(dir: Path): Unit =
    rootDirs -= dir
    val idx = (0 until treeRoot.children.size).find { i =>
      treeRoot.children.get(i).getValue match
        case d: DirectoryItem => d.path == dir
        case _                => false
    }
    idx.foreach(i => treeRoot.children.remove(i))

  def refreshAll(): Unit =
    treeRoot.children.clear()
    rootDirs.foreach { dir =>
      if Files.isDirectory(dir) then treeRoot.children.add(buildTreeItem(dir, isRoot = true))
    }

  def toggleBookmark(path: Path): Unit =
    if bookmarkedPaths.contains(path) then bookmarkedPaths -= path
    else bookmarkedPaths += path
    refreshAll()

  def setBookmarks(entries: List[BookmarkEntry]): Unit =
    bookmarkedPaths.clear()
    entries.foreach { e =>
      val p = Path.of(e.path)
      bookmarkedPaths += p
      if e.isDirectory && !rootDirs.contains(p) then rootDirs += p
    }
    refreshAll()

  def getBookmarks: List[BookmarkEntry] =
    bookmarkedPaths.toList.map { p =>
      val isDir = Files.isDirectory(p)
      BookmarkEntry(p.toString, isDir, p.getFileName.toString)
    }

  def getRootDirs: List[Path] = rootDirs.toList

  private def buildTreeItem(path: Path, isRoot: Boolean = false): JTreeItem[FileTreeItem] =
    val isBookmarked = bookmarkedPaths.contains(path)
    val name         = path.getFileName.toString
    val item         = new JTreeItem[FileTreeItem](DirectoryItem(path, name, isBookmarked))

    if Files.isDirectory(path) then
      try
        val stream = Files.list(path)
        try
          val entries = stream.toArray.map(_.asInstanceOf[Path]).toList.sortBy(_.getFileName.toString)
          val dirs    = entries.filter(Files.isDirectory(_))
          val files = entries.filter { p =>
            !Files.isDirectory(p) && {
              val n = p.getFileName.toString.toLowerCase
              n.endsWith(".swar") || n.endsWith(".html")
            }
          }
          dirs.foreach { d =>
            item.getChildren.add(buildTreeItem(d))
          }
          files.foreach { f =>
            val fname = f.getFileName.toString
            val bm    = bookmarkedPaths.contains(f)
            val fileItem: FileTreeItem =
              if fname.toLowerCase.endsWith(".swar") then SwarFileItem(f, fname, bm)
              else HtmlFileItem(f, fname, bm)
            item.getChildren.add(new JTreeItem[FileTreeItem](fileItem))
          }
        finally stream.close()
      catch case _: Exception => ()

    if isRoot then item.setExpanded(true)
    item

  private def buildContextMenu(item: FileTreeItem): ContextMenu =
    item match
      case d: DirectoryItem =>
        new ContextMenu(
          new MenuItem("New .swar File"):
            onAction = _ => createNewFile(d.path)
          ,
          new MenuItem("New Folder"):
            onAction = _ => createNewFolder(d.path)
          ,
          new SeparatorMenuItem(),
          new MenuItem(if d.isBookmarked then "Remove Bookmark" else "Bookmark"):
            onAction = _ =>
              toggleBookmark(d.path)
              statusBar.log(
                if bookmarkedPaths.contains(d.path) then s"Bookmarked: ${d.name}"
                else s"Removed bookmark: ${d.name}"
              )
          ,
          new MenuItem("Refresh"):
            onAction = _ => refreshAll()
          ,
          new SeparatorMenuItem(),
          new MenuItem("Remove from Browser"):
            onAction = _ =>
              removeDirectory(d.path)
              statusBar.log(s"Removed folder: ${d.name}")
        )
      case f: SwarFileItem =>
        new ContextMenu(
          new MenuItem("Open"):
            onAction = _ => tabManager.openFile(f.path)
          ,
          new MenuItem("Rename"):
            onAction = _ => renameFile(f.path)
          ,
          new MenuItem("Move to..."):
            onAction = _ => moveFile(f.path)
          ,
          new MenuItem("Delete"):
            onAction = _ => deleteFile(f.path)
        )
      case f: HtmlFileItem =>
        new ContextMenu(
          new MenuItem("Rename"):
            onAction = _ => renameFile(f.path)
          ,
          new MenuItem("Move to..."):
            onAction = _ => moveFile(f.path)
          ,
          new MenuItem("Delete"):
            onAction = _ => deleteFile(f.path)
        )

  private def createNewFile(parentDir: Path): Unit =
    val dialog = new TextInputDialog(""):
      initOwner(panel.scene.value.getWindow)
      title = "New Composition File"
      headerText = "Enter filename (without .swar extension)"
    dialog.showAndWait() match
      case Some(name) if name.trim.nonEmpty =>
        val fileName = if name.endsWith(".swar") then name else s"$name.swar"
        val filePath = parentDir.resolve(fileName)
        if Files.exists(filePath) then statusBar.log(s"File already exists: $fileName")
        else
          import com.varpas.sangeet.core.editor.CompositionEditor
          import com.varpas.sangeet.core.model.{CompositionType, Laya}
          import com.varpas.sangeet.core.raag.Raags
          import com.varpas.sangeet.core.taal.Taals
          import com.varpas.sangeet.core.format.SwarFormat
          val editor = CompositionEditor.create(
            title = name.trim.replaceAll("\\.swar$", ""),
            compositionType = CompositionType.Gat,
            taal = Taals.teentaal,
            raag = Raags.yaman,
            laya = Some(Laya.Madhya),
            showStrokeLine = true
          )
          SwarFormat.writeFile(filePath, editor.composition)
          refreshAll()
          tabManager.openFile(filePath)
          statusBar.log(s"Created: $fileName")
      case _ => ()

  private def createNewFolder(parentDir: Path): Unit =
    val dialog = new TextInputDialog(""):
      initOwner(panel.scene.value.getWindow)
      title = "New Folder"
      headerText = "Enter folder name"
    dialog.showAndWait() match
      case Some(name) if name.trim.nonEmpty =>
        val folderPath = parentDir.resolve(name.trim)
        if Files.exists(folderPath) then statusBar.log(s"Folder already exists: ${name.trim}")
        else
          Files.createDirectories(folderPath)
          refreshAll()
          statusBar.log(s"Created folder: ${name.trim}")
      case _ => ()

  private def renameFile(path: Path): Unit =
    val currentName = path.getFileName.toString
    val dialog = new TextInputDialog(currentName):
      initOwner(panel.scene.value.getWindow)
      title = "Rename"
      headerText = "Enter new name"
    dialog.showAndWait() match
      case Some(newName) if newName.trim.nonEmpty && newName.trim != currentName =>
        val newPath = path.getParent.resolve(newName.trim)
        if Files.exists(newPath) then statusBar.log(s"A file with that name already exists")
        else
          Files.move(path, newPath)
          tabManager.allTabs.foreach { et =>
            if et.filePath.contains(path) then
              et.filePath = Some(newPath)
              et.editorPane.setFilePath(newPath)
          }
          refreshAll()
          statusBar.log(s"Renamed: $currentName -> ${newName.trim}")
      case _ => ()

  private def moveFile(path: Path): Unit =
    val dc = new scalafx.stage.DirectoryChooser:
      title = "Move to..."
      initialDirectory = new java.io.File(path.getParent.toString)
    val ownerWindow = panel.scene.value.getWindow
    val destDir     = dc.showDialog(ownerWindow)
    if destDir != null then
      val dest = destDir.toPath.resolve(path.getFileName)
      if Files.exists(dest) then statusBar.log(s"A file named ${path.getFileName} already exists in the destination")
      else
        Files.move(path, dest)
        tabManager.allTabs.foreach { et =>
          if et.filePath.contains(path) then
            et.filePath = Some(dest)
            et.editorPane.setFilePath(dest)
        }
        refreshAll()
        statusBar.log(s"Moved: ${path.getFileName} -> ${destDir.getName}")

  private def deleteFile(path: Path): Unit =
    val alert = new Alert(Alert.AlertType.Confirmation):
      initOwner(panel.scene.value.getWindow)
      title = "Delete File"
      headerText = s"Delete ${path.getFileName}?"
      contentText = "This action cannot be undone."
    alert.showAndWait() match
      case Some(result) if result == ButtonType.OK =>
        Files.deleteIfExists(path)
        tabManager.allTabs.filter(_.filePath.contains(path)).foreach(tabManager.closeTab)
        refreshAll()
        statusBar.log(s"Deleted: ${path.getFileName}")
      case _ => ()

sealed trait FileTreeItem:
  def path: Path
  def name: String
  def isBookmarked: Boolean

case class DirectoryItem(path: Path, name: String, isBookmarked: Boolean) extends FileTreeItem
case class SwarFileItem(path: Path, name: String, isBookmarked: Boolean)  extends FileTreeItem
case class HtmlFileItem(path: Path, name: String, isBookmarked: Boolean)  extends FileTreeItem
