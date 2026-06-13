# UI Strings Parity Report

> Generated: 2026-06-13. Regenerate with `make strings-report`.

## Summary

| Bucket                         | Count |
| ------------------------------ | ----- |
| Shared (`platform: both`)    | 54 |
| Desktop-only                   | 269 |
| Web-only                       | 256 |
| **Total**                      | **579** |

**Goal:** Minimize Desktop-only and Web-only buckets toward zero by dispositioning each entry:
- **PORT** — add equivalent UI to the missing side
- **REMOVE** — delete from the side that has it
- **ACCEPT** — keep as justified platform-specific

## Desktop-only entries (review one-by-one)

Grouped by `area.component` prefix for easier review.

### app.windowTitle

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `app.windowTitle` | `Sangeet Notes Editor` | Desktop main window title (web has hardcoded title in index.html) | TODO |

### appAction.addSection

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `appAction.addSection` | `Add section` | Command palette action title (web doesn't expose section management in palette) | TODO |

### appAction.closeActiveTab

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `appAction.closeActiveTab` | `Close active tab` | Command palette action title (web doesn't expose tab close in palette) | TODO |

### appAction.cycleNotationScript

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `appAction.cycleNotationScript` | `Cycle notation script` | Command palette action title (web doesn't expose script cycling in palette) | TODO |

### appAction.group

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `appAction.group.sections` | `Sections` | Command palette group label (web has no section management in palette) | TODO |
| `appAction.group.tabs` | `Tabs` | Command palette group label (web has no tab management in palette) | TODO |

### appAction.nextTab

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `appAction.nextTab` | `Next tab` | Command palette action title (web doesn't have tabs) | TODO |

### appAction.openFolder

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `appAction.openFolder` | `Open folder` | Command palette action title (web uses Drive picker) | TODO |

### appAction.openUserGuide

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `appAction.openUserGuide` | `Open user guide` | Command palette action title (web doesn't have user guide in palette) | TODO |

### appAction.previousTab

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `appAction.previousTab` | `Previous tab` | Command palette action title (web doesn't have tabs) | TODO |

### appAction.removeCurrentSection

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `appAction.removeCurrentSection` | `Remove current section` | Command palette action title (web doesn't expose section management in palette) | TODO |

### appAction.renameCurrentSection

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `appAction.renameCurrentSection` | `Rename current section` | Command palette action title (web doesn't expose section management in palette) | TODO |

### appAction.saveAs

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `appAction.saveAs` | `Save as` | Command palette action title (web auto-saves to Drive) | TODO |

### appAction.toggleFileBrowser

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `appAction.toggleFileBrowser` | `Toggle file browser` | Command palette action title (web doesn't have file browser panel) | TODO |

### appAction.toggleTheme

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `appAction.toggleTheme` | `Toggle light / dark theme` | Command palette action title (web doesn't expose theme toggle in palette) | TODO |

### dialog.about

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `dialog.about.betaNote.desktop` | `Beta release — actively iterating toward v1.0. Expect rough edges; please file bugs via the 🐞 button in the toolbar.` | Beta note in about dialog (desktop) | TODO |
| `dialog.about.description.desktop.line1` | `A notation editor for Hindustani classical music in the Bhatkhande style.` | Description line 1 in about dialog (desktop) | TODO |
| `dialog.about.description.desktop.line2` | `Designed primarily for sitar compositions — Gat, Bandish, and Palta.` | Description line 2 in about dialog (desktop) | TODO |
| `dialog.about.license.desktop` | `Free and open source. Copyright (c) 2026 Bharadwaj.` | License and copyright text (desktop) | TODO |
| `dialog.about.links.userGuide.desktop` | `User guide & documentation` | Link text for user guide (desktop) | TODO |
| `dialog.about.links.webVersion` | `Web version: {url} [1 param]` | Link text for web version (desktop only) | TODO |
| `dialog.about.privacy.desktop` | `Anonymous usage stats (which features get touched, how long sessions are — never the content you type) are sent to PostHog so I can prioritise what to build next. Set the SANGEET_ANALYTICS_DISABLED=1 environment variable to turn this off.` | Privacy text (desktop) | TODO |
| `dialog.about.sampleToggle` | `Show sample composition on startup` | Checkbox label for sample toggle (desktop only) | TODO |
| `dialog.about.tech.desktop` | `Built with Scala 3 + ScalaFX (desktop) and Elm + Tapir (web)` | Tech stack note (desktop) | TODO |

### dialog.bugReport

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `dialog.bugReport.button.sentSuccess` | `Sent ✓` | Send button label after successful submit (desktop only) | TODO |
| `dialog.bugReport.disclosure.desktop` | `We'll include a short replay of recent keystrokes + a screenshot of this window + the active composition (the .swar JSON of the tab you have open) so the bug can be reproduced. Password fields aren't typed in this app at all. Nothing leaves your machine until you click Send.` | Desktop disclosure text - mentions keystrokes, screenshot, composition | TODO |
| `dialog.bugReport.status.screenshotFailed` | `Screenshot failed ({error}) — sending without it. [1 param]` | Status message when screenshot capture fails (desktop only) | TODO |
| `dialog.bugReport.status.sendFailed` | `Send failed: {error} [1 param]` | Status message shown on send failure (desktop only) | TODO |
| `dialog.bugReport.status.sending` | `Sending report...` | Status message shown while submitting (desktop only) | TODO |
| `dialog.bugReport.status.sendThrew` | `Send threw: {message} [1 param]` | Status message shown on exception (desktop only) | TODO |
| `dialog.bugReport.status.sent` | `Sent. Report id: {reportId} [1 param]` | Status message shown on success with report ID (desktop only) | TODO |

### dialog.commandPalette

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `dialog.commandPalette.searchPlaceholder` | `Search actions… (Esc to close)` | Desktop placeholder is shorter (navigation help in web version) | TODO |
| `dialog.commandPalette.title` | `Command Palette` | Window title (desktop only — web has no window chrome) | TODO |

### dialog.crashRecovery

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `dialog.crashRecovery.buttonDiscard` | `Discard` | Discard button text | TODO |
| `dialog.crashRecovery.buttonRetry` | `Retry send` | Retry button text after failure | TODO |
| `dialog.crashRecovery.buttonSend` | `Send report` | Send button text | TODO |
| `dialog.crashRecovery.descriptionLabel` | `Anything you remember doing right before? (optional)` | Label for user description field | TODO |
| `dialog.crashRecovery.descriptionPlaceholder` | `Optional context — what tab was open, what you'd just typed, etc.` | Placeholder for description textarea | TODO |
| `dialog.crashRecovery.emailLabel` | `Email (optional, only if you want a reply)` | Label for email field | TODO |
| `dialog.crashRecovery.explanation` | `The app crashed during your last session. Sending a report (including the stack trace + your recent keystrokes) helps fix the underlying bug. Password fields are not captured. Nothing leaves your machine until you click Send.` | Crash recovery explanation text | TODO |
| `dialog.crashRecovery.stackTraceLabel` | `Stack trace:` | Label for stack trace section | TODO |
| `dialog.crashRecovery.statusSending` | `Sending...` | Button text while sending | TODO |
| `dialog.crashRecovery.statusSendingReport` | `Sending report...` | Status label while sending | TODO |
| `dialog.crashRecovery.title` | `Sangeet didn't shut down cleanly last time` | Crash recovery dialog title label | TODO |
| `dialog.crashRecovery.windowTitle` | `Sangeet — crash recovery` | Crash recovery dialog window title | TODO |

### dialog.keyboardCheatSheet

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `dialog.keyboardCheatSheet.action.addSection` | `Add section` | Action description (desktop only) | TODO |
| `dialog.keyboardCheatSheet.action.closeTab` | `Close tab` | Action description (desktop only) | TODO |
| `dialog.keyboardCheatSheet.action.compositionProperties` | `Composition properties` | Action description (desktop only) | TODO |
| `dialog.keyboardCheatSheet.action.copy` | `Copy` | Action description (desktop only) | TODO |
| `dialog.keyboardCheatSheet.action.cut` | `Cut` | Action description (desktop only) | TODO |
| `dialog.keyboardCheatSheet.action.cycleScript` | `Cycle notation script` | Action description (desktop only) | TODO |
| `dialog.keyboardCheatSheet.action.exportHtml` | `Export HTML` | Action description (desktop only) | TODO |
| `dialog.keyboardCheatSheet.action.newComposition` | `New composition` | Action description (desktop only) | TODO |
| `dialog.keyboardCheatSheet.action.nextTab` | `Next tab` | Action description (desktop only) | TODO |
| `dialog.keyboardCheatSheet.action.openFile` | `Open file` | Action description (desktop only) | TODO |
| `dialog.keyboardCheatSheet.action.openFolder` | `Open folder` | Action description (desktop only) | TODO |
| `dialog.keyboardCheatSheet.action.openUserGuide` | `Open user guide` | Action description (desktop only) | TODO |
| `dialog.keyboardCheatSheet.action.paste` | `Paste` | Action description (desktop only) | TODO |
| `dialog.keyboardCheatSheet.action.previousTab` | `Previous tab` | Action description (desktop only) | TODO |
| `dialog.keyboardCheatSheet.action.redo` | `Redo` | Action description (desktop only) | TODO |
| `dialog.keyboardCheatSheet.action.removeSection` | `Remove current section` | Action description (desktop only) | TODO |
| `dialog.keyboardCheatSheet.action.renameSection` | `Rename current section` | Action description (desktop only) | TODO |
| `dialog.keyboardCheatSheet.action.reportBug` | `Report a bug` | Action description (desktop only) | TODO |
| `dialog.keyboardCheatSheet.action.save` | `Save` | Action description (desktop only) | TODO |
| `dialog.keyboardCheatSheet.action.saveAs` | `Save as` | Action description (desktop only) | TODO |
| `dialog.keyboardCheatSheet.action.showCheatSheet` | `Show this cheat sheet` | Action description (desktop only) | TODO |
| `dialog.keyboardCheatSheet.action.toggleFileBrowser` | `Toggle file browser` | Action description (desktop only) | TODO |
| `dialog.keyboardCheatSheet.action.toggleTheme` | `Toggle theme` | Action description (desktop only) | TODO |
| `dialog.keyboardCheatSheet.action.undo` | `Undo` | Action description (desktop only) | TODO |
| `dialog.keyboardCheatSheet.section.edit.desktop` | `Edit` | Section title (desktop only) | TODO |
| `dialog.keyboardCheatSheet.section.file.desktop` | `File` | Section title (desktop only) | TODO |
| `dialog.keyboardCheatSheet.section.help.desktop` | `Help` | Section title (desktop only) | TODO |
| `dialog.keyboardCheatSheet.section.sections.desktop` | `Sections` | Section title (desktop only) | TODO |
| `dialog.keyboardCheatSheet.section.tabs.desktop` | `Tabs` | Section title (desktop only) | TODO |
| `dialog.keyboardCheatSheet.section.view.desktop` | `View` | Section title (desktop only) | TODO |
| `dialog.keyboardCheatSheet.subtitle.desktop` | `Full reference: Help → User Guide → Keyboard Reference` | Subtitle pointing to full guide (desktop only) | TODO |

### dialog.newComposition

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `dialog.newComposition.field.antaraStartingBeat.labelDesktop` | `Antara Starting Beat:` | Antara starting beat field label (desktop) | TODO |
| `dialog.newComposition.field.arohan.label` | `Arohan:` | Arohan field label (desktop only) | TODO |
| `dialog.newComposition.field.arohan.placeholder` | `auto-detected or enter manually` | Arohan field placeholder (desktop only) | TODO |
| `dialog.newComposition.field.avrohan.label` | `Avrohan:` | Avrohan field label (desktop only) | TODO |
| `dialog.newComposition.field.avrohan.placeholder` | `auto-detected or enter manually` | Avrohan field placeholder (desktop only) | TODO |
| `dialog.newComposition.field.filePath.browseButton` | `Browse...` | Browse button for file path selection (desktop only) | TODO |
| `dialog.newComposition.field.filePath.browserTitle` | `Save Composition As` | File chooser dialog title (desktop only) | TODO |
| `dialog.newComposition.field.filePath.label` | `Save to:` | File path field label (desktop only) | TODO |
| `dialog.newComposition.field.filePath.placeholder` | `Select location to save .swar file` | File path field placeholder (desktop only) | TODO |
| `dialog.newComposition.field.gatStartingBeat.labelDesktop` | `Gat Starting Beat:` | Gat starting beat field label (desktop) | TODO |
| `dialog.newComposition.field.laya.atidrutDesktop` | `Ati-Drut` | Ati-Drut laya option (desktop) | TODO |
| `dialog.newComposition.field.laya.ativilambitDesktop` | `Ati-Vilambit` | Ati-Vilambit laya option (desktop) | TODO |
| `dialog.newComposition.field.laya.labelDesktop` | `Laya:` | Laya field label (desktop) | TODO |
| `dialog.newComposition.field.laya.noneDesktop` | `(none)` | None laya option (desktop) | TODO |
| `dialog.newComposition.field.raag.labelDesktop` | `Raag:` | Raag field label (desktop) | TODO |
| `dialog.newComposition.field.raag.placeholder` | `Type to search or enter custom raag` | Raag field placeholder (desktop only) | TODO |
| `dialog.newComposition.field.samvadi.label` | `Samvadi:` | Samvadi field label (desktop only) | TODO |
| `dialog.newComposition.field.samvadi.placeholder` | `auto-detected` | Samvadi field placeholder (desktop only) | TODO |
| `dialog.newComposition.field.script.label` | `Script:` | Script field label (desktop only) | TODO |
| `dialog.newComposition.field.showSahitya.checkboxDesktop` | `Show lyrics row below swar` | Show sahitya checkbox label (desktop) | TODO |
| `dialog.newComposition.field.showSahitya.labelDesktop` | `Sahitya line:` | Show sahitya field label (desktop) | TODO |
| `dialog.newComposition.field.showStrokes.checkboxDesktop` | `Show Da/Ra stroke indicators below swar` | Show strokes checkbox label (desktop) | TODO |
| `dialog.newComposition.field.showStrokes.labelDesktop` | `Stroke line:` | Show strokes field label (desktop) | TODO |
| `dialog.newComposition.field.sthayiStartingBeat.labelDesktop` | `Sthayi Starting Beat:` | Sthayi starting beat field label for Bandish (desktop) | TODO |
| `dialog.newComposition.field.taal.labelDesktop` | `Taal:` | Taal field label (desktop) | TODO |
| `dialog.newComposition.field.taanCount.labelDesktop` | `Taans:` | Taan count field label (desktop) | TODO |
| `dialog.newComposition.field.taanStartingBeat.labelDesktop` | `Taan Starting Beat:` | Taan starting beat field label (desktop) | TODO |
| `dialog.newComposition.field.thaat.label` | `Thaat:` | Thaat field label (desktop only) | TODO |
| `dialog.newComposition.field.thaat.placeholder` | `auto-detected or enter manually` | Thaat field placeholder (desktop only) | TODO |
| `dialog.newComposition.field.title.labelDesktop` | `Title:` | Title field label (desktop) | TODO |
| `dialog.newComposition.field.title.placeholderDesktop` | `e.g. Yaman Vilambit Gat` | Title field placeholder (desktop) | TODO |
| `dialog.newComposition.field.type.bandishDesktop` | `Bandish` | Bandish composition type option (desktop) | TODO |
| `dialog.newComposition.field.type.gatDesktop` | `Gat` | Gat composition type option (desktop) | TODO |
| `dialog.newComposition.field.type.labelDesktop` | `Type:` | Type field label (desktop) | TODO |
| `dialog.newComposition.field.type.paltaDesktop` | `Palta` | Palta composition type option (desktop) | TODO |
| `dialog.newComposition.field.type.sargamDesktop` | `Sargam` | Sargam composition type option (desktop) | TODO |
| `dialog.newComposition.field.vadi.label` | `Vadi:` | Vadi field label (desktop only) | TODO |
| `dialog.newComposition.field.vadi.placeholder` | `auto-detected` | Vadi field placeholder (desktop only) | TODO |
| `dialog.newComposition.header` | `Create a new composition` | NewComposition dialog header text (desktop only) | TODO |
| `dialog.newComposition.raagDetected` | `Raag {name} recognized [1 param]` | Raag recognized message (desktop only) | TODO |
| `dialog.newComposition.raagNotFound` | `(raag not in database -- enter details manually)` | Raag not found message (desktop only) | TODO |
| `dialog.newComposition.validation.filePathRequired` | `File path is required` | Validation error: file path required (desktop only) | TODO |
| `dialog.newComposition.validation.layaRequired` | `Laya is required for Gat` | Validation error: laya required for Gat (desktop only) | TODO |
| `dialog.newComposition.validation.raagRequired` | `Raag is required` | Validation error: raag required (desktop only) | TODO |
| `dialog.newComposition.validation.titleRequired` | `Title is required` | Validation error: title required (desktop only) | TODO |

### dialog.properties

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `dialog.properties.field.antaraStartingBeat.labelDesktop` | `Antara Starting Beat:` | Antara starting beat field label (desktop) | TODO |
| `dialog.properties.field.gatStartingBeat.labelDesktop` | `Gat Starting Beat:` | Gat starting beat field label (desktop) | TODO |
| `dialog.properties.field.raag.label` | `Raag:` | Raag field label (desktop only — web doesn't show raag) | TODO |
| `dialog.properties.field.sthayiStartingBeat.labelDesktop` | `Sthayi Starting Beat:` | Sthayi starting beat field label for Bandish (desktop) | TODO |
| `dialog.properties.field.taal.labelDesktop` | `Taal:` | Taal field label (desktop) | TODO |
| `dialog.properties.field.taanStartingBeat.labelDesktop` | `Taan Starting Beat:` | Taan starting beat field label (desktop) | TODO |
| `dialog.properties.field.title.labelDesktop` | `Title:` | Title field label (desktop) | TODO |
| `dialog.properties.field.type.label` | `Type:` | Type field label (desktop only — web doesn't show type) | TODO |
| `dialog.properties.header` | `Edit composition details` | Properties dialog header text (desktop only) | TODO |
| `dialog.properties.validation.beatsClamped` | `Starting beats clamped to new taal range (1-{matras}) [1 param]` | Validation warning when starting beats exceed new taal range (desktop only) | TODO |

### dialog.support

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `dialog.support.international.platformLink` | `Support via {platform} [1 param]` | Platform-specific donation link text (desktop) | TODO |
| `dialog.support.upi.handleLabelWithValue` | `UPI handle: {handle} [1 param]` | Label with UPI handle value (desktop) | TODO |
| `dialog.support.upi.qrPlaceholder` | `(QR code image will appear here)` | Placeholder text when QR image fails to load (desktop only) | TODO |
| `dialog.support.windowTitle` | `Support — Sangeet Notes Editor` | Window title for support dialog (desktop only) | TODO |

### editor.sampleWarning

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `editor.sampleWarning` | `This is a read-only sample showing Yaman Vilambit Gat.` | Warning banner on sample composition | TODO |

### fileBrowser.addFolderDialogTitle

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.addFolderDialogTitle` | `Add Folder` | Desktop dialog title (web uses Drive integration) | TODO |

### fileBrowser.addFolderTooltip

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.addFolderTooltip` | `Add a folder` | Desktop tooltip for add-folder button (web uses Drive integration) | TODO |

### fileBrowser.deleteDialogPrompt

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.deleteDialogPrompt` | `Delete {filename}?` | Desktop dialog prompt — {filename} is a placeholder (web has no delete confirmation) | TODO |

### fileBrowser.deleteDialogTitle

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.deleteDialogTitle` | `Delete File` | Desktop dialog title (web has no delete confirmation dialog) | TODO |

### fileBrowser.deleteDialogWarning

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.deleteDialogWarning` | `This action cannot be undone.` | Desktop dialog warning (web has no delete confirmation) | TODO |

### fileBrowser.errorFileExists

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.errorFileExists` | `File already exists: {name}` | Desktop error message — {name} is a placeholder (web has no file creation) | TODO |

### fileBrowser.errorFolderExists

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.errorFolderExists` | `Folder already exists: {name}` | Desktop error message — {name} is a placeholder (web has no folder creation) | TODO |

### fileBrowser.errorFolderOpen

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.errorFolderOpen` | `Folder already open: {name}` | Desktop error message — {name} is a placeholder (web uses Drive integration) | TODO |

### fileBrowser.errorMoveExists

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.errorMoveExists` | `A file named {name} already exists in the destination` | Desktop error message — {name} is a placeholder (web has no move UI) | TODO |

### fileBrowser.errorNotDirectory

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.errorNotDirectory` | `Not a directory: {path}` | Desktop error message — {path} is a placeholder (web uses Drive integration) | TODO |

### fileBrowser.errorRenameExists

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.errorRenameExists` | `A file with that name already exists` | Desktop error message (web has no rename UI) | TODO |

### fileBrowser.headerLabel

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.headerLabel` | `FILES` | Desktop uppercase header label (web uses regular-case panelTitle) | TODO |

### fileBrowser.logAddedFolder

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.logAddedFolder` | `Added folder: {name}` | Desktop status message — {name} is a placeholder (web uses Drive integration) | TODO |

### fileBrowser.logBookmarked

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.logBookmarked` | `Bookmarked: {name}` | Desktop status message — {name} is a placeholder (web has no status log) | TODO |

### fileBrowser.logCreatedFile

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.logCreatedFile` | `Created: {name}` | Desktop status message — {name} is a placeholder (web has no file creation) | TODO |

### fileBrowser.logCreatedFolder

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.logCreatedFolder` | `Created folder: {name}` | Desktop status message — {name} is a placeholder (web has no folder creation) | TODO |

### fileBrowser.logDeleted

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.logDeleted` | `Deleted: {name}` | Desktop status message — {name} is a placeholder (web has no status log) | TODO |

### fileBrowser.logMoved

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.logMoved` | `Moved: {name} -> {dest}` | Desktop status message — {name} and {dest} are placeholders (web has no move) | TODO |

### fileBrowser.logRemovedBookmark

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.logRemovedBookmark` | `Removed bookmark: {name}` | Desktop status message — {name} is a placeholder (web has no status log) | TODO |

### fileBrowser.logRemovedFolder

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.logRemovedFolder` | `Removed folder: {name}` | Desktop status message — {name} is a placeholder (web uses Drive integration) | TODO |

### fileBrowser.logRenamed

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.logRenamed` | `Renamed: {old} -> {new}` | Desktop status message — {old} and {new} are placeholders (web has no rename) | TODO |

### fileBrowser.menuBookmark

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.menuBookmark` | `Bookmark` | Desktop context menu item (web uses icon button with tooltip) | TODO |

### fileBrowser.menuDelete

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.menuDelete` | `Delete` | Desktop context menu item (web uses icon button) | TODO |

### fileBrowser.menuMoveTo

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.menuMoveTo` | `Move to...` | Desktop context menu item (web has no move UI) | TODO |

### fileBrowser.menuNewFile

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.menuNewFile` | `New .swar File` | Desktop context menu item (web has no file creation UI) | TODO |

### fileBrowser.menuNewFolder

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.menuNewFolder` | `New Folder` | Desktop context menu item (web has no folder creation UI) | TODO |

### fileBrowser.menuOpen

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.menuOpen` | `Open` | Desktop context menu item (web has no file context menu) | TODO |

### fileBrowser.menuRefresh

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.menuRefresh` | `Refresh` | Desktop context menu item (web uses icon button with tooltip) | TODO |

### fileBrowser.menuRemoveBookmark

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.menuRemoveBookmark` | `Remove Bookmark` | Desktop context menu item (web uses icon button with tooltip) | TODO |

### fileBrowser.menuRemoveFromBrowser

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.menuRemoveFromBrowser` | `Remove from Browser` | Desktop context menu item (web uses Drive integration) | TODO |

### fileBrowser.menuRename

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.menuRename` | `Rename` | Desktop context menu item (web has no rename UI) | TODO |

### fileBrowser.moveToDialogTitle

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.moveToDialogTitle` | `Move to...` | Desktop dialog title (web has no move UI) | TODO |

### fileBrowser.newFileDialogPrompt

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.newFileDialogPrompt` | `Enter filename (without .swar extension)` | Desktop dialog prompt (web has no file creation UI) | TODO |

### fileBrowser.newFileDialogTitle

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.newFileDialogTitle` | `New Composition File` | Desktop dialog title (web has no file creation UI) | TODO |

### fileBrowser.newFolderDialogPrompt

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.newFolderDialogPrompt` | `Enter folder name` | Desktop dialog prompt (web has no folder creation UI) | TODO |

### fileBrowser.newFolderDialogTitle

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.newFolderDialogTitle` | `New Folder` | Desktop dialog title (web has no folder creation UI) | TODO |

### fileBrowser.renameDialogPrompt

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.renameDialogPrompt` | `Enter new name` | Desktop dialog prompt (web has no rename UI) | TODO |

### fileBrowser.renameDialogTitle

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.renameDialogTitle` | `Rename` | Desktop dialog title (web has no rename UI) | TODO |

### header.arohanLabel

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `header.arohanLabel` | `Arohan` | Desktop composition metadata label (web header shows cursor position, not metadata) | TODO |

### header.avrohanLabel

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `header.avrohanLabel` | `Avrohan` | Desktop composition metadata label (web header shows cursor position, not metadata) | TODO |

### header.layaLabel

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `header.layaLabel` | `Laya` | Desktop composition metadata label (web header shows cursor position, not metadata) | TODO |

### header.raagLabel

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `header.raagLabel` | `Raag` | Desktop composition metadata label (web header shows cursor position, not metadata) | TODO |

### header.samvadiLabel

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `header.samvadiLabel` | `Samvadi` | Desktop composition metadata label (web header shows cursor position, not metadata) | TODO |

### header.taalLabel

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `header.taalLabel` | `Taal` | Desktop composition metadata label (web header shows cursor position, not metadata) | TODO |

### header.thaatLabel

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `header.thaatLabel` | `Thaat` | Desktop composition metadata label (web header shows cursor position, not metadata) | TODO |

### header.vadiLabel

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `header.vadiLabel` | `Vadi` | Desktop composition metadata label (web header shows cursor position, not metadata) | TODO |

### keyboardLegend.nav

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `keyboardLegend.nav.enter` | `Next cycle` | Desktop description for Enter key (web doesn't show this) | TODO |
| `keyboardLegend.nav.moveCursor` | `Move cursor` | Desktop description for arrow keys | TODO |
| `keyboardLegend.nav.tab.desktop` | `Next beat` | Desktop description for Tab key | TODO |

### keyboardLegend.octave

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `keyboardLegend.octave.backToMadhya` | `Back to madhya` | Desktop description for backtick key (web doesn't have this) | TODO |
| `keyboardLegend.octave.mandra.desktop` | `Next note in mandra` | Desktop description for . key | TODO |
| `keyboardLegend.octave.taar.desktop` | `Next note in taar` | Desktop description for ' key | TODO |

### keyboardLegend.ornamentKeys

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `keyboardLegend.ornamentKeys.multiNote` | `..↵ = type notes, press Enter` | Desktop explanation for multi-note ornament syntax | TODO |
| `keyboardLegend.ornamentKeys.oneNote` | `♪  = type one swar key` | Desktop explanation for one-note ornament syntax | TODO |
| `keyboardLegend.ornamentKeys.twoNotes` | `♪♪ = type start, then end note` | Desktop explanation for two-note ornament syntax | TODO |

### keyboardLegend.ornaments

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `keyboardLegend.ornaments.andolan.desktop` | `Andolan (gentle oscillation)` | Desktop ornament description for Ctrl+A | TODO |
| `keyboardLegend.ornaments.gamak.desktop` | `Gamak (heavy oscillation)` | Desktop ornament description for Ctrl+G | TODO |
| `keyboardLegend.ornaments.ghaseet.desktop` | `Ghaseet (heavy pull)` | Desktop ornament description for Ctrl+E | TODO |
| `keyboardLegend.ornaments.gitkari.desktop` | `Gitkari (hammer/pull trill)` | Desktop ornament description for Ctrl+I | TODO |
| `keyboardLegend.ornaments.kan.desktop` | `Kan Swar (grace note)` | Desktop ornament description for Ctrl+K | TODO |
| `keyboardLegend.ornaments.krintan.desktop` | `Krintan (pull-off seq.)` | Desktop ornament description for Ctrl+J | TODO |
| `keyboardLegend.ornaments.meendAsc.desktop` | `Meend ↑ (ascending glide)` | Desktop ornament description for Ctrl+M | TODO |
| `keyboardLegend.ornaments.meendDesc.desktop` | `Meend ↓ (descending glide)` | Desktop ornament description for Ctrl+Shift+M | TODO |
| `keyboardLegend.ornaments.murki.desktop` | `Murki (ornamental turn)` | Desktop ornament description for Ctrl+U | TODO |
| `keyboardLegend.ornaments.sparsh.desktop` | `Sparsh (light touch)` | Desktop ornament description for Ctrl+H | TODO |
| `keyboardLegend.ornaments.zamzama.desktop` | `Zamzama (rapid cluster)` | Desktop ornament description for Ctrl+W | TODO |

### keyboardLegend.redo

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `keyboardLegend.redo.desktop` | `Redo` | Desktop description for Ctrl+Shift+Z (different from web Ctrl+Y) | TODO |

### keyboardLegend.scriptLabel

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `keyboardLegend.scriptLabel` | `Script: {scriptName}` | Script selection label shown on desktop — {scriptName} is a placeholder (web doesn't display script in legend) | TODO |

### keyboardLegend.section

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `keyboardLegend.section.ornamentKeys` | `Ornament Keys` | Desktop section title for ornament key legend (web doesn't have this) | TODO |
| `keyboardLegend.section.ornamentsMultiNote` | `Ornaments -- Multi-Note` | Desktop section title (web doesn't categorize by note count) | TODO |
| `keyboardLegend.section.ornamentsOneNote` | `Ornaments -- One Note` | Desktop section title (web doesn't categorize by note count) | TODO |
| `keyboardLegend.section.ornamentsSimple` | `Ornaments -- Simple` | Desktop section title for simple ornaments | TODO |
| `keyboardLegend.section.ornamentsTwoNotes` | `Ornaments -- Two Notes` | Desktop section title (web doesn't categorize by note count) | TODO |
| `keyboardLegend.section.strokesMizrab` | `Strokes (Mizrab)` | Desktop section title (web uses 'Strokes') | TODO |
| `keyboardLegend.section.subdivisions` | `Subdivisions` | Desktop section title (web shows subdivisions in Special section) | TODO |
| `keyboardLegend.section.swarNotes` | `Swar (Notes)` | Desktop section title (web uses 'Swar Input') | TODO |
| `keyboardLegend.section.tips` | `Tips` | Desktop section title for tips (web doesn't have this) | TODO |
| `keyboardLegend.section.undoRedoDesktop` | `Undo / Redo` | Desktop section title (web uses 'Undo/Redo') | TODO |

### keyboardLegend.special

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `keyboardLegend.special.deleteLast` | `Delete last note` | Desktop description for Del key (web shows in Navigation section) | TODO |
| `keyboardLegend.special.rest` | `Rest (silence)` | Desktop description for Space key (web shows in Swar Input section) | TODO |
| `keyboardLegend.special.sustain` | `Sustain (hold)` | Desktop description for - key (web shows in Swar Input section) | TODO |

### keyboardLegend.strokes

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `keyboardLegend.strokes.da` | `Da (inward stroke)` | Desktop description for Ctrl+D | TODO |
| `keyboardLegend.strokes.ra` | `Ra (outward stroke)` | Desktop description for Ctrl+R | TODO |

### keyboardLegend.subdivisions

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `keyboardLegend.subdivisions.doubleTap` | `Double-tap for dual swar` | Desktop description for ss/rr/gg.. | TODO |
| `keyboardLegend.subdivisions.setPerBeat` | `Set notes per beat (2-8)` | Desktop description for Ctrl+2-8 | TODO |

### keyboardLegend.tips

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `keyboardLegend.tips.applyToLast` | `Strokes & ornaments apply to the last entered note` | Desktop tip about stroke/ornament application | TODO |
| `keyboardLegend.tips.octaveReset` | `. and ' affect only the next note, then reset to madhya` | Desktop tip about octave modifier behavior | TODO |
| `keyboardLegend.tips.shiftVariant` | `Shift = komal/tivra variant` | Desktop tip about Shift key | TODO |

### keyboardLegend.title

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `keyboardLegend.title.desktop` | `Keyboard Reference` | Desktop uses 'Reference', web uses 'Shortcuts' | TODO |

### mainApp.openFolderDialogTitle

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `mainApp.openFolderDialogTitle` | `Open Folder` | Desktop folder chooser dialog title (web uses Drive picker) | TODO |

### status.clipboardEmpty

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.clipboardEmpty` | `Clipboard is empty` | Desktop error message | TODO |

### status.clipboardNotSangeetData

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.clipboardNotSangeetData` | `Clipboard does not contain Sangeet data` | Desktop error message | TODO |

### status.copiedEvents

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.copiedEvents` | `Copied {count} event(s)` | Desktop status message — {count} is a placeholder | TODO |

### status.cursorPlaced

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.cursorPlaced` | `Cursor placed at cycle {cycle}, beat {beat}` | Desktop status message — {cycle} and {beat} are placeholders | TODO |

### status.cutEvents

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.cutEvents` | `Cut {count} event(s)` | Desktop status message — {count} is a placeholder | TODO |

### status.errorOpeningFile

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.errorOpeningFile` | `Error opening file: {message}` | Desktop error message — {message} is a placeholder | TODO |

### status.errorOpeningHtml

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.errorOpeningHtml` | `Error opening HTML: {message}` | Desktop error message — {message} is a placeholder | TODO |

### status.errorReloading

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.errorReloading` | `Error reloading: {message}` | Desktop error message — {message} is a placeholder | TODO |

### status.fileWasDeleted

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.fileWasDeleted` | `File was deleted: {title}` | Desktop warning message — {title} is a placeholder | TODO |

### status.noEventsInSelection

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.noEventsInSelection` | `No events in selection` | Desktop error message | TODO |

### status.noSelection

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.noSelection` | `No selection` | Desktop error message | TODO |

### status.openedDesktop

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.openedDesktop` | `Opened: {filename}` | Desktop status message — {filename} is a placeholder | TODO |

### status.pastedEvents

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.pastedEvents` | `Pasted {count} event(s)` | Desktop status message — {count} is a placeholder | TODO |

### status.preview

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.preview` | `Preview: {filename}` | Desktop status message — {filename} is a placeholder | TODO |

### status.reloaded

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.reloaded` | `Reloaded: {filename}` | Desktop status message — {filename} is a placeholder | TODO |

### status.sampleDismissed

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.sampleDismissed` | `Sample dismissed — won't appear on next launch` | Status message when user closes sample tab | TODO |

### status.sampleLoaded

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.sampleLoaded` | `Uneditable sample loaded` | Status message when sample composition is loaded | TODO |

### status.samplePrompt

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.samplePrompt` | `To start, click New to create a composition` | Status prompt when sample is shown | TODO |

### status.switchedToSectionDesktop

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.switchedToSectionDesktop` | `Switched to section: {name}` | Desktop status message — {name} is a placeholder (web uses section number) | TODO |

### statusBar.logLabel

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `statusBar.logLabel` | `Log` | Status bar header label (desktop only; web has no header label) | TODO |

### toolbar.edit

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `toolbar.edit.redo.tooltip.desktop` | `Redo (Ctrl+Shift+Z)` | Tooltip for Redo button on desktop | TODO |
| `toolbar.edit.undo.tooltip.desktop` | `Undo last edit (Ctrl+Z)` | Tooltip for Undo button on desktop | TODO |

### toolbar.file

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `toolbar.file.copy.tooltip.desktop` | `Copy selected events (Ctrl+C)` | Tooltip for Copy button on desktop | TODO |
| `toolbar.file.cut.tooltip.desktop` | `Cut selected events (Ctrl+X)` | Tooltip for Cut button on desktop | TODO |
| `toolbar.file.exportHtml.tooltip.desktop` | `Export composition as HTML` | Tooltip for Export HTML button on desktop (shortcut added via ShortcutText) | TODO |
| `toolbar.file.new.tooltip.desktop` | `Create a new composition` | Tooltip for New button on desktop (shortcut added via ShortcutText) | TODO |
| `toolbar.file.open.tooltip.desktop` | `Open a .swar file` | Tooltip for Open button on desktop (shortcut added via ShortcutText) | TODO |
| `toolbar.file.openFolder.tooltip` | `Open a folder in the file browser` | Tooltip for Open Folder button (desktop only, shortcut added via ShortcutText) | TODO |
| `toolbar.file.paste.tooltip.desktop` | `Paste clipboard events (Ctrl+V)` | Tooltip for Paste button on desktop | TODO |
| `toolbar.file.save.tooltip.desktop` | `Save composition to current file` | Tooltip for Save button on desktop (shortcut added via ShortcutText) | TODO |
| `toolbar.file.saveAs.tooltip` | `Save composition as a new .swar file` | Tooltip for Save As button (desktop only, shortcut added via ShortcutText) | TODO |

### toolbar.help

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `toolbar.help.about.tooltip.desktop` | `About Sangeet Notes Editor` | Tooltip for About button on desktop | TODO |
| `toolbar.help.keyboardShortcuts.tooltip.desktop` | `Show keyboard shortcuts (?)` | Tooltip for keyboard shortcuts button on desktop | TODO |
| `toolbar.help.properties.tooltip.desktop` | `Edit composition metadata` | Tooltip for Properties button on desktop (shortcut added via ShortcutText) | TODO |
| `toolbar.help.reportBug.tooltip.desktop` | `Report a bug — includes a screenshot + recent keystrokes + the open composition` | Tooltip for Report bug button on desktop (shortcut added via ShortcutText) | TODO |
| `toolbar.help.support.tooltip.desktop` | `Support the project` | Tooltip for support button on desktop | TODO |
| `toolbar.help.userGuide.tooltip` | `Open the user guide (F1)` | Tooltip for user guide button (desktop only) | TODO |

### toolbar.script

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `toolbar.script.devanagari.desktop` | `Devanagari (Hindi)` | Devanagari script option in dropdown (desktop) | TODO |
| `toolbar.script.tooltip` | `Change notation script` | Tooltip for script selector (desktop only) | TODO |

### toolbar.section

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `toolbar.section.add.tooltip.desktop` | `Add a new section to the composition` | Tooltip for add section button on desktop (shortcut added via ShortcutText) | TODO |
| `toolbar.section.remove.tooltip.desktop` | `Remove the current section` | Tooltip for remove section button on desktop (shortcut added via ShortcutText) | TODO |
| `toolbar.section.rename.tooltip.desktop` | `Rename the current section (F2)` | Tooltip for rename section button on desktop | TODO |

### toolbar.theme

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `toolbar.theme.toggle.tooltip` | `Toggle light / dark theme` | Tooltip for theme toggle button (desktop only, shortcut added via ShortcutText) | TODO |


## Web-only entries (review one-by-one)

Grouped by `area.component` prefix for easier review.

### action.addSection

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `action.addSection.defaultName` | `New Section` | Default name for new section | TODO |

### appAction.redo

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `appAction.redo` | `Redo` | Command palette action title (desktop doesn't include undo/redo in palette) | TODO |

### appAction.supportProject

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `appAction.supportProject` | `Support the project` | Command palette action title (desktop doesn't have this in palette) | TODO |

### appAction.toggleKeyboardLegend

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `appAction.toggleKeyboardLegend` | `Toggle keyboard legend` | Command palette action title (desktop doesn't expose this in palette) | TODO |

### appAction.toggleSahityaLine

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `appAction.toggleSahityaLine` | `Toggle sahitya line` | Command palette action title (desktop doesn't expose view toggles in palette) | TODO |

### appAction.toggleStrokeLine

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `appAction.toggleStrokeLine` | `Toggle stroke line` | Command palette action title (desktop doesn't expose view toggles in palette) | TODO |

### appAction.undo

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `appAction.undo` | `Undo` | Command palette action title (desktop doesn't include undo/redo in palette) | TODO |

### dialog.about

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `dialog.about.betaNote` | `Beta release — actively iterating toward v1.0. Expect rough edges; please file bugs via the 🐞 Report bug button in the toolbar.` | Beta note in about dialog (web) | TODO |
| `dialog.about.copyright` | `© 2026 Bharadwaj. ` | Copyright text (web) | TODO |
| `dialog.about.description.paragraph1` | `A notation editor for Hindustani classical music in the Bhatkhande style. Built for sitar compositions: gat, bandish, palta — with mizrab strokes, meend, kan swar, gamak, and the full Bhatkhande notation set.` | First description paragraph in about dialog (web) | TODO |
| `dialog.about.description.paragraph2` | `Supports Devanagari, Kannada, Telugu, and English scripts.` | Second description paragraph in about dialog (web only) | TODO |
| `dialog.about.license` | `Free and open source under the MIT License.` | License text (web) | TODO |
| `dialog.about.links.header` | `Links` | Links section header in about dialog (web only) | TODO |
| `dialog.about.links.selfHosting` | `Self-hosting guide` | Link text for self-hosting guide (web only) | TODO |
| `dialog.about.links.userGuide` | `User guide` | Link text for user guide (web) | TODO |
| `dialog.about.privacy.header` | `Privacy` | Privacy section header (web) | TODO |
| `dialog.about.privacy.text` | `While you use the app, anonymous usage events (clicks, keystrokes — never the text content of fields) are sent to PostHog so I can see which features people actually reach for. If you click "🐞 Report bug", the last few minutes of your activity in this page are recorded as a video-like replay and sent along with your message so I can reproduce what you saw. Password fields are never captured. Nothing leaves your browser unless you click Send. Reports auto-delete from storage after 90 days. The desktop app sends a smaller, separate set of anonymous events to a different PostHog project for the same reason; users can opt out by setting SANGEET_ANALYTICS_DISABLED=1.` | Privacy text (web) | TODO |
| `dialog.about.support.link` | `Support the project` | Support link text (web) | TODO |
| `dialog.about.support.suffix` | ` — UPI / PayPal options.` | Support text suffix (web) | TODO |
| `dialog.about.support.text` | `💖 ` | Support emoji prefix (web) | TODO |
| `dialog.about.tech` | `Desktop: Scala 3 + ScalaFX. Web: Elm + Tapir.` | Tech stack note (web) | TODO |

### dialog.bugReport

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `dialog.bugReport.disclosure.web` | `We'll include a short replay of your recent actions in the app (the last few minutes only) so the bug can be reproduced. Password fields are never captured. Nothing leaves your browser until you click Send below.` | Web disclosure text - mentions browser + recent actions replay (PostHog) | TODO |

### dialog.commandPalette

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `dialog.commandPalette.noResults` | `No matching actions.` | Web-only empty-state message (desktop shows empty ListView without text) | TODO |
| `dialog.commandPalette.searchPlaceholderWeb` | `Search actions… (Esc to close, ↑↓ to navigate, Enter to run)` | Web placeholder includes navigation help (desktop handles navigation differently) | TODO |

### dialog.keyboardCheatSheet

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `dialog.keyboardCheatSheet.hint.desktopFull` | `-shortcuts wired (browsers reserve many of them on web). Full reference:` | Hint paragraph middle part before link (web only) | TODO |
| `dialog.keyboardCheatSheet.hint.keyboardRef` | `Keyboard Reference` | Link text to keyboard reference doc (web only) | TODO |
| `dialog.keyboardCheatSheet.hint.web` | `Tip: most toolbar actions are accessible via the buttons above. The desktop app has the full set of` | Hint paragraph start (web only) | TODO |
| `dialog.keyboardCheatSheet.label.cancelOrnament` | `Cancel ornament mode` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.label.chikari` | `Chikari (open strings)` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.label.cutCopyPaste` | `Cut / Copy / Paste` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.label.daRaStrokes` | `Da (inward) / Ra (outward)` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.label.deleteEvent` | `Delete event` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.label.doubleTapDual` | `Double-tap dual swar` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.label.extendSelection` | `Extend selection` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.label.fastTyping` | `Type 2–4 notes within 500 ms to auto-group` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.label.finishOrnament` | `Finish multi-note ornament` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.label.gamakAndolan` | `Gamak / Andolan / Gitkari` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.label.kanSwar` | `Kan swar` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.label.komalRe` | `Komal Re / Ga / Dha / Ni` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.label.madhyaDefault` | `Madhya (default)` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.label.mandraLower` | `Mandra (lower)` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.label.meendDown` | `Meend ↓` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.label.meendUp` | `Meend ↑` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.label.moveCursor` | `Move cursor one beat` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.label.nextSubbeat` | `Next sub-beat` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.label.rest` | `Rest` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.label.setNotesPerBeat` | `Set notes per beat` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.label.showCheatSheet` | `Show this cheat sheet` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.label.shuddhaSwaras` | `Shuddha swaras` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.label.sparsh` | `Sparsh` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.label.sustain` | `Sustain` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.label.taarUpper` | `Taar (upper)` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.label.tivraMa` | `Tivra Ma` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.label.undoRedo` | `Undo / Redo` | Shortcut label (web only) | TODO |
| `dialog.keyboardCheatSheet.section.help.web` | `Help` | Section title (web only) | TODO |
| `dialog.keyboardCheatSheet.section.navigation` | `Navigation` | Section title (web only) | TODO |
| `dialog.keyboardCheatSheet.section.octave` | `Octave (saptak)` | Section title (web only) | TODO |
| `dialog.keyboardCheatSheet.section.ornaments` | `Ornaments` | Section title (web only) | TODO |
| `dialog.keyboardCheatSheet.section.selectionClipboard` | `Selection & clipboard` | Section title (web only) | TODO |
| `dialog.keyboardCheatSheet.section.strokes` | `Strokes` | Section title (web only) | TODO |
| `dialog.keyboardCheatSheet.section.subdivisions` | `Subdivisions` | Section title (web only) | TODO |
| `dialog.keyboardCheatSheet.section.swar` | `Swar (notes)` | Section title (web only) | TODO |

### dialog.newComposition

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `dialog.newComposition.button.cancel` | `Cancel` | Cancel button text (web only — desktop uses JavaFX ButtonType.CANCEL) | TODO |
| `dialog.newComposition.button.create` | `Create` | Create button text (web) | TODO |
| `dialog.newComposition.field.antaraStartingBeat.label` | `Antara Starting Beat (1-{matras}) [1 param]` | Antara starting beat field label with range (web) | TODO |
| `dialog.newComposition.field.gatStartingBeat.label` | `Gat Starting Beat (1-{matras}) [1 param]` | Gat starting beat field label with range (web) | TODO |
| `dialog.newComposition.field.laya.atidrut` | `Ati-drut` | Ati-drut laya option (web) | TODO |
| `dialog.newComposition.field.laya.ativilambit` | `Ati-vilambit` | Ati-vilambit laya option (web) | TODO |
| `dialog.newComposition.field.laya.label` | `Laya` | Laya field label (web) | TODO |
| `dialog.newComposition.field.laya.none` | `None (Palta)` | None laya option (web) | TODO |
| `dialog.newComposition.field.raag.label` | `Raag` | Raag field label (web) | TODO |
| `dialog.newComposition.field.showSahitya.label` | `Show Sahitya Line (Lyrics)` | Show sahitya checkbox label (web) | TODO |
| `dialog.newComposition.field.showStrokes.label` | `Show Stroke Line (Da/Ra)` | Show strokes checkbox label (web) | TODO |
| `dialog.newComposition.field.sthayiStartingBeat.label` | `Sthayi Starting Beat (1-{matras}) [1 param]` | Sthayi starting beat field label with range for Bandish (web) | TODO |
| `dialog.newComposition.field.taal.label` | `Taal` | Taal field label (web) | TODO |
| `dialog.newComposition.field.taanCount.label` | `Taan Count` | Taan count field label (web) | TODO |
| `dialog.newComposition.field.taanStartingBeat.label` | `Taan Starting Beat (1-{matras}) [1 param]` | Taan starting beat field label with range (web) | TODO |
| `dialog.newComposition.field.title.label` | `Title` | Title field label (web) | TODO |
| `dialog.newComposition.field.title.placeholder` | `Enter composition title` | Title field placeholder (web) | TODO |
| `dialog.newComposition.field.type.bandish` | `Bandish (Vocal)` | Bandish composition type option (web) | TODO |
| `dialog.newComposition.field.type.gat` | `Gat (Instrumental)` | Gat composition type option (web) | TODO |
| `dialog.newComposition.field.type.label` | `Type` | Type field label (web) | TODO |
| `dialog.newComposition.field.type.palta` | `Palta (Practice)` | Palta composition type option (web) | TODO |
| `dialog.newComposition.field.type.sargam` | `Sargam (Practice)` | Sargam composition type option (web) | TODO |

### dialog.properties

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `dialog.properties.button.cancel` | `Cancel` | Cancel button text (web only — desktop uses JavaFX ButtonType.CANCEL) | TODO |
| `dialog.properties.button.save` | `Save` | Save button text (web) | TODO |
| `dialog.properties.field.sectionStartingBeat.label` | `{name} Starting Beat (1-{matras}) [2 params]` | Section starting beat field label with range (web) | TODO |
| `dialog.properties.field.taal.label` | `Taal` | Taal field label (web) | TODO |
| `dialog.properties.field.title.label` | `Title` | Title field label (web) | TODO |
| `dialog.properties.field.title.placeholder` | `Composition title` | Title field placeholder (web only) | TODO |

### dialog.support

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `dialog.support.international.paypalLink` | `Support via PayPal` | PayPal donation link text (web) | TODO |
| `dialog.support.upi.handle` | `bharath12345-1@oksbi` | UPI handle value (web only) | TODO |
| `dialog.support.upi.handleLabel` | `UPI handle: ` | Label prefix for UPI handle (web) | TODO |
| `dialog.support.upi.qrAlt` | `UPI QR code` | Alt text for UPI QR code image | TODO |

### fileBrowser.bookmarkTooltip

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.bookmarkTooltip` | `Bookmark` | Web tooltip for bookmark button (desktop uses context menu) | TODO |

### fileBrowser.connectDrive

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.connectDrive` | `Connect Google Drive` | Web-only Google Drive integration button (desktop uses local filesystem) | TODO |

### fileBrowser.connecting

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.connecting` | `Connecting...` | Web-only Drive connection status (desktop uses local filesystem) | TODO |

### fileBrowser.deleteTooltip

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.deleteTooltip` | `Delete` | Web tooltip for delete button (desktop uses context menu) | TODO |

### fileBrowser.driveConnected

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.driveConnected` | `Drive connected` | Web-only Drive connection status (desktop uses local filesystem) | TODO |

### fileBrowser.emptyState

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.emptyState` | `Connect Drive to browse files` | Web-only empty state message (desktop uses local filesystem) | TODO |

### fileBrowser.hideFilesTooltip

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.hideFilesTooltip` | `Hide Files` | Web tooltip for expanded panel button (desktop has no tooltip) | TODO |

### fileBrowser.newFolder

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.newFolder.defaultName` | `New Folder` | Default name for new folder | TODO |

### fileBrowser.refreshTooltip

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.refreshTooltip` | `Refresh` | Web tooltip for refresh button (desktop uses context menu) | TODO |

### fileBrowser.removeBookmarkTooltip

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.removeBookmarkTooltip` | `Remove bookmark` | Web tooltip for unbookmark button (desktop uses context menu) | TODO |

### fileBrowser.showFilesTooltip

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `fileBrowser.showFilesTooltip` | `Show Files` | Web tooltip for collapsed panel button (desktop has no tooltip) | TODO |

### header.beatPrefix

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `header.beatPrefix` | `Beat ` | Web header cursor position: beat number (desktop header shows metadata, not cursor) | TODO |

### header.cyclePrefix

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `header.cyclePrefix` | `Cycle ` | Web header cursor position: cycle number (desktop header shows metadata, not cursor) | TODO |

### header.modeLabel

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `header.modeLabel` | `Mode: ` | Web header cursor position: edit mode label (desktop header shows metadata, not cursor) | TODO |

### header.modeStroke

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `header.modeStroke` | `Stroke` | Web header edit mode: stroke editing (desktop header shows metadata, not cursor) | TODO |

### header.modeSwar

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `header.modeSwar` | `Swar` | Web header edit mode: swar editing (desktop header shows metadata, not cursor) | TODO |

### header.octaveAtiMandra

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `header.octaveAtiMandra` | `Ati-Mandra` | Web header octave name (desktop header shows metadata, not cursor) | TODO |

### header.octaveAtiTaar

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `header.octaveAtiTaar` | `Ati-Taar` | Web header octave name (desktop header shows metadata, not cursor) | TODO |

### header.octaveLabel

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `header.octaveLabel` | `Octave: ` | Web header cursor position: octave label (desktop header shows metadata, not cursor) | TODO |

### header.octaveMadhya

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `header.octaveMadhya` | `Madhya` | Web header octave name (desktop header shows metadata, not cursor) | TODO |

### header.octaveMandra

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `header.octaveMandra` | `Mandra` | Web header octave name (desktop header shows metadata, not cursor) | TODO |

### header.octaveTaar

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `header.octaveTaar` | `Taar` | Web header octave name (desktop header shows metadata, not cursor) | TODO |

### header.subPrefix

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `header.subPrefix` | `Sub ` | Web header cursor position: subdivision number (desktop header shows metadata, not cursor) | TODO |

### keyboardLegend.nav

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `keyboardLegend.nav.backspace.web` | `Delete last` | Web description for Backspace key | TODO |
| `keyboardLegend.nav.prevNextBeat.web` | `Previous / Next beat` | Web description for arrow keys | TODO |
| `keyboardLegend.nav.tab.web` | `Next sub-beat` | Web description for Tab key | TODO |

### keyboardLegend.octave

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `keyboardLegend.octave.madhya.web` | `Madhya (middle)` | Web description for backslash key | TODO |
| `keyboardLegend.octave.mandra.web` | `Mandra (lower)` | Web description for [ key | TODO |
| `keyboardLegend.octave.taar.web` | `Taar (upper)` | Web description for ] key | TODO |

### keyboardLegend.ornaments

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `keyboardLegend.ornaments.andolan` | `Andolan` | Web ornament description for Alt+a | TODO |
| `keyboardLegend.ornaments.gamak` | `Gamak` | Web ornament description for Alt+g | TODO |
| `keyboardLegend.ornaments.ghaseet` | `Ghaseet (then type note)` | Web ornament description for Alt+h | TODO |
| `keyboardLegend.ornaments.gitkari` | `Gitkari` | Web ornament description for Alt+i | TODO |
| `keyboardLegend.ornaments.kan` | `Kan Swar (then type note)` | Web ornament description for Alt+k | TODO |
| `keyboardLegend.ornaments.krintan.web` | `Krintan (type notes, Enter)` | Web ornament description for Alt+r | TODO |
| `keyboardLegend.ornaments.meendAsc` | `Meend Asc (type start, end)` | Web ornament description for Alt+m | TODO |
| `keyboardLegend.ornaments.meendDesc` | `Meend Desc` | Web ornament description for Alt+Shift+M | TODO |
| `keyboardLegend.ornaments.murki` | `Murki (type notes, Enter)` | Web ornament description for Alt+u | TODO |
| `keyboardLegend.ornaments.sparsh` | `Sparsh (then type note)` | Web ornament description for Alt+s | TODO |
| `keyboardLegend.ornaments.zamzama` | `Zamzama (type notes, Enter)` | Web ornament description for Alt+z | TODO |

### keyboardLegend.redo

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `keyboardLegend.redo.web` | `Redo` | Web description for Ctrl+Y | TODO |

### keyboardLegend.section

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `keyboardLegend.section.ornaments` | `Ornaments (Alt+key)` | Web section title showing Alt modifier | TODO |
| `keyboardLegend.section.strokes` | `Strokes` | Web section title (desktop uses 'Strokes (Mizrab)') | TODO |
| `keyboardLegend.section.swarInput` | `Swar Input` | Web section title (desktop uses 'Swar (Notes)') | TODO |
| `keyboardLegend.section.undoRedo` | `Undo/Redo` | Web section title (desktop uses 'Undo / Redo') | TODO |

### keyboardLegend.special

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `keyboardLegend.special.subdivisions` | `Set subdivisions per beat` | Web description for 2-8 keys | TODO |

### keyboardLegend.strokes

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `keyboardLegend.strokes.keys.web` | `Da / Ra / Jod (in stroke mode)` | Web description for d r j keys | TODO |
| `keyboardLegend.strokes.toggleMode.web` | `Toggle Swar/Stroke mode` | Web description for Shift+Tab | TODO |

### keyboardLegend.swar

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `keyboardLegend.swar.dualSwar` | `Dual swar (double-tap)` | Web description for ss rr gg etc. | TODO |
| `keyboardLegend.swar.komal` | `Komal variants` | Web description for Shift+R G D N keys | TODO |
| `keyboardLegend.swar.rest` | `Rest` | Web description for dash key | TODO |
| `keyboardLegend.swar.shuddha` | `Shuddha notes` | Web description for s r g m p d n keys | TODO |
| `keyboardLegend.swar.sustain` | `Sustain` | Web description for equals key | TODO |
| `keyboardLegend.swar.tivraMa` | `Tivra Ma` | Web description for Shift+M key | TODO |

### keyboardLegend.title

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `keyboardLegend.title.web` | `Keyboard Shortcuts` | Web uses 'Shortcuts', desktop uses 'Reference' | TODO |

### status.apiError

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.apiError` | `API error: {message}` | Web error message — {message} is a placeholder (desktop doesn't use REST API) | TODO |

### status.badBody

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.badBody` | `Bad body: {error}` | Web HTTP error — {error} is a placeholder (desktop doesn't use HTTP) | TODO |

### status.badStatus

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.badStatus` | `Bad status: {code}` | Web HTTP error — {code} is a placeholder (desktop doesn't use HTTP) | TODO |

### status.badUrl

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.badUrl` | `Bad URL: {url}` | Web HTTP error — {url} is a placeholder (desktop doesn't use HTTP) | TODO |

### status.bugReportFailed

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.bugReportFailed` | `Bug report failed: {message}` | Web error message — {message} is a placeholder (desktop doesn't have inline bug reporting) | TODO |

### status.bugReportSent

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.bugReportSent` | `Bug report sent — thanks! ({message})` | Web status message — {message} is a placeholder (desktop doesn't have inline bug reporting) | TODO |

### status.closedTabSwitched

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.closedTabSwitched` | `Closed tab, switched to {filename}` | Web status message — {filename} is a placeholder (desktop doesn't log tab operations) | TODO |

### status.colorsLoaded

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.colorsLoaded` | `Colors loaded` | Web status message (desktop loads silently) | TODO |

### status.connectedToDrive

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.connectedToDrive` | `Connected to Google Drive` | Web status message (desktop uses local filesystem) | TODO |

### status.created

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.created` | `Created: {title}` | Web status message — {title} is a placeholder (desktop doesn't have status log) | TODO |

### status.driveAuthFailed

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.driveAuthFailed` | `Drive authentication failed` | Web error message (desktop uses local filesystem) | TODO |

### status.driveError

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.driveError` | `Drive error: {message}` | Web error message — {message} is a placeholder (desktop uses local filesystem) | TODO |

### status.exportingHtml

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.exportingHtml` | `Exporting HTML...` | Web status message (desktop doesn't show export progress) | TODO |

### status.failedToParseDriveFileContent

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.failedToParseDriveFileContent` | `Failed to parse Drive file content` | Web error message (desktop uses local filesystem) | TODO |

### status.failedToParseDriveFolderListing

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.failedToParseDriveFolderListing` | `Failed to parse Drive folder listing` | Web error message (desktop uses local filesystem) | TODO |

### status.fileSavedToDrive

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.fileSavedToDrive` | `File saved to Drive` | Web status message (desktop uses local filesystem) | TODO |

### status.fileSelected

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.fileSelected` | `File selected: {filename}` | Web status message — {filename} is a placeholder (desktop doesn't show file picker status) | TODO |

### status.httpError

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.httpError` | `HTTP error: {message}` | Web error message — {message} is a placeholder (desktop doesn't use HTTP) | TODO |

### status.lastTabClosedNewCreated

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.lastTabClosedNewCreated` | `Last tab closed — new tab created` | Web status message (desktop doesn't log tab operations) | TODO |

### status.loadedRaags

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.loadedRaags` | `Loaded {count} raags` | Web status message — {count} is a placeholder (desktop loads silently) | TODO |

### status.loadedTaals

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.loadedTaals` | `Loaded {count} taals` | Web status message — {count} is a placeholder (desktop loads silently) | TODO |

### status.loadingFileFromDrive

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.loadingFileFromDrive` | `Loading file from Drive: {filename}` | Web status message — {filename} is a placeholder (desktop uses local filesystem) | TODO |

### status.networkError

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.networkError` | `Network error` | Web HTTP error category (desktop doesn't use network) | TODO |

### status.newTab

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.newTab` | `New tab` | Web status message (desktop doesn't log tab operations) | TODO |

### status.noSelectionToCopy

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.noSelectionToCopy` | `No selection to copy` | Web error message | TODO |

### status.noSelectionToCut

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.noSelectionToCut` | `No selection to cut` | Web error message | TODO |

### status.nothingToRedo

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.nothingToRedo` | `Nothing to redo` | Web error message (desktop doesn't log redo operations) | TODO |

### status.nothingToUndo

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.nothingToUndo` | `Nothing to undo` | Web error message (desktop doesn't log undo operations) | TODO |

### status.opened

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.opened` | `Opened: {title}` | Web status message — {title} is a placeholder | TODO |

### status.openingFromDrive

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.openingFromDrive` | `Opening from Drive: {filename}` | Web status message — {filename} is a placeholder (desktop uses local filesystem) | TODO |

### status.ornamentCancelled

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.ornamentCancelled` | `Ornament mode cancelled` | Status message when ornament mode is cancelled | TODO |

### status.ornamentCollecting

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.ornamentCollecting` | `Collecting ornament notes...` | Web status message (desktop doesn't show ornament progress) | TODO |

### status.ornamentGhaseet

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.ornamentGhaseet` | `Ghaseet: type the target note` | Web ornament mode prompt (desktop doesn't show prompts in status) | TODO |

### status.ornamentKanSwar

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.ornamentKanSwar` | `Kan Swar: type the grace note` | Web ornament mode prompt (desktop doesn't show prompts in status) | TODO |

### status.ornamentKrintan

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.ornamentKrintan` | `Krintan: type notes, then Enter` | Web ornament mode prompt (desktop doesn't show prompts in status) | TODO |

### status.ornamentMeendAsc

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.ornamentMeendAsc` | `Meend (ascending): type start note` | Web ornament mode prompt (desktop doesn't show prompts in status) | TODO |

### status.ornamentMeendDesc

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.ornamentMeendDesc` | `Meend (descending): type start note` | Web ornament mode prompt (desktop doesn't show prompts in status) | TODO |

### status.ornamentMurki

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.ornamentMurki` | `Murki: type notes, then Enter` | Web ornament mode prompt (desktop doesn't show prompts in status) | TODO |

### status.ornamentSparsh

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.ornamentSparsh` | `Sparsh: type the touch note` | Web ornament mode prompt (desktop doesn't show prompts in status) | TODO |

### status.ornamentZamzama

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.ornamentZamzama` | `Zamzama: type notes, then Enter` | Web ornament mode prompt (desktop doesn't show prompts in status) | TODO |

### status.pleaseSelectValidTaalRaag

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.pleaseSelectValidTaalRaag` | `Please select a valid taal and raag` | Web validation error (desktop has different dialog validation) | TODO |

### status.propertiesUpdatedTaal

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.propertiesUpdatedTaal` | `Properties updated — taal: {taalName}` | Web status message — {taalName} is a placeholder (desktop doesn't have status log) | TODO |

### status.propertiesUpdatedTaalNotFound

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.propertiesUpdatedTaalNotFound` | `Properties updated (taal not found, kept previous)` | Web status message (desktop doesn't have status log) | TODO |

### status.redo

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.redo` | `Redo` | Web status message (desktop doesn't log redo operations) | TODO |

### status.requestTimeout

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.requestTimeout` | `Request timed out` | Web HTTP error (desktop doesn't use HTTP) | TODO |

### status.sahityaLineHidden

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.sahityaLineHidden` | `Sahitya line hidden` | Web status message (desktop doesn't log view toggles) | TODO |

### status.sahityaLineShown

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.sahityaLineShown` | `Sahitya line shown` | Web status message (desktop doesn't log view toggles) | TODO |

### status.savingComposition

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.savingComposition` | `Saving composition...` | Web status message (desktop saves synchronously) | TODO |

### status.scriptChanged

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.scriptChanged` | `Script changed to {scriptName}` | Web status message — {scriptName} is a placeholder (desktop doesn't log script changes) | TODO |

### status.sectionAdded

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.sectionAdded` | `Section added` | Web status message (desktop doesn't have status log) | TODO |

### status.sectionRemoved

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.sectionRemoved` | `Section removed` | Web status message (desktop doesn't have status log) | TODO |

### status.sectionRenamed

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.sectionRenamed` | `Section renamed` | Web status message (desktop doesn't have status log) | TODO |

### status.sectionsReordered

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.sectionsReordered` | `Sections reordered` | Web status message (desktop doesn't have status log) | TODO |

### status.startingBeatsUpdated

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.startingBeatsUpdated` | `Starting beats updated` | Web status message (desktop doesn't have status log) | TODO |

### status.strokeLineHidden

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.strokeLineHidden` | `Stroke line hidden` | Web status message (desktop doesn't log view toggles) | TODO |

### status.strokeLineShown

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.strokeLineShown` | `Stroke line shown` | Web status message (desktop doesn't log view toggles) | TODO |

### status.switchedToSection

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.switchedToSection` | `Switched to section {number}` | Web status message — {number} is a placeholder (desktop uses 'Switched to section: {name}') | TODO |

### status.switchedToTab

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.switchedToTab` | `Switched to {filename}` | Web status message — {filename} is a placeholder (desktop doesn't log tab switches) | TODO |

### status.tabClosed

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.tabClosed` | `Tab closed` | Web status message (desktop doesn't log tab operations) | TODO |

### status.undo

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `status.undo` | `Undo` | Web status message (desktop doesn't log undo operations) | TODO |

### toolbar.edit

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `toolbar.edit.redo` | `Redo` | Redo button label (web uses text, desktop uses icon) | TODO |
| `toolbar.edit.redo.tooltip` | `Redo (Ctrl+Y)` | Tooltip for Redo button on web | TODO |
| `toolbar.edit.undo` | `Undo` | Undo button label (web uses text, desktop uses icon) | TODO |
| `toolbar.edit.undo.tooltip` | `Undo (Ctrl+Z)` | Tooltip for Undo button on web | TODO |

### toolbar.file

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `toolbar.file.copy.tooltip` | `Copy (Ctrl+C)` | Tooltip for Copy button on web | TODO |
| `toolbar.file.cut.tooltip` | `Cut (Ctrl+X)` | Tooltip for Cut button on web | TODO |
| `toolbar.file.exportHtml` | `HTML` | Export HTML button label (web uses text, desktop uses icon) | TODO |
| `toolbar.file.exportHtml.tooltip` | `Export HTML` | Tooltip for Export HTML button on web | TODO |
| `toolbar.file.new` | `New` | New button label (web uses text, desktop uses icon) | TODO |
| `toolbar.file.new.tooltip` | `New Composition (Ctrl+N)` | Tooltip for New button on web | TODO |
| `toolbar.file.open` | `Open` | Open button label (web uses text, desktop uses icon) | TODO |
| `toolbar.file.open.tooltip` | `Open File` | Tooltip for Open button on web | TODO |
| `toolbar.file.paste.tooltip` | `Paste (Ctrl+V)` | Tooltip for Paste button on web | TODO |
| `toolbar.file.save` | `Save` | Save button label (web uses text, desktop uses icon) | TODO |
| `toolbar.file.save.tooltip` | `Save File (Ctrl+S)` | Tooltip for Save button on web | TODO |

### toolbar.help

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `toolbar.help.about` | `About` | About button label (web uses text, desktop uses icon) | TODO |
| `toolbar.help.about.tooltip` | `About` | Tooltip for About button on web | TODO |
| `toolbar.help.keyboardShortcuts` | `?` | Keyboard shortcuts button label | TODO |
| `toolbar.help.keyboardShortcuts.tooltip` | `Keyboard shortcuts (?)` | Tooltip for keyboard shortcuts button on web | TODO |
| `toolbar.help.properties` | `Properties` | Properties button label (web uses text, desktop uses icon) | TODO |
| `toolbar.help.properties.tooltip` | `Composition Properties` | Tooltip for Properties button on web | TODO |
| `toolbar.help.reportBug` | `🐞 Report bug` | Report bug button label on web | TODO |
| `toolbar.help.reportBug.tooltip` | `Report a bug — includes a short replay so it can be reproduced` | Tooltip for Report bug button on web | TODO |
| `toolbar.help.support` | `💖` | Support button emoji label | TODO |
| `toolbar.help.support.tooltip` | `Support the project — donate via UPI or PayPal` | Tooltip for support button on web | TODO |

### toolbar.mode

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `toolbar.mode.stroke` | `Mode: Stroke` | Edit mode indicator when in Stroke mode (web only — desktop doesn't show edit mode in toolbar) | TODO |
| `toolbar.mode.swar` | `Mode: Swar` | Edit mode indicator when in Swar mode (web only — desktop doesn't show edit mode in toolbar) | TODO |

### toolbar.ornament

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `toolbar.ornament.krintanEnd` | `Krintan: type end note / Enter` | Ornament-mode badge when entering krintan end note (web only — desktop shows in status bar) | TODO |
| `toolbar.ornament.krintanStart` | `Krintan: type start note` | Ornament-mode badge when starting a krintan (web only — desktop shows in status bar) | TODO |
| `toolbar.ornament.meendEnd` | `Meend: type end note` | Ornament-mode badge when entering meend end note (web only — desktop shows in status bar) | TODO |
| `toolbar.ornament.meendStart` | `Meend: type start note` | Ornament-mode badge when starting a meend (web only — desktop shows in status bar) | TODO |
| `toolbar.ornament.murki` | `Murki: {count} notes (Enter to apply) [1 param]` | Ornament-mode badge for murki with note count (web only — desktop shows in status bar) | TODO |
| `toolbar.ornament.singleNote` | `Orn: {name} (type note) [1 param]` | Ornament-mode badge for single-note ornaments (kan, gamak, andolan) (web only — desktop shows in status bar) | TODO |
| `toolbar.ornament.zamzama` | `Zamzama: {count} notes (Enter to apply) [1 param]` | Ornament-mode badge for zamzama with note count (web only — desktop shows in status bar) | TODO |

### toolbar.script

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `toolbar.script.devanagari` | `Devanagari` | Devanagari script option in dropdown (web) | TODO |

### toolbar.section

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `toolbar.section.add.tooltip` | `Add Section` | Tooltip for add section button | TODO |
| `toolbar.section.remove.tooltip` | `Remove current section` | Tooltip for remove section button on web | TODO |
| `toolbar.section.rename.tooltip` | `Rename current section` | Tooltip for rename section button on web | TODO |

### toolbar.tabs

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `toolbar.tabs.close.tooltip` | `Close tab` | Tooltip for tab close button | TODO |
| `toolbar.tabs.new.tooltip` | `New Tab` | Tooltip for new tab button | TODO |

### toolbar.view

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `toolbar.view.toggleKeyboardLegend` | `Keys` | Button to toggle keyboard legend visibility | TODO |
| `toolbar.view.toggleKeyboardLegend.tooltip` | `Keyboard Shortcuts` | Tooltip for toggle keyboard legend button | TODO |
| `toolbar.view.toggleSahityaLine` | `Sahitya` | Button to toggle sahitya line visibility | TODO |
| `toolbar.view.toggleSahityaLine.tooltip` | `Toggle Sahitya Line` | Tooltip for toggle sahitya line button | TODO |
| `toolbar.view.toggleStrokeLine` | `Strokes` | Button to toggle stroke line visibility | TODO |
| `toolbar.view.toggleStrokeLine.tooltip` | `Toggle Stroke Line` | Tooltip for toggle stroke line button | TODO |

### view.loading

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
| `view.loading` | `Loading...` | Loading indicator text | TODO |


## Shared entries summary

54 shared entries. Full list omitted; query the catalog directly:
```bash
jq '.entries | to_entries[] | select(.value.platform=="both") | .key' \
  sangeet-core/src/main/resources/ui-strings.json
```
