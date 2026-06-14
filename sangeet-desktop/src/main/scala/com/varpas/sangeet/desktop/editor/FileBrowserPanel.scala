package com.varpas.sangeet.desktop.editor

import java.nio.file.{Files, Path}

import scala.collection.mutable.ListBuffer

import javafx.scene.control.{TreeItem => JTreeItem}
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control._
import scalafx.scene.input.InputIncludes.jfxMouseEvent2sfx
import scalafx.scene.layout.{HBox, Priority, Region, VBox}

import com.varpas.sangeet.core.config.BookmarkEntry
import com.varpas.sangeet.core.strings.UiStrings
import com.varpas.sangeet.desktop.Icons

class FileBrowserPanel(tabManager: TabManager, statusBar: StatusBar):

  private val rootDirs        = ListBuffer.empty[Path]
  private val bookmarkedPaths = ListBuffer.empty[Path]

  private val treeRoot =
    new TreeItem[FileTreeItem](DirectoryItem(Path.of("/"), UiStrings.fileBrowserPanelTitle, isBookmarked = false)):
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
            val iconCode = newItem match
              case _: DirectoryItem => "mdi2f-folder-outline"
              case _: SwarFileItem  => "mdi2m-music-note-eighth"
              case _: HtmlFileItem  => "mdi2l-language-html5"
            val star = if newItem.isBookmarked then " ★" else ""
            text = newItem.name + star
            graphic = Icons.make(iconCode, 14)
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

  private val headerLabel = new Label(UiStrings.fileBrowserHeaderLabel):
    style = "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 8 4 8;"

  private val addFolderBtn = new Button():
    style = "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2 6 2 6;"
    graphic = Icons.make("mdi2f-folder-plus-outline", 16)
    tooltip = new Tooltip(UiStrings.fileBrowserAddFolderTooltip)
    onAction = _ =>
      val dc = new scalafx.stage.DirectoryChooser:
        title = UiStrings.fileBrowserAddFolderDialogTitle
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
      statusBar.log(UiStrings.fileBrowserErrorNotDirectory.replace("{path}", dir.toString))
      return
    if rootDirs.contains(dir) then
      statusBar.log(UiStrings.fileBrowserErrorFolderOpen.replace("{name}", dir.getFileName.toString))
      return
    rootDirs += dir
    val dirItem = buildTreeItem(dir, isRoot = true)
    treeRoot.children.add(dirItem)
    AppLogger.info(s"Added directory to browser: $dir")
    statusBar.log(UiStrings.fileBrowserLogAddedFolder.replace("{name}", dir.getFileName.toString))

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
    val wasBookmarked = bookmarkedPaths.contains(path)
    if wasBookmarked then bookmarkedPaths -= path
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
          new MenuItem(UiStrings.fileBrowserMenuNewFile):
            onAction = _ => createNewFile(d.path)
          ,
          new MenuItem(UiStrings.fileBrowserMenuNewFolder):
            onAction = _ => createNewFolder(d.path)
          ,
          new SeparatorMenuItem(),
          new MenuItem(
            if d.isBookmarked then UiStrings.fileBrowserMenuRemoveBookmark else UiStrings.fileBrowserMenuBookmark
          ):
            onAction = _ =>
              val wasBookmarked = bookmarkedPaths.contains(d.path)
              toggleBookmark(d.path)
              statusBar.log(
                if !wasBookmarked then UiStrings.fileBrowserLogBookmarked.replace("{name}", d.name)
                else UiStrings.fileBrowserLogRemovedBookmark.replace("{name}", d.name)
              )
          ,
          new MenuItem(UiStrings.fileBrowserMenuRefresh):
            onAction = _ => refreshAll()
          ,
          new SeparatorMenuItem(),
          new MenuItem(UiStrings.fileBrowserMenuRemoveFromBrowser):
            onAction = _ =>
              removeDirectory(d.path)
              statusBar.log(UiStrings.fileBrowserLogRemovedFolder.replace("{name}", d.name))
        )
      case f: SwarFileItem =>
        new ContextMenu(
          new MenuItem(UiStrings.fileBrowserMenuOpen):
            onAction = _ => tabManager.openFile(f.path)
          ,
          new MenuItem(UiStrings.fileBrowserMenuRename):
            onAction = _ => renameFile(f.path)
          ,
          new MenuItem(UiStrings.fileBrowserMenuMoveTo):
            onAction = _ => moveFile(f.path)
          ,
          new MenuItem(UiStrings.fileBrowserMenuDelete):
            onAction = _ => deleteFile(f.path)
        )
      case f: HtmlFileItem =>
        new ContextMenu(
          new MenuItem(UiStrings.fileBrowserMenuRename):
            onAction = _ => renameFile(f.path)
          ,
          new MenuItem(UiStrings.fileBrowserMenuMoveTo):
            onAction = _ => moveFile(f.path)
          ,
          new MenuItem(UiStrings.fileBrowserMenuDelete):
            onAction = _ => deleteFile(f.path)
        )

  private def createNewFile(parentDir: Path): Unit =
    val dialog = new TextInputDialog(""):
      initOwner(panel.scene.value.getWindow)
      title = UiStrings.fileBrowserNewFileDialogTitle
      headerText = UiStrings.fileBrowserNewFileDialogPrompt
    dialog.showAndWait() match
      case Some(name) if name.trim.nonEmpty =>
        val fileName = if name.endsWith(".swar") then name else s"$name.swar"
        val filePath = parentDir.resolve(fileName)
        if Files.exists(filePath) then statusBar.log(UiStrings.fileBrowserErrorFileExists.replace("{name}", fileName))
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
          statusBar.log(UiStrings.fileBrowserLogCreatedFile.replace("{name}", fileName))
      case _ => ()

  private def createNewFolder(parentDir: Path): Unit =
    val dialog = new TextInputDialog(""):
      initOwner(panel.scene.value.getWindow)
      title = UiStrings.fileBrowserNewFolderDialogTitle
      headerText = UiStrings.fileBrowserNewFolderDialogPrompt
    dialog.showAndWait() match
      case Some(name) if name.trim.nonEmpty =>
        val folderPath = parentDir.resolve(name.trim)
        if Files.exists(folderPath) then
          statusBar.log(UiStrings.fileBrowserErrorFolderExists.replace("{name}", name.trim))
        else
          Files.createDirectories(folderPath)
          refreshAll()
          statusBar.log(UiStrings.fileBrowserLogCreatedFolder.replace("{name}", name.trim))
      case _ => ()

  private def renameFile(path: Path): Unit =
    val currentName = path.getFileName.toString
    val dialog = new TextInputDialog(currentName):
      initOwner(panel.scene.value.getWindow)
      title = UiStrings.fileBrowserRenameDialogTitle
      headerText = UiStrings.fileBrowserRenameDialogPrompt
    dialog.showAndWait() match
      case Some(newName) if newName.trim.nonEmpty && newName.trim != currentName =>
        val newPath = path.getParent.resolve(newName.trim)
        if Files.exists(newPath) then statusBar.log(UiStrings.fileBrowserErrorRenameExists)
        else
          Files.move(path, newPath)
          tabManager.allTabs.foreach { et =>
            if et.filePath.contains(path) then
              et.filePath = Some(newPath)
              et.editorPane.setFilePath(newPath)
          }
          refreshAll()
          statusBar.log(UiStrings.fileBrowserLogRenamed.replace("{old}", currentName).replace("{new}", newName.trim))
      case _ => ()

  private def moveFile(path: Path): Unit =
    val dc = new scalafx.stage.DirectoryChooser:
      title = UiStrings.fileBrowserMoveToDialogTitle
      initialDirectory = new java.io.File(path.getParent.toString)
    val ownerWindow = panel.scene.value.getWindow
    val destDir     = dc.showDialog(ownerWindow)
    if destDir != null then
      val dest = destDir.toPath.resolve(path.getFileName)
      if Files.exists(dest) then
        statusBar.log(UiStrings.fileBrowserErrorMoveExists.replace("{name}", path.getFileName.toString))
      else
        Files.move(path, dest)
        tabManager.allTabs.foreach { et =>
          if et.filePath.contains(path) then
            et.filePath = Some(dest)
            et.editorPane.setFilePath(dest)
        }
        refreshAll()
        statusBar.log(
          UiStrings.fileBrowserLogMoved.replace("{name}", path.getFileName.toString).replace("{dest}", destDir.getName)
        )

  private def deleteFile(path: Path): Unit =
    val alert = new Alert(Alert.AlertType.Confirmation):
      initOwner(panel.scene.value.getWindow)
      title = UiStrings.fileBrowserDeleteDialogTitle
      headerText = UiStrings.fileBrowserDeleteDialogPrompt.replace("{filename}", path.getFileName.toString)
      contentText = UiStrings.fileBrowserDeleteDialogWarning
    alert.showAndWait() match
      case Some(result) if result == ButtonType.OK =>
        Files.deleteIfExists(path)
        tabManager.allTabs.filter(_.filePath.contains(path)).foreach(tabManager.closeTab)
        refreshAll()
        statusBar.log(UiStrings.fileBrowserLogDeleted.replace("{name}", path.getFileName.toString))
      case _ => ()

sealed trait FileTreeItem:
  def path: Path
  def name: String
  def isBookmarked: Boolean

case class DirectoryItem(path: Path, name: String, isBookmarked: Boolean) extends FileTreeItem
case class SwarFileItem(path: Path, name: String, isBookmarked: Boolean)  extends FileTreeItem
case class HtmlFileItem(path: Path, name: String, isBookmarked: Boolean)  extends FileTreeItem
