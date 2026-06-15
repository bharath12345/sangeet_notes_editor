# Studio: File Browser, Tabs, and Google Drive

## Context

Transform the single-file editor into a studio workspace. The user stores `.swar` and `.html` files organized by raag in directories — one directory per raag. The studio adds a left panel for browsing/managing these directories, tabbed editing for multiple open files, and persistent bookmarks so the workspace restores on restart.

### Platforms
- **Desktop**: Browse local filesystem directories
- **Web**: Browse Google Drive directories (no localhost filesystem access)
- Both built in parallel within each phase

### Design Decisions (confirmed with user)

| Decision | Choice |
|----------|--------|
| Config file name | `sangeet-notes-editor.json` (matches app name) |
| Config location | OS-standard app data dir (macOS: `~/Library/Application Support/sangeet-notes-editor/`, Linux: `~/.config/sangeet-notes-editor/`, Windows: `%APPDATA%/sangeet-notes-editor/`) |
| Tab behavior | `.swar` → editable, `.html` → read-only preview |
| Unsaved changes | Auto-save silently when switching tabs |
| File management | Full CRUD: create, rename, delete, move files & directories |
| Web filesystem | Google Drive only (no localStorage files). User must connect Drive to save `.swar` files. Can still create new compositions and export HTML without Drive. |
| Drive storage | Native files in Drive (plain `.swar` and `.html`, visible in Drive UI) |
| Panel collapse | Thin icon strip (VS Code-style) — single icon to expand |
| New file flow | Keep current "New Composition" flow (in-memory, Save As picks location) |
| Open button | Keep existing single-file "Open" button. Add "Open Directory" next to it. |
| File filter | Left panel shows only `.swar` and `.html` files. Other file types hidden. |
| Bookmarking | Star icon in left panel. Bookmarked dirs/files appear on app restart. |

---

## Current State

### Desktop (`MainApp.scala`)
- `BorderPane`: toolbar (top), `horizontalSplit` (center)
- `horizontalSplit` = `SplitPane` with `verticalSplit` (editor+status) and `keyboardLegend`
- `verticalSplit` = `SplitPane` with `editorPane` and `statusBar`
- `EditorPane` extends `VBox`, holds single composition via `currentFilePath: Option[Path]`
- `EditorPane` has `autoSave()` (500ms debounced), `setComposition()`, `setReadOnly()`
- No config persistence mechanism exists

### Web (`sangeet-web`)
- Elm `Model` has no file path tracking — compositions are in-memory, saved via download
- Ports: `selectFile`, `fileSelected`, `fileLoaded`, `downloadFile` for file I/O
- No Google OAuth, Drive API, or cloud storage code exists
- Toolbar: two rows — top row (file/edit/view buttons), bottom row (section tabs)
- Layout: single composition view, no panels or tabs

### Server (`sangeet-server`)
- Stateless REST API — client owns all state
- No file/directory endpoints exist
- Server does not touch filesystem (only computation)

---

## Phase 1: Config Persistence Layer

Persist bookmarks and workspace state across app restarts. Desktop writes JSON to OS app data dir. Web stores in `localStorage` (Drive connection state) and IndexedDB (for potential offline cache).

### Desktop — Config Module

**New file: `sangeet-core/src/main/scala/com/varpas/sangeet/core/config/AppConfig.scala`**

```scala
case class BookmarkEntry(
    path: String,         // absolute path to dir or file
    isDirectory: Boolean,
    label: String         // display name
)

case class OpenTab(
    filePath: String,
    sectionIndex: Int     // remember which section user was viewing
)

case class AppConfig(
    bookmarks: List[BookmarkEntry] = Nil,
    openTabs: List[OpenTab] = Nil,
    activeTabPath: Option[String] = None,
    leftPanelWidth: Double = 250.0,
    leftPanelCollapsed: Boolean = false
)
```

**New file: `sangeet-core/src/main/scala/com/varpas/sangeet/core/config/ConfigStore.scala`**

```scala
object ConfigStore:
    def configDir: Path       // OS-specific app data dir
    def configFile: Path      // configDir / "sangeet-notes-editor.json"
    def load(): AppConfig     // read + parse, return default if missing/corrupt
    def save(config: AppConfig): Unit  // write atomically (write tmp, rename)
```

Circe codecs for `AppConfig`. Place in `sangeet-core` so both desktop and future platforms can use it.

### Web — localStorage persistence

**Elm model additions:**
```elm
type alias WebConfig =
    { bookmarks : List BookmarkEntry
    , openTabs : List OpenTabEntry
    , driveConnected : Bool
    }
```

**New ports:**
```elm
port saveConfig : Json.Encode.Value -> Cmd msg
port loadedConfig : (Json.Decode.Value -> msg) -> Sub msg
port requestConfig : () -> Cmd msg
```

**ports.js:** Read/write `localStorage` key `"sangeet-notes-editor-config"`.

### Verification
- Desktop: `sbt sangeet-core/compile`, unit test for load/save roundtrip, missing file returns default
- Web: `elm make`, port wiring test

---

## Phase 2: Tabbed Editor (Desktop)

Replace the single `EditorPane` with a `TabPane` that can hold multiple compositions.

### Desktop Changes

**New file: `sangeet-desktop/.../editor/EditorTab.scala`**

Wraps an `EditorPane` with file metadata:
```scala
class EditorTab(
    val filePath: Option[Path],
    val editorPane: EditorPane,
    val isReadOnly: Boolean
):
    def title: String     // filename or "Untitled"
    def isDirty: Boolean  // has unsaved changes
    def autoSave(): Unit  // delegate to editorPane.autoSave()
```

**New file: `sangeet-desktop/.../editor/TabManager.scala`**

Manages the `TabPane`:
```scala
class TabManager:
    val tabPane: TabPane
    def openFile(path: Path): Unit        // create tab or focus existing
    def openHtml(path: Path): Unit        // read-only WebView tab
    def newTab(): EditorTab               // untitled composition
    def closeTab(tab: Tab): Unit          // auto-save, remove
    def activeTab: Option[EditorTab]
    def switchTo(path: Path): Unit        // focus existing tab
    def autoSaveActive(): Unit            // save current before switch
    def restoreTabs(config: AppConfig): Unit  // on startup
    def getOpenTabs: List[OpenTab]        // for config persistence
```

Tab switching calls `autoSaveActive()` on the outgoing tab before switching.

### MainApp.scala Changes

Replace `editorPane` with `tabManager`:
```
horizontalSplit
├── leftPanel (Phase 3)       ← NEW
├── verticalSplit
│   ├── tabManager.tabPane    ← CHANGED (was editorPane)
│   └── statusBar
└── keyboardLegend
```

Toolbar buttons (Save, Cut, Copy, Paste, Undo, Redo, Properties, etc.) delegate to `tabManager.activeTab.editorPane`.

**Key binding changes:**
- `Ctrl+W` → close active tab
- `Ctrl+Tab` → next tab
- `Ctrl+Shift+Tab` → previous tab

### Verification
- Open 3 files → tabs appear with filenames
- Switch tabs → auto-save fires, correct composition displays
- Close tab → tab removed, adjacent tab activates
- New composition → "Untitled" tab
- `sbt sangeet-desktop/compile`

---

## Phase 3: Tabbed Editor (Web)

### Elm Model Changes

**`State/Model.elm`** — Add tab state:
```elm
type alias FileTab =
    { id : String            -- unique ID (filename or generated)
    , filename : String
    , filePath : Maybe String -- Drive file ID for web, local path concept for future
    , isReadOnly : Bool
    , history : UndoHistory
    , currentSectionIndex : Int
    , editMode : EditMode
    , ornamentMode : OrnamentMode
    , groupingState : Maybe GroupingState
    , layoutGrids : List SectionGrid
    }

type alias Model =
    { ...existing fields...
    , tabs : List FileTab
    , activeTabId : Maybe String
    }
```

Each tab has its own `UndoHistory`, `sectionIndex`, `editMode`, etc. The shared state (colors, scripts, taals, raags, keyboard legend, dialogs) stays at `Model` level.

### State/Update.elm — Tab Management

New `Msg` variants:
- `SwitchTab String` — switch to tab by ID, auto-save outgoing
- `CloseTab String` — close tab, switch to adjacent
- `NewTab` — create untitled tab
- `TabFileLoaded String String` — file content loaded into specific tab

### View Changes

**`View/Toolbar.elm`** — Add tab bar below the section tabs row:
```
Toolbar Row 1: File / Edit / Mode / View / Properties / Script
Toolbar Row 2: Section tabs (per composition)
Tab Bar:       [File1.swar] [File2.swar] [Untitled] [+]
```

Or integrate tabs as a third toolbar row. Each tab shows filename, close button (×), dirty indicator (●).

### Verification
- `elm make src/Main.elm` + `elm-test`
- Open multiple files → tabs appear
- Switch tabs → correct composition displays
- Close tab → adjacent activates

---

## Phase 4: Left Panel — Directory Browser (Desktop)

### Desktop — File Browser Panel

**New file: `sangeet-desktop/.../editor/FileBrowserPanel.scala`**

```scala
class FileBrowserPanel(tabManager: TabManager, statusBar: StatusBar):
    val panel: VBox           // the entire left panel
    val treeView: TreeView[FileTreeItem]

    def addDirectory(dir: Path): Unit       // add root dir to tree
    def removeDirectory(dir: Path): Unit
    def refreshDirectory(dir: Path): Unit
    def toggleBookmark(item: FileTreeItem): Unit
    def collapse(): Unit     // shrink to icon strip
    def expand(): Unit

    // File management context menu
    def createFile(parent: Path): Unit      // dialog → .swar
    def createDirectory(parent: Path): Unit
    def renameItem(item: Path): Unit
    def deleteItem(item: Path): Unit        // confirmation dialog
    def moveItem(item: Path, dest: Path): Unit
```

**`FileTreeItem`:**
```scala
sealed trait FileTreeItem:
    def path: Path
    def name: String
    def isBookmarked: Boolean

case class DirectoryItem(path: Path, name: String, isBookmarked: Boolean) extends FileTreeItem
case class FileItem(path: Path, name: String, isBookmarked: Boolean, fileType: FileType) extends FileTreeItem

enum FileType:
    case Swar, Html
```

**Tree population:** `Files.list(dir)` filtered to `.swar`/`.html` files and subdirectories. Lazy-load subdirectories on expand.

**Interactions:**
- Double-click `.swar` file → `tabManager.openFile(path)`
- Double-click `.html` file → `tabManager.openHtml(path)` (read-only)
- Star icon toggle → bookmark/unbookmark, persist to config
- Right-click → context menu (New File, New Folder, Rename, Delete, Move)

### MainApp.scala Layout

```
BorderPane
├── top: ToolBar (add "Open Directory" button)
└── center: mainSplit (SplitPane, horizontal)
    ├── left: fileBrowserPanel (collapsible)
    └── right: horizontalSplit (existing)
        ├── verticalSplit
        │   ├── tabManager.tabPane
        │   └── statusBar
        └── keyboardLegend
```

**Toolbar addition:** "Open Directory" button after "Open" button. Opens `DirectoryChooser`, adds selected dir to left panel.

### Collapse Behavior (Desktop)

- Collapsed state: panel shrinks to ~40px wide, shows a single folder icon (📁)
- Click the icon → expand back to `leftPanelWidth`
- Persist collapsed state and width in `AppConfig`
- `SplitPane` divider position controlled programmatically

### Verification
- Open directory → tree appears with .swar and .html files
- Double-click file → opens in tab
- Bookmark dir → restart app → dir appears
- Right-click → create/rename/delete works
- Collapse → icon strip → expand restores

---

## Phase 5: Left Panel — Directory Browser (Web + Google Drive)

### Google Drive Integration

**New Elm module: `Api/GoogleDrive.elm`**

Handles Drive API calls via ports (JS does the actual gapi calls):

```elm
-- Outgoing ports (Elm → JS)
port googleDriveAuth : () -> Cmd msg         -- initiate OAuth
port googleDriveListDir : String -> Cmd msg  -- list folder contents (folder ID)
port googleDriveReadFile : String -> Cmd msg  -- read file content (file ID)
port googleDriveWriteFile : { fileId : String, content : String, mimeType : String } -> Cmd msg
port googleDriveCreateFile : { name : String, parentId : String, content : String, mimeType : String } -> Cmd msg
port googleDriveCreateFolder : { name : String, parentId : String } -> Cmd msg
port googleDriveRenameItem : { fileId : String, newName : String } -> Cmd msg
port googleDriveDeleteItem : String -> Cmd msg
port googleDriveMoveItem : { fileId : String, newParentId : String } -> Cmd msg

-- Incoming ports (JS → Elm)
port googleDriveAuthResult : (Json.Decode.Value -> msg) -> Sub msg
port googleDriveDirListing : (Json.Decode.Value -> msg) -> Sub msg
port googleDriveFileContent : (Json.Decode.Value -> msg) -> Sub msg
port googleDriveWriteResult : (Json.Decode.Value -> msg) -> Sub msg
port googleDriveError : (String -> msg) -> Sub msg
```

**ports.js — Google Drive section:**

Uses Google Identity Services (GIS) for OAuth 2.0 and Google Drive API v3:
- Load `https://apis.google.com/js/api.js` and `https://accounts.google.com/gsi/client`
- OAuth scopes: `https://www.googleapis.com/auth/drive.file` (only files created/opened by app)
- Token stored in `sessionStorage` (not persisted — user re-authorizes each session for security)
- All Drive API calls go through `gapi.client.drive`

**Note:** Requires a Google Cloud project with Drive API enabled and OAuth client ID configured. The client ID will be set via environment variable / build config, not hardcoded.

### Web — File Browser Component

**New Elm module: `View/FileBrowser.elm`**

```elm
type alias DriveItem =
    { id : String          -- Drive file ID
    , name : String
    , mimeType : String    -- "application/vnd.google-apps.folder" or file type
    , isBookmarked : Bool
    }

type alias FolderState =
    { folderId : String
    , name : String
    , items : List DriveItem
    , expanded : Bool
    , isBookmarked : Bool
    }
```

Renders as a collapsible tree in a `div` on the left side of the app. CSS flexbox layout:
```
.app-container (flex row)
├── .file-browser-panel (flex column, collapsible)
│   ├── .panel-header ("Files" + collapse button)
│   ├── .drive-connect-btn (if not connected)
│   └── .folder-tree
└── .editor-area (flex column, flex-grow: 1)
    ├── .toolbar
    ├── .tab-bar
    └── .editor-content
```

**Collapsed state:** Panel shows only a 40px column with a folder icon. Click to expand.

### Web — Save Flow Change

Current: "Save" always downloads a file via browser download.

New flow:
- If Drive connected AND file has a Drive ID → save to Drive (overwrite)
- If Drive connected AND file is new → prompt: save to Drive (pick folder) or download
- If Drive NOT connected → download as before (existing behavior preserved)

"Save As" always shows the choice dialog.

### index.html Changes

Add Google API script tags (loaded conditionally, not blocking):
```html
<script src="https://apis.google.com/js/api.js" async defer></script>
<script src="https://accounts.google.com/gsi/client" async defer></script>
```

Add Google OAuth client ID via meta tag or environment variable injection at build time.

### Verification
- Web: Connect Drive → folders appear in left panel
- Open .swar from Drive → editable tab
- Edit → auto-save writes back to Drive
- Create new file in Drive folder → appears in tree
- Rename/delete from context menu → reflected in Drive
- Without Drive → existing download-based flow works unchanged
- `elm make`, `elm-test`

---

## Phase 6: File Management Operations

### Desktop — Context Menu Actions

All operations on the left panel tree via right-click context menu:

| Action | Behavior |
|--------|----------|
| New .swar File | Dialog for filename → creates empty composition JSON → opens in tab |
| New Folder | Dialog for name → `Files.createDirectory()` |
| Rename | Inline edit or dialog → `Files.move()` (same dir, new name). Update open tabs if renamed file is open. |
| Delete | Confirmation dialog → `Files.delete()` (or `Files.walkFileTree` for dirs). Close tab if file is open. |
| Move | Drag-and-drop within tree, or "Move to..." dialog → `Files.move()` |
| Refresh | Re-scan directory contents |

### Web — Context Menu Actions

Same operations but via Google Drive API calls through ports. Each action:
1. Elm sends port command
2. JS executes Drive API call
3. JS sends result back via incoming port
4. Elm updates folder state and refreshes tree

### Verification
- Create file → appears in tree and Drive
- Rename → tree updates, open tab title updates
- Delete → confirmation → removed from tree, tab closed
- Move → item moves in tree

---

## Phase 7: App Startup & State Restoration

### Desktop Startup Sequence

1. Load `AppConfig` from disk (or defaults)
2. Create `FileBrowserPanel` with bookmarked directories
3. Create `TabManager`
4. Restore open tabs from config (load each file, create tab)
5. Focus the `activeTabPath` tab
6. If no tabs and no config → load sample composition as before (read-only)
7. Restore panel width and collapsed state

### Web Startup Sequence

1. Load config from `localStorage`
2. If Drive was previously connected → attempt silent re-auth (token may be expired)
3. If re-auth succeeds → load bookmarked Drive folders
4. Restore tabs (Drive files need to be re-fetched)
5. If no tabs → show empty editor as before

### Auto-save on Tab Switch

Both platforms:
- When user clicks a different tab → auto-save current tab
- Desktop: `EditorPane.autoSave()` (already exists, writes to file)
- Web + Drive: port call to `googleDriveWriteFile`
- Web without Drive: no auto-save (user must download manually — can't write to browser FS)

### Config Persistence Triggers

Save config on:
- Bookmark add/remove
- Tab open/close
- Panel resize/collapse
- App close (desktop `onCloseRequest`)
- Periodic (every 30s as safety net)

### Verification
- Desktop: bookmark dirs → close app → reopen → dirs appear, tabs restored
- Web: connect Drive → bookmark folders → refresh page → folders reappear, tabs restored (if re-auth succeeds)
- Fresh install → sample composition loads as before

---

## Phase 8: Polish & Edge Cases

### HTML Preview Tab (Desktop)
- `.html` files open in a JavaFX `WebView` inside a tab
- **build.sbt change required**: remove `javafx-web` from the excluded modules list (currently excluded at line ~75). WebView is part of `javafx.web`.
- Read-only — no editing controls, toolbar buttons disabled
- Tab title shows filename with "(preview)" suffix

### HTML Preview Tab (Web)
- `.html` files rendered in a sandboxed `iframe` or displayed via `Html.innerHTML`
- Read-only indicator in tab

### Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+W` | Close active tab |
| `Ctrl+Tab` | Next tab |
| `Ctrl+Shift+Tab` | Previous tab |
| `Ctrl+O` | Open file (existing) |
| `Ctrl+Shift+O` | Open directory |
| `Ctrl+B` | Toggle left panel |

### Edge Cases
- File deleted externally while tab is open → detect on save, show "file was deleted" dialog
- File modified externally → detect on focus (compare mtime), offer reload
- Very large directories (100+ files) → lazy loading, don't block UI
- Drive API rate limits → exponential backoff, show status message
- Drive token expiry mid-session → re-auth prompt, queue pending operations
- Tab with unsaved "Untitled" composition → close asks "Save?" (not auto-save, since no path)
- Multiple instances (desktop) → file locking or last-write-wins with warning

### Verification
- All keyboard shortcuts work
- External file changes detected
- Large directories don't freeze UI
- Drive token expiry handled gracefully

---

## Phase 9: Tests

### Scala Core
- `AppConfigSpec` — serialization roundtrip, default values, missing fields
- `ConfigStoreSpec` — load from file, save atomically, handle corrupt file

### Desktop (manual verification)
- Tab switching preserves composition state
- Bookmark persistence across restart
- File management operations (create/rename/delete/move)
- Panel collapse/expand
- HTML preview in WebView

### Elm
- `FileBrowserTest` — folder tree rendering, bookmark toggle, file click
- `TabManagerTest` — tab switching, close, auto-save trigger
- `ConfigPersistenceTest` — encode/decode config, localStorage roundtrip
- `GoogleDriveTest` — port message encoding, error handling

### E2E (Playwright)
- `file-browser.spec.ts` — panel visibility, collapse, expand
- `tabs.spec.ts` — open multiple files, switch, close
- `google-drive.spec.ts` — mock Drive API responses, connect flow
- Update `app-page.ts` — add panel/tab locators

### Server
- No server changes needed — all file operations are client-side

---

## Implementation Order

```
Phase 1: Config Persistence
    ↓
Phase 2: Tabbed Editor (Desktop)     Phase 3: Tabbed Editor (Web)
    ↓                                     ↓
Phase 4: Left Panel (Desktop)        Phase 5: Left Panel (Web + Drive)
    ↓                                     ↓
Phase 6: File Management (both platforms)
    ↓
Phase 7: Startup & State Restoration
    ↓
Phase 8: Polish & Edge Cases
    ↓
Phase 9: Tests
```

Phases 2/3 and 4/5 can be done in parallel (desktop and web tracks). Phase 1 must come first. Phase 6+ depends on panels and tabs being in place.

## Files Changed Summary

### New Files (~15)
- `sangeet-core/.../config/AppConfig.scala`
- `sangeet-core/.../config/ConfigStore.scala`
- `sangeet-core/.../config/AppConfigSpec.scala` (test)
- `sangeet-desktop/.../editor/EditorTab.scala`
- `sangeet-desktop/.../editor/TabManager.scala`
- `sangeet-desktop/.../editor/FileBrowserPanel.scala`
- `sangeet-desktop/.../editor/FileTreeItem.scala`
- `sangeet-web/src/Api/GoogleDrive.elm`
- `sangeet-web/src/View/FileBrowser.elm`
- `sangeet-web/src/Model/FileTab.elm`
- `sangeet-web/tests/FileBrowserTest.elm`
- `sangeet-web/tests/TabManagerTest.elm`
- `e2e/tests/file-browser.spec.ts`
- `e2e/tests/tabs.spec.ts`

### Modified Files (~15)
- `sangeet-desktop/.../MainApp.scala` — layout restructure, toolbar additions
- `sangeet-desktop/.../editor/EditorPane.scala` — minor interface adjustments for TabManager
- `sangeet-web/src/State/Model.elm` — add tabs, file browser, Drive state
- `sangeet-web/src/State/Msg.elm` — tab/browser/Drive message variants
- `sangeet-web/src/State/Update.elm` — handle new messages
- `sangeet-web/src/View/Toolbar.elm` — tab bar row
- `sangeet-web/src/View/Canvas.elm` — layout adjustment for panel
- `sangeet-web/src/Ports.elm` — Drive + config ports
- `sangeet-web/public/ports.js` — Drive API integration, config persistence
- `sangeet-web/public/index.html` — Google API scripts, layout CSS
- `sangeet-web/public/styles.css` — panel, tab bar, collapse styles
- `e2e/helpers/app-page.ts` — panel/tab locators
- `build.sbt` — no new dependencies expected (JavaFX WebView is built-in)

## Final Verification

1. `sbt compile` — all modules compile
2. `sbt test` — all Scala tests pass
3. `elm make src/Main.elm` + `elm-test` — Elm compiles and passes
4. Desktop: open dirs → browse files → open in tabs → switch tabs → auto-save → bookmark → restart → restored
5. Web: connect Drive → browse Drive folders → open files → edit → save to Drive → bookmark → refresh → restored
6. Web without Drive: existing new/open/save-download flow works unchanged
7. `make lint` + `make check-all` — CI green
