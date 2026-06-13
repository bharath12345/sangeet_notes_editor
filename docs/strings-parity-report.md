# UI Strings Parity Report — Side-by-Side

> Generated: 2026-06-13. Regenerate with `make strings-report`.

## Summary

| Bucket                              | Count |
| ----------------------------------- | ----- |
| Shared (identical, hidden below)    | 54 |
| NORMALIZE candidates                | 0 |
| PORT→desk candidates                | 142 |
| PORT→web candidates                 | 166 |
| ACCEPT candidates                   | 217 |
| **Total asymmetric concepts**       | **525** |

## How to use this report

Walk through component tables. For each row, the **Suggest** column provides a heuristic default based on:

- **NORMALIZE** — Both platforms have the concept but with different wording; pick one to adopt.
- **PORT→desk** — Web has it, desktop doesn't, but the component exists on desktop; likely should be added.
- **PORT→web** — Desktop has it, web doesn't, but the component exists on web; likely should be added.
- **ACCEPT** — Platform-specific architectural difference; keep as-is.

These suggestions are **heuristics**, not authoritative. Override any suggestion by telling me the disposition you prefer (e.g., "for `dialog.about.title`, use NORMALIZE→'About Sangeet Notes Editor'" or "all `googleDrive.*` are ACCEPT").

Rows where Desktop and Web have identical values are hidden from this report — they're already symmetric.

## Components

### action.addSection  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| defaultName | (none) | New Section | ACCEPT |

### app.windowTitle  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| windowTitle | Sangeet Notes Editor | (none) | ACCEPT |

### appAction.addSection  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| addSection | Add section | (none) | ACCEPT |

### appAction.closeActiveTab  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| closeActiveTab | Close active tab | (none) | ACCEPT |

### appAction.cycleNotationScript  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| cycleNotationScript | Cycle notation script | (none) | ACCEPT |

### appAction.group  (2 entries)

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| sections | Sections | (none) | PORT→web |
| tabs | Tabs | (none) | PORT→web |

### appAction.nextTab  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| nextTab | Next tab | (none) | ACCEPT |

### appAction.openFolder  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| openFolder | Open folder | (none) | ACCEPT |

### appAction.openUserGuide  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| openUserGuide | Open user guide | (none) | ACCEPT |

### appAction.previousTab  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| previousTab | Previous tab | (none) | ACCEPT |

### appAction.redo  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| redo | (none) | Redo | ACCEPT |

### appAction.removeCurrentSection  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| removeCurrentSection | Remove current section | (none) | ACCEPT |

### appAction.renameCurrentSection  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| renameCurrentSection | Rename current section | (none) | ACCEPT |

### appAction.saveAs  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| saveAs | Save as | (none) | ACCEPT |

### appAction.supportProject  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| supportProject | (none) | Support the project | ACCEPT |

### appAction.toggleFileBrowser  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| toggleFileBrowser | Toggle file browser | (none) | ACCEPT |

### appAction.toggleKeyboardLegend  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| toggleKeyboardLegend | (none) | Toggle keyboard legend | ACCEPT |

### appAction.toggleSahityaLine  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| toggleSahityaLine | (none) | Toggle sahitya line | ACCEPT |

### appAction.toggleStrokeLine  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| toggleStrokeLine | (none) | Toggle stroke line | ACCEPT |

### appAction.toggleTheme  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| toggleTheme | Toggle light / dark theme | (none) | ACCEPT |

### appAction.undo  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| undo | (none) | Undo | ACCEPT |

### dialog.about  (23 entries)

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| betaNote | (none) | Beta release — actively iterating toward v1.0. Expect rou… | PORT→desk |
| betaNote.desktop | Beta release — actively iterating toward v1.0. Expect rou… | (none) | PORT→web |
| copyright | (none) | © 2026 Bharadwaj.  | PORT→desk |
| description.desktop.line1 | A notation editor for Hindustani classical music in the B… | (none) | PORT→web |
| description.desktop.line2 | Designed primarily for sitar compositions — Gat, Bandish,… | (none) | PORT→web |
| description.paragraph1 | (none) | A notation editor for Hindustani classical music in the B… | PORT→desk |
| description.paragraph2 | (none) | Supports Devanagari, Kannada, Telugu, and English scripts. | PORT→desk |
| license | (none) | Free and open source under the MIT License. | PORT→desk |
| license.desktop | Free and open source. Copyright (c) 2026 Bharadwaj. | (none) | PORT→web |
| links.header | (none) | Links | PORT→desk |
| links.selfHosting | (none) | Self-hosting guide | PORT→desk |
| links.userGuide | (none) | User guide | PORT→desk |
| links.userGuide.desktop | User guide & documentation | (none) | PORT→web |
| links.webVersion | Web version: {url} [1 param] | (none) | PORT→web |
| privacy.desktop | Anonymous usage stats (which features get touched, how lo… | (none) | PORT→web |
| privacy.header | (none) | Privacy | PORT→desk |
| privacy.text | (none) | While you use the app, anonymous usage events (clicks, ke… | PORT→desk |
| sampleToggle | Show sample composition on startup | (none) | PORT→web |
| support.link | (none) | Support the project | PORT→desk |
| support.suffix | (none) |  — UPI / PayPal options. | PORT→desk |
| support.text | (none) | 💖  | PORT→desk |
| tech | (none) | Desktop: Scala 3 + ScalaFX. Web: Elm + Tapir. | PORT→desk |
| tech.desktop | Built with Scala 3 + ScalaFX (desktop) and Elm + Tapir (web) | (none) | PORT→web |

### dialog.bugReport  (8 entries)

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| button.sentSuccess | Sent ✓ | (none) | PORT→web |
| disclosure.desktop | We'll include a short replay of recent keystrokes + a scr… | (none) | PORT→web |
| disclosure.web | (none) | We'll include a short replay of your recent actions in th… | PORT→desk |
| status.screenshotFailed | Screenshot failed ({error}) — sending without it. [1 param] | (none) | PORT→web |
| status.sendFailed | Send failed: {error} [1 param] | (none) | PORT→web |
| status.sending | Sending report... | (none) | PORT→web |
| status.sendThrew | Send threw: {message} [1 param] | (none) | PORT→web |
| status.sent | Sent. Report id: {reportId} [1 param] | (none) | PORT→web |

### dialog.commandPalette  (4 entries)

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| noResults | (none) | No matching actions. | PORT→desk |
| searchPlaceholder | Search actions… (Esc to close) | (none) | PORT→web |
| searchPlaceholderWeb | (none) | Search actions… (Esc to close, ↑↓ to navigate, Enter to run) | PORT→desk |
| title | Command Palette | (none) | PORT→web |

### dialog.crashRecovery  (12 entries)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| buttonDiscard | Discard | (none) | ACCEPT |
| buttonRetry | Retry send | (none) | ACCEPT |
| buttonSend | Send report | (none) | ACCEPT |
| descriptionLabel | Anything you remember doing right before? (optional) | (none) | ACCEPT |
| descriptionPlaceholder | Optional context — what tab was open, what you'd just typ… | (none) | ACCEPT |
| emailLabel | Email (optional, only if you want a reply) | (none) | ACCEPT |
| explanation | The app crashed during your last session. Sending a repor… | (none) | ACCEPT |
| stackTraceLabel | Stack trace: | (none) | ACCEPT |
| statusSending | Sending... | (none) | ACCEPT |
| statusSendingReport | Sending report... | (none) | ACCEPT |
| title | Sangeet didn't shut down cleanly last time | (none) | ACCEPT |
| windowTitle | Sangeet — crash recovery | (none) | ACCEPT |

### dialog.keyboardCheatSheet  (69 entries)

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| action.addSection | Add section | (none) | PORT→web |
| action.closeTab | Close tab | (none) | PORT→web |
| action.compositionProperties | Composition properties | (none) | PORT→web |
| action.copy | Copy | (none) | PORT→web |
| action.cut | Cut | (none) | PORT→web |
| action.cycleScript | Cycle notation script | (none) | PORT→web |
| action.exportHtml | Export HTML | (none) | PORT→web |
| action.newComposition | New composition | (none) | PORT→web |
| action.nextTab | Next tab | (none) | PORT→web |
| action.openFile | Open file | (none) | PORT→web |
| action.openFolder | Open folder | (none) | PORT→web |
| action.openUserGuide | Open user guide | (none) | PORT→web |
| action.paste | Paste | (none) | PORT→web |
| action.previousTab | Previous tab | (none) | PORT→web |
| action.redo | Redo | (none) | PORT→web |
| action.removeSection | Remove current section | (none) | PORT→web |
| action.renameSection | Rename current section | (none) | PORT→web |
| action.reportBug | Report a bug | (none) | PORT→web |
| action.save | Save | (none) | PORT→web |
| action.saveAs | Save as | (none) | PORT→web |
| action.showCheatSheet | Show this cheat sheet | (none) | PORT→web |
| action.toggleFileBrowser | Toggle file browser | (none) | PORT→web |
| action.toggleTheme | Toggle theme | (none) | PORT→web |
| action.undo | Undo | (none) | PORT→web |
| hint.desktopFull | (none) | -shortcuts wired (browsers reserve many of them on web). … | PORT→desk |
| hint.keyboardRef | (none) | Keyboard Reference | PORT→desk |
| hint.web | (none) | Tip: most toolbar actions are accessible via the buttons … | PORT→desk |
| label.cancelOrnament | (none) | Cancel ornament mode | PORT→desk |
| label.chikari | (none) | Chikari (open strings) | PORT→desk |
| label.cutCopyPaste | (none) | Cut / Copy / Paste | PORT→desk |
| label.daRaStrokes | (none) | Da (inward) / Ra (outward) | PORT→desk |
| label.deleteEvent | (none) | Delete event | PORT→desk |
| label.doubleTapDual | (none) | Double-tap dual swar | PORT→desk |
| label.extendSelection | (none) | Extend selection | PORT→desk |
| label.fastTyping | (none) | Type 2–4 notes within 500 ms to auto-group | PORT→desk |
| label.finishOrnament | (none) | Finish multi-note ornament | PORT→desk |
| label.gamakAndolan | (none) | Gamak / Andolan / Gitkari | PORT→desk |
| label.kanSwar | (none) | Kan swar | PORT→desk |
| label.komalRe | (none) | Komal Re / Ga / Dha / Ni | PORT→desk |
| label.madhyaDefault | (none) | Madhya (default) | PORT→desk |
| label.mandraLower | (none) | Mandra (lower) | PORT→desk |
| label.meendDown | (none) | Meend ↓ | PORT→desk |
| label.meendUp | (none) | Meend ↑ | PORT→desk |
| label.moveCursor | (none) | Move cursor one beat | PORT→desk |
| label.nextSubbeat | (none) | Next sub-beat | PORT→desk |
| label.rest | (none) | Rest | PORT→desk |
| label.setNotesPerBeat | (none) | Set notes per beat | PORT→desk |
| label.showCheatSheet | (none) | Show this cheat sheet | PORT→desk |
| label.shuddhaSwaras | (none) | Shuddha swaras | PORT→desk |
| label.sparsh | (none) | Sparsh | PORT→desk |
| label.sustain | (none) | Sustain | PORT→desk |
| label.taarUpper | (none) | Taar (upper) | PORT→desk |
| label.tivraMa | (none) | Tivra Ma | PORT→desk |
| label.undoRedo | (none) | Undo / Redo | PORT→desk |
| section.edit.desktop | Edit | (none) | PORT→web |
| section.file.desktop | File | (none) | PORT→web |
| section.help.desktop | Help | (none) | PORT→web |
| section.help.web | (none) | Help | PORT→desk |
| section.navigation | (none) | Navigation | PORT→desk |
| section.octave | (none) | Octave (saptak) | PORT→desk |
| section.ornaments | (none) | Ornaments | PORT→desk |
| section.sections.desktop | Sections | (none) | PORT→web |
| section.selectionClipboard | (none) | Selection & clipboard | PORT→desk |
| section.strokes | (none) | Strokes | PORT→desk |
| section.subdivisions | (none) | Subdivisions | PORT→desk |
| section.swar | (none) | Swar (notes) | PORT→desk |
| section.tabs.desktop | Tabs | (none) | PORT→web |
| section.view.desktop | View | (none) | PORT→web |
| subtitle.desktop | Full reference: Help → User Guide → Keyboard Reference | (none) | PORT→web |

### dialog.newComposition  (67 entries)

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| button.cancel | (none) | Cancel | PORT→desk |
| button.create | (none) | Create | PORT→desk |
| field.antaraStartingBeat.label | (none) | Antara Starting Beat (1-{matras}) [1 param] | PORT→desk |
| field.antaraStartingBeat.labelDesktop | Antara Starting Beat: | (none) | PORT→web |
| field.arohan.label | Arohan: | (none) | PORT→web |
| field.arohan.placeholder | auto-detected or enter manually | (none) | PORT→web |
| field.avrohan.label | Avrohan: | (none) | PORT→web |
| field.avrohan.placeholder | auto-detected or enter manually | (none) | PORT→web |
| field.filePath.browseButton | Browse... | (none) | PORT→web |
| field.filePath.browserTitle | Save Composition As | (none) | PORT→web |
| field.filePath.label | Save to: | (none) | PORT→web |
| field.filePath.placeholder | Select location to save .swar file | (none) | PORT→web |
| field.gatStartingBeat.label | (none) | Gat Starting Beat (1-{matras}) [1 param] | PORT→desk |
| field.gatStartingBeat.labelDesktop | Gat Starting Beat: | (none) | PORT→web |
| field.laya.atidrut | (none) | Ati-drut | PORT→desk |
| field.laya.atidrutDesktop | Ati-Drut | (none) | PORT→web |
| field.laya.ativilambit | (none) | Ati-vilambit | PORT→desk |
| field.laya.ativilambitDesktop | Ati-Vilambit | (none) | PORT→web |
| field.laya.label | (none) | Laya | PORT→desk |
| field.laya.labelDesktop | Laya: | (none) | PORT→web |
| field.laya.none | (none) | None (Palta) | PORT→desk |
| field.laya.noneDesktop | (none) | (none) | PORT→web |
| field.raag.label | (none) | Raag | PORT→desk |
| field.raag.labelDesktop | Raag: | (none) | PORT→web |
| field.raag.placeholder | Type to search or enter custom raag | (none) | PORT→web |
| field.samvadi.label | Samvadi: | (none) | PORT→web |
| field.samvadi.placeholder | auto-detected | (none) | PORT→web |
| field.script.label | Script: | (none) | PORT→web |
| field.showSahitya.checkboxDesktop | Show lyrics row below swar | (none) | PORT→web |
| field.showSahitya.label | (none) | Show Sahitya Line (Lyrics) | PORT→desk |
| field.showSahitya.labelDesktop | Sahitya line: | (none) | PORT→web |
| field.showStrokes.checkboxDesktop | Show Da/Ra stroke indicators below swar | (none) | PORT→web |
| field.showStrokes.label | (none) | Show Stroke Line (Da/Ra) | PORT→desk |
| field.showStrokes.labelDesktop | Stroke line: | (none) | PORT→web |
| field.sthayiStartingBeat.label | (none) | Sthayi Starting Beat (1-{matras}) [1 param] | PORT→desk |
| field.sthayiStartingBeat.labelDesktop | Sthayi Starting Beat: | (none) | PORT→web |
| field.taal.label | (none) | Taal | PORT→desk |
| field.taal.labelDesktop | Taal: | (none) | PORT→web |
| field.taanCount.label | (none) | Taan Count | PORT→desk |
| field.taanCount.labelDesktop | Taans: | (none) | PORT→web |
| field.taanStartingBeat.label | (none) | Taan Starting Beat (1-{matras}) [1 param] | PORT→desk |
| field.taanStartingBeat.labelDesktop | Taan Starting Beat: | (none) | PORT→web |
| field.thaat.label | Thaat: | (none) | PORT→web |
| field.thaat.placeholder | auto-detected or enter manually | (none) | PORT→web |
| field.title.label | (none) | Title | PORT→desk |
| field.title.labelDesktop | Title: | (none) | PORT→web |
| field.title.placeholder | (none) | Enter composition title | PORT→desk |
| field.title.placeholderDesktop | e.g. Yaman Vilambit Gat | (none) | PORT→web |
| field.type.bandish | (none) | Bandish (Vocal) | PORT→desk |
| field.type.bandishDesktop | Bandish | (none) | PORT→web |
| field.type.gat | (none) | Gat (Instrumental) | PORT→desk |
| field.type.gatDesktop | Gat | (none) | PORT→web |
| field.type.label | (none) | Type | PORT→desk |
| field.type.labelDesktop | Type: | (none) | PORT→web |
| field.type.palta | (none) | Palta (Practice) | PORT→desk |
| field.type.paltaDesktop | Palta | (none) | PORT→web |
| field.type.sargam | (none) | Sargam (Practice) | PORT→desk |
| field.type.sargamDesktop | Sargam | (none) | PORT→web |
| field.vadi.label | Vadi: | (none) | PORT→web |
| field.vadi.placeholder | auto-detected | (none) | PORT→web |
| header | Create a new composition | (none) | PORT→web |
| raagDetected | Raag {name} recognized [1 param] | (none) | PORT→web |
| raagNotFound | (raag not in database -- enter details manually) | (none) | PORT→web |
| validation.filePathRequired | File path is required | (none) | PORT→web |
| validation.layaRequired | Laya is required for Gat | (none) | PORT→web |
| validation.raagRequired | Raag is required | (none) | PORT→web |
| validation.titleRequired | Title is required | (none) | PORT→web |

### dialog.properties  (16 entries)

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| button.cancel | (none) | Cancel | PORT→desk |
| button.save | (none) | Save | PORT→desk |
| field.antaraStartingBeat.labelDesktop | Antara Starting Beat: | (none) | PORT→web |
| field.gatStartingBeat.labelDesktop | Gat Starting Beat: | (none) | PORT→web |
| field.raag.label | Raag: | (none) | PORT→web |
| field.sectionStartingBeat.label | (none) | {name} Starting Beat (1-{matras}) [2 params] | PORT→desk |
| field.sthayiStartingBeat.labelDesktop | Sthayi Starting Beat: | (none) | PORT→web |
| field.taal.label | (none) | Taal | PORT→desk |
| field.taal.labelDesktop | Taal: | (none) | PORT→web |
| field.taanStartingBeat.labelDesktop | Taan Starting Beat: | (none) | PORT→web |
| field.title.label | (none) | Title | PORT→desk |
| field.title.labelDesktop | Title: | (none) | PORT→web |
| field.title.placeholder | (none) | Composition title | PORT→desk |
| field.type.label | Type: | (none) | PORT→web |
| header | Edit composition details | (none) | PORT→web |
| validation.beatsClamped | Starting beats clamped to new taal range (1-{matras}) [1 … | (none) | PORT→web |

### dialog.support  (8 entries)

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| international.paypalLink | (none) | Support via PayPal | PORT→desk |
| international.platformLink | Support via {platform} [1 param] | (none) | PORT→web |
| upi.handle | (none) | bharath12345-1@oksbi | PORT→desk |
| upi.handleLabel | (none) | UPI handle:  | PORT→desk |
| upi.handleLabelWithValue | UPI handle: {handle} [1 param] | (none) | PORT→web |
| upi.qrAlt | (none) | UPI QR code | PORT→desk |
| upi.qrPlaceholder | (QR code image will appear here) | (none) | PORT→web |
| windowTitle | Support — Sangeet Notes Editor | (none) | PORT→web |

### editor.sampleWarning  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| sampleWarning | This is a read-only sample showing Yaman Vilambit Gat. | (none) | ACCEPT |

### fileBrowser.addFolderDialogTitle  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| addFolderDialogTitle | Add Folder | (none) | ACCEPT |

### fileBrowser.addFolderTooltip  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| addFolderTooltip | Add a folder | (none) | ACCEPT |

### fileBrowser.bookmarkTooltip  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| bookmarkTooltip | (none) | Bookmark | ACCEPT |

### fileBrowser.connectDrive  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| connectDrive | (none) | Connect Google Drive | ACCEPT |

### fileBrowser.connecting  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| connecting | (none) | Connecting... | ACCEPT |

### fileBrowser.deleteDialogPrompt  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| deleteDialogPrompt | Delete {filename}? | (none) | ACCEPT |

### fileBrowser.deleteDialogTitle  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| deleteDialogTitle | Delete File | (none) | ACCEPT |

### fileBrowser.deleteDialogWarning  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| deleteDialogWarning | This action cannot be undone. | (none) | ACCEPT |

### fileBrowser.deleteTooltip  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| deleteTooltip | (none) | Delete | ACCEPT |

### fileBrowser.driveConnected  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| driveConnected | (none) | Drive connected | ACCEPT |

### fileBrowser.emptyState  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| emptyState | (none) | Connect Drive to browse files | ACCEPT |

### fileBrowser.errorFileExists  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| errorFileExists | File already exists: {name} | (none) | ACCEPT |

### fileBrowser.errorFolderExists  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| errorFolderExists | Folder already exists: {name} | (none) | ACCEPT |

### fileBrowser.errorFolderOpen  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| errorFolderOpen | Folder already open: {name} | (none) | ACCEPT |

### fileBrowser.errorMoveExists  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| errorMoveExists | A file named {name} already exists in the destination | (none) | ACCEPT |

### fileBrowser.errorNotDirectory  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| errorNotDirectory | Not a directory: {path} | (none) | ACCEPT |

### fileBrowser.errorRenameExists  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| errorRenameExists | A file with that name already exists | (none) | ACCEPT |

### fileBrowser.headerLabel  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| headerLabel | FILES | (none) | ACCEPT |

### fileBrowser.hideFilesTooltip  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| hideFilesTooltip | (none) | Hide Files | ACCEPT |

### fileBrowser.logAddedFolder  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| logAddedFolder | Added folder: {name} | (none) | ACCEPT |

### fileBrowser.logBookmarked  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| logBookmarked | Bookmarked: {name} | (none) | ACCEPT |

### fileBrowser.logCreatedFile  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| logCreatedFile | Created: {name} | (none) | ACCEPT |

### fileBrowser.logCreatedFolder  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| logCreatedFolder | Created folder: {name} | (none) | ACCEPT |

### fileBrowser.logDeleted  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| logDeleted | Deleted: {name} | (none) | ACCEPT |

### fileBrowser.logMoved  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| logMoved | Moved: {name} -> {dest} | (none) | ACCEPT |

### fileBrowser.logRemovedBookmark  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| logRemovedBookmark | Removed bookmark: {name} | (none) | ACCEPT |

### fileBrowser.logRemovedFolder  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| logRemovedFolder | Removed folder: {name} | (none) | ACCEPT |

### fileBrowser.logRenamed  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| logRenamed | Renamed: {old} -> {new} | (none) | ACCEPT |

### fileBrowser.menuBookmark  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| menuBookmark | Bookmark | (none) | ACCEPT |

### fileBrowser.menuDelete  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| menuDelete | Delete | (none) | ACCEPT |

### fileBrowser.menuMoveTo  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| menuMoveTo | Move to... | (none) | ACCEPT |

### fileBrowser.menuNewFile  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| menuNewFile | New .swar File | (none) | ACCEPT |

### fileBrowser.menuNewFolder  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| menuNewFolder | New Folder | (none) | ACCEPT |

### fileBrowser.menuOpen  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| menuOpen | Open | (none) | ACCEPT |

### fileBrowser.menuRefresh  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| menuRefresh | Refresh | (none) | ACCEPT |

### fileBrowser.menuRemoveBookmark  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| menuRemoveBookmark | Remove Bookmark | (none) | ACCEPT |

### fileBrowser.menuRemoveFromBrowser  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| menuRemoveFromBrowser | Remove from Browser | (none) | ACCEPT |

### fileBrowser.menuRename  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| menuRename | Rename | (none) | ACCEPT |

### fileBrowser.moveToDialogTitle  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| moveToDialogTitle | Move to... | (none) | ACCEPT |

### fileBrowser.newFileDialogPrompt  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| newFileDialogPrompt | Enter filename (without .swar extension) | (none) | ACCEPT |

### fileBrowser.newFileDialogTitle  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| newFileDialogTitle | New Composition File | (none) | ACCEPT |

### fileBrowser.newFolder  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| defaultName | (none) | New Folder | ACCEPT |

### fileBrowser.newFolderDialogPrompt  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| newFolderDialogPrompt | Enter folder name | (none) | ACCEPT |

### fileBrowser.newFolderDialogTitle  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| newFolderDialogTitle | New Folder | (none) | ACCEPT |

### fileBrowser.refreshTooltip  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| refreshTooltip | (none) | Refresh | ACCEPT |

### fileBrowser.removeBookmarkTooltip  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| removeBookmarkTooltip | (none) | Remove bookmark | ACCEPT |

### fileBrowser.renameDialogPrompt  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| renameDialogPrompt | Enter new name | (none) | ACCEPT |

### fileBrowser.renameDialogTitle  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| renameDialogTitle | Rename | (none) | ACCEPT |

### fileBrowser.showFilesTooltip  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| showFilesTooltip | (none) | Show Files | ACCEPT |

### header.arohanLabel  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| arohanLabel | Arohan | (none) | ACCEPT |

### header.avrohanLabel  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| avrohanLabel | Avrohan | (none) | ACCEPT |

### header.beatPrefix  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| beatPrefix | (none) | Beat  | ACCEPT |

### header.cyclePrefix  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| cyclePrefix | (none) | Cycle  | ACCEPT |

### header.layaLabel  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| layaLabel | Laya | (none) | ACCEPT |

### header.modeLabel  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| modeLabel | (none) | Mode:  | ACCEPT |

### header.modeStroke  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| modeStroke | (none) | Stroke | ACCEPT |

### header.modeSwar  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| modeSwar | (none) | Swar | ACCEPT |

### header.octaveAtiMandra  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| octaveAtiMandra | (none) | Ati-Mandra | ACCEPT |

### header.octaveAtiTaar  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| octaveAtiTaar | (none) | Ati-Taar | ACCEPT |

### header.octaveLabel  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| octaveLabel | (none) | Octave:  | ACCEPT |

### header.octaveMadhya  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| octaveMadhya | (none) | Madhya | ACCEPT |

### header.octaveMandra  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| octaveMandra | (none) | Mandra | ACCEPT |

### header.octaveTaar  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| octaveTaar | (none) | Taar | ACCEPT |

### header.raagLabel  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| raagLabel | Raag | (none) | ACCEPT |

### header.samvadiLabel  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| samvadiLabel | Samvadi | (none) | ACCEPT |

### header.subPrefix  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| subPrefix | (none) | Sub  | ACCEPT |

### header.taalLabel  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| taalLabel | Taal | (none) | ACCEPT |

### header.thaatLabel  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| thaatLabel | Thaat | (none) | ACCEPT |

### header.vadiLabel  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| vadiLabel | Vadi | (none) | ACCEPT |

### keyboardLegend.nav  (6 entries)

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| backspace.web | (none) | Delete last | PORT→desk |
| enter | Next cycle | (none) | PORT→web |
| moveCursor | Move cursor | (none) | PORT→web |
| prevNextBeat.web | (none) | Previous / Next beat | PORT→desk |
| tab.desktop | Next beat | (none) | PORT→web |
| tab.web | (none) | Next sub-beat | PORT→desk |

### keyboardLegend.octave  (6 entries)

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| backToMadhya | Back to madhya | (none) | PORT→web |
| madhya.web | (none) | Madhya (middle) | PORT→desk |
| mandra.desktop | Next note in mandra | (none) | PORT→web |
| mandra.web | (none) | Mandra (lower) | PORT→desk |
| taar.desktop | Next note in taar | (none) | PORT→web |
| taar.web | (none) | Taar (upper) | PORT→desk |

### keyboardLegend.ornamentKeys  (3 entries)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| multiNote | ..↵ = type notes, press Enter | (none) | ACCEPT |
| oneNote | ♪  = type one swar key | (none) | ACCEPT |
| twoNotes | ♪♪ = type start, then end note | (none) | ACCEPT |

### keyboardLegend.ornaments  (22 entries)

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| andolan | (none) | Andolan | PORT→desk |
| andolan.desktop | Andolan (gentle oscillation) | (none) | PORT→web |
| gamak | (none) | Gamak | PORT→desk |
| gamak.desktop | Gamak (heavy oscillation) | (none) | PORT→web |
| ghaseet | (none) | Ghaseet (then type note) | PORT→desk |
| ghaseet.desktop | Ghaseet (heavy pull) | (none) | PORT→web |
| gitkari | (none) | Gitkari | PORT→desk |
| gitkari.desktop | Gitkari (hammer/pull trill) | (none) | PORT→web |
| kan | (none) | Kan Swar (then type note) | PORT→desk |
| kan.desktop | Kan Swar (grace note) | (none) | PORT→web |
| krintan.desktop | Krintan (pull-off seq.) | (none) | PORT→web |
| krintan.web | (none) | Krintan (type notes, Enter) | PORT→desk |
| meendAsc | (none) | Meend Asc (type start, end) | PORT→desk |
| meendAsc.desktop | Meend ↑ (ascending glide) | (none) | PORT→web |
| meendDesc | (none) | Meend Desc | PORT→desk |
| meendDesc.desktop | Meend ↓ (descending glide) | (none) | PORT→web |
| murki | (none) | Murki (type notes, Enter) | PORT→desk |
| murki.desktop | Murki (ornamental turn) | (none) | PORT→web |
| sparsh | (none) | Sparsh (then type note) | PORT→desk |
| sparsh.desktop | Sparsh (light touch) | (none) | PORT→web |
| zamzama | (none) | Zamzama (type notes, Enter) | PORT→desk |
| zamzama.desktop | Zamzama (rapid cluster) | (none) | PORT→web |

### keyboardLegend.redo  (2 entries)

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| desktop | Redo | (none) | PORT→web |
| web | (none) | Redo | PORT→desk |

### keyboardLegend.scriptLabel  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| scriptLabel | Script: {scriptName} | (none) | ACCEPT |

### keyboardLegend.section  (14 entries)

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| ornamentKeys | Ornament Keys | (none) | PORT→web |
| ornaments | (none) | Ornaments (Alt+key) | PORT→desk |
| ornamentsMultiNote | Ornaments -- Multi-Note | (none) | PORT→web |
| ornamentsOneNote | Ornaments -- One Note | (none) | PORT→web |
| ornamentsSimple | Ornaments -- Simple | (none) | PORT→web |
| ornamentsTwoNotes | Ornaments -- Two Notes | (none) | PORT→web |
| strokes | (none) | Strokes | PORT→desk |
| strokesMizrab | Strokes (Mizrab) | (none) | PORT→web |
| subdivisions | Subdivisions | (none) | PORT→web |
| swarInput | (none) | Swar Input | PORT→desk |
| swarNotes | Swar (Notes) | (none) | PORT→web |
| tips | Tips | (none) | PORT→web |
| undoRedo | (none) | Undo/Redo | PORT→desk |
| undoRedoDesktop | Undo / Redo | (none) | PORT→web |

### keyboardLegend.special  (4 entries)

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| deleteLast | Delete last note | (none) | PORT→web |
| rest | Rest (silence) | (none) | PORT→web |
| subdivisions | (none) | Set subdivisions per beat | PORT→desk |
| sustain | Sustain (hold) | (none) | PORT→web |

### keyboardLegend.strokes  (4 entries)

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| da | Da (inward stroke) | (none) | PORT→web |
| keys.web | (none) | Da / Ra / Jod (in stroke mode) | PORT→desk |
| ra | Ra (outward stroke) | (none) | PORT→web |
| toggleMode.web | (none) | Toggle Swar/Stroke mode | PORT→desk |

### keyboardLegend.subdivisions  (2 entries)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| doubleTap | Double-tap for dual swar | (none) | ACCEPT |
| setPerBeat | Set notes per beat (2-8) | (none) | ACCEPT |

### keyboardLegend.swar  (6 entries)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| dualSwar | (none) | Dual swar (double-tap) | ACCEPT |
| komal | (none) | Komal variants | ACCEPT |
| rest | (none) | Rest | ACCEPT |
| shuddha | (none) | Shuddha notes | ACCEPT |
| sustain | (none) | Sustain | ACCEPT |
| tivraMa | (none) | Tivra Ma | ACCEPT |

### keyboardLegend.tips  (3 entries)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| applyToLast | Strokes & ornaments apply to the last entered note | (none) | ACCEPT |
| octaveReset | . and ' affect only the next note, then reset to madhya | (none) | ACCEPT |
| shiftVariant | Shift = komal/tivra variant | (none) | ACCEPT |

### keyboardLegend.title  (2 entries)

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| desktop | Keyboard Reference | (none) | PORT→web |
| web | (none) | Keyboard Shortcuts | PORT→desk |

### mainApp.openFolderDialogTitle  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| openFolderDialogTitle | Open Folder | (none) | ACCEPT |

### status.apiError  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| apiError | (none) | API error: {message} | ACCEPT |

### status.badBody  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| badBody | (none) | Bad body: {error} | ACCEPT |

### status.badStatus  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| badStatus | (none) | Bad status: {code} | ACCEPT |

### status.badUrl  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| badUrl | (none) | Bad URL: {url} | ACCEPT |

### status.bugReportFailed  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| bugReportFailed | (none) | Bug report failed: {message} | ACCEPT |

### status.bugReportSent  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| bugReportSent | (none) | Bug report sent — thanks! ({message}) | ACCEPT |

### status.clipboardEmpty  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| clipboardEmpty | Clipboard is empty | (none) | ACCEPT |

### status.clipboardNotSangeetData  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| clipboardNotSangeetData | Clipboard does not contain Sangeet data | (none) | ACCEPT |

### status.closedTabSwitched  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| closedTabSwitched | (none) | Closed tab, switched to {filename} | ACCEPT |

### status.colorsLoaded  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| colorsLoaded | (none) | Colors loaded | ACCEPT |

### status.connectedToDrive  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| connectedToDrive | (none) | Connected to Google Drive | ACCEPT |

### status.copiedEvents  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| copiedEvents | Copied {count} event(s) | (none) | ACCEPT |

### status.created  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| created | (none) | Created: {title} | ACCEPT |

### status.cursorPlaced  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| cursorPlaced | Cursor placed at cycle {cycle}, beat {beat} | (none) | ACCEPT |

### status.cutEvents  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| cutEvents | Cut {count} event(s) | (none) | ACCEPT |

### status.driveAuthFailed  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| driveAuthFailed | (none) | Drive authentication failed | ACCEPT |

### status.driveError  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| driveError | (none) | Drive error: {message} | ACCEPT |

### status.errorOpeningFile  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| errorOpeningFile | Error opening file: {message} | (none) | ACCEPT |

### status.errorOpeningHtml  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| errorOpeningHtml | Error opening HTML: {message} | (none) | ACCEPT |

### status.errorReloading  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| errorReloading | Error reloading: {message} | (none) | ACCEPT |

### status.exportingHtml  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| exportingHtml | (none) | Exporting HTML... | ACCEPT |

### status.failedToParseDriveFileContent  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| failedToParseDriveFileContent | (none) | Failed to parse Drive file content | ACCEPT |

### status.failedToParseDriveFolderListing  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| failedToParseDriveFolderListing | (none) | Failed to parse Drive folder listing | ACCEPT |

### status.fileSavedToDrive  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| fileSavedToDrive | (none) | File saved to Drive | ACCEPT |

### status.fileSelected  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| fileSelected | (none) | File selected: {filename} | ACCEPT |

### status.fileWasDeleted  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| fileWasDeleted | File was deleted: {title} | (none) | ACCEPT |

### status.httpError  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| httpError | (none) | HTTP error: {message} | ACCEPT |

### status.lastTabClosedNewCreated  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| lastTabClosedNewCreated | (none) | Last tab closed — new tab created | ACCEPT |

### status.loadedRaags  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| loadedRaags | (none) | Loaded {count} raags | ACCEPT |

### status.loadedTaals  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| loadedTaals | (none) | Loaded {count} taals | ACCEPT |

### status.loadingFileFromDrive  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| loadingFileFromDrive | (none) | Loading file from Drive: {filename} | ACCEPT |

### status.networkError  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| networkError | (none) | Network error | ACCEPT |

### status.newTab  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| newTab | (none) | New tab | ACCEPT |

### status.noEventsInSelection  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| noEventsInSelection | No events in selection | (none) | ACCEPT |

### status.noSelection  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| noSelection | No selection | (none) | ACCEPT |

### status.noSelectionToCopy  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| noSelectionToCopy | (none) | No selection to copy | ACCEPT |

### status.noSelectionToCut  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| noSelectionToCut | (none) | No selection to cut | ACCEPT |

### status.nothingToRedo  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| nothingToRedo | (none) | Nothing to redo | ACCEPT |

### status.nothingToUndo  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| nothingToUndo | (none) | Nothing to undo | ACCEPT |

### status.opened  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| opened | (none) | Opened: {title} | ACCEPT |

### status.openedDesktop  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| openedDesktop | Opened: {filename} | (none) | ACCEPT |

### status.openingFromDrive  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| openingFromDrive | (none) | Opening from Drive: {filename} | ACCEPT |

### status.ornamentCancelled  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| ornamentCancelled | (none) | Ornament mode cancelled | ACCEPT |

### status.ornamentCollecting  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| ornamentCollecting | (none) | Collecting ornament notes... | ACCEPT |

### status.ornamentGhaseet  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| ornamentGhaseet | (none) | Ghaseet: type the target note | ACCEPT |

### status.ornamentKanSwar  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| ornamentKanSwar | (none) | Kan Swar: type the grace note | ACCEPT |

### status.ornamentKrintan  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| ornamentKrintan | (none) | Krintan: type notes, then Enter | ACCEPT |

### status.ornamentMeendAsc  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| ornamentMeendAsc | (none) | Meend (ascending): type start note | ACCEPT |

### status.ornamentMeendDesc  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| ornamentMeendDesc | (none) | Meend (descending): type start note | ACCEPT |

### status.ornamentMurki  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| ornamentMurki | (none) | Murki: type notes, then Enter | ACCEPT |

### status.ornamentSparsh  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| ornamentSparsh | (none) | Sparsh: type the touch note | ACCEPT |

### status.ornamentZamzama  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| ornamentZamzama | (none) | Zamzama: type notes, then Enter | ACCEPT |

### status.pastedEvents  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| pastedEvents | Pasted {count} event(s) | (none) | ACCEPT |

### status.pleaseSelectValidTaalRaag  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| pleaseSelectValidTaalRaag | (none) | Please select a valid taal and raag | ACCEPT |

### status.preview  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| preview | Preview: {filename} | (none) | ACCEPT |

### status.propertiesUpdatedTaal  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| propertiesUpdatedTaal | (none) | Properties updated — taal: {taalName} | ACCEPT |

### status.propertiesUpdatedTaalNotFound  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| propertiesUpdatedTaalNotFound | (none) | Properties updated (taal not found, kept previous) | ACCEPT |

### status.redo  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| redo | (none) | Redo | ACCEPT |

### status.reloaded  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| reloaded | Reloaded: {filename} | (none) | ACCEPT |

### status.requestTimeout  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| requestTimeout | (none) | Request timed out | ACCEPT |

### status.sahityaLineHidden  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| sahityaLineHidden | (none) | Sahitya line hidden | ACCEPT |

### status.sahityaLineShown  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| sahityaLineShown | (none) | Sahitya line shown | ACCEPT |

### status.sampleDismissed  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| sampleDismissed | Sample dismissed — won't appear on next launch | (none) | ACCEPT |

### status.sampleLoaded  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| sampleLoaded | Uneditable sample loaded | (none) | ACCEPT |

### status.samplePrompt  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| samplePrompt | To start, click New to create a composition | (none) | ACCEPT |

### status.savingComposition  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| savingComposition | (none) | Saving composition... | ACCEPT |

### status.scriptChanged  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| scriptChanged | (none) | Script changed to {scriptName} | ACCEPT |

### status.sectionAdded  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| sectionAdded | (none) | Section added | ACCEPT |

### status.sectionRemoved  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| sectionRemoved | (none) | Section removed | ACCEPT |

### status.sectionRenamed  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| sectionRenamed | (none) | Section renamed | ACCEPT |

### status.sectionsReordered  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| sectionsReordered | (none) | Sections reordered | ACCEPT |

### status.startingBeatsUpdated  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| startingBeatsUpdated | (none) | Starting beats updated | ACCEPT |

### status.strokeLineHidden  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| strokeLineHidden | (none) | Stroke line hidden | ACCEPT |

### status.strokeLineShown  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| strokeLineShown | (none) | Stroke line shown | ACCEPT |

### status.switchedToSection  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| switchedToSection | (none) | Switched to section {number} | ACCEPT |

### status.switchedToSectionDesktop  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| switchedToSectionDesktop | Switched to section: {name} | (none) | ACCEPT |

### status.switchedToTab  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| switchedToTab | (none) | Switched to {filename} | ACCEPT |

### status.tabClosed  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| tabClosed | (none) | Tab closed | ACCEPT |

### status.undo  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| undo | (none) | Undo | ACCEPT |

### statusBar.logLabel  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| logLabel | Log | (none) | ACCEPT |

### toolbar.edit  (6 entries)

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| redo | (none) | Redo | PORT→desk |
| redo.tooltip | (none) | Redo (Ctrl+Y) | PORT→desk |
| redo.tooltip.desktop | Redo (Ctrl+Shift+Z) | (none) | PORT→web |
| undo | (none) | Undo | PORT→desk |
| undo.tooltip | (none) | Undo (Ctrl+Z) | PORT→desk |
| undo.tooltip.desktop | Undo last edit (Ctrl+Z) | (none) | PORT→web |

### toolbar.file  (20 entries)

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| copy.tooltip | (none) | Copy (Ctrl+C) | PORT→desk |
| copy.tooltip.desktop | Copy selected events (Ctrl+C) | (none) | PORT→web |
| cut.tooltip | (none) | Cut (Ctrl+X) | PORT→desk |
| cut.tooltip.desktop | Cut selected events (Ctrl+X) | (none) | PORT→web |
| exportHtml | (none) | HTML | PORT→desk |
| exportHtml.tooltip | (none) | Export HTML | PORT→desk |
| exportHtml.tooltip.desktop | Export composition as HTML | (none) | PORT→web |
| new | (none) | New | PORT→desk |
| new.tooltip | (none) | New Composition (Ctrl+N) | PORT→desk |
| new.tooltip.desktop | Create a new composition | (none) | PORT→web |
| open | (none) | Open | PORT→desk |
| open.tooltip | (none) | Open File | PORT→desk |
| open.tooltip.desktop | Open a .swar file | (none) | PORT→web |
| openFolder.tooltip | Open a folder in the file browser | (none) | PORT→web |
| paste.tooltip | (none) | Paste (Ctrl+V) | PORT→desk |
| paste.tooltip.desktop | Paste clipboard events (Ctrl+V) | (none) | PORT→web |
| save | (none) | Save | PORT→desk |
| save.tooltip | (none) | Save File (Ctrl+S) | PORT→desk |
| save.tooltip.desktop | Save composition to current file | (none) | PORT→web |
| saveAs.tooltip | Save composition as a new .swar file | (none) | PORT→web |

### toolbar.help  (16 entries)

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| about | (none) | About | PORT→desk |
| about.tooltip | (none) | About | PORT→desk |
| about.tooltip.desktop | About Sangeet Notes Editor | (none) | PORT→web |
| keyboardShortcuts | (none) | ? | PORT→desk |
| keyboardShortcuts.tooltip | (none) | Keyboard shortcuts (?) | PORT→desk |
| keyboardShortcuts.tooltip.desktop | Show keyboard shortcuts (?) | (none) | PORT→web |
| properties | (none) | Properties | PORT→desk |
| properties.tooltip | (none) | Composition Properties | PORT→desk |
| properties.tooltip.desktop | Edit composition metadata | (none) | PORT→web |
| reportBug | (none) | 🐞 Report bug | PORT→desk |
| reportBug.tooltip | (none) | Report a bug — includes a short replay so it can be repro… | PORT→desk |
| reportBug.tooltip.desktop | Report a bug — includes a screenshot + recent keystrokes … | (none) | PORT→web |
| support | (none) | 💖 | PORT→desk |
| support.tooltip | (none) | Support the project — donate via UPI or PayPal | PORT→desk |
| support.tooltip.desktop | Support the project | (none) | PORT→web |
| userGuide.tooltip | Open the user guide (F1) | (none) | PORT→web |

### toolbar.mode  (2 entries)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| stroke | (none) | Mode: Stroke | ACCEPT |
| swar | (none) | Mode: Swar | ACCEPT |

### toolbar.ornament  (7 entries)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| krintanEnd | (none) | Krintan: type end note / Enter | ACCEPT |
| krintanStart | (none) | Krintan: type start note | ACCEPT |
| meendEnd | (none) | Meend: type end note | ACCEPT |
| meendStart | (none) | Meend: type start note | ACCEPT |
| murki | (none) | Murki: {count} notes (Enter to apply) [1 param] | ACCEPT |
| singleNote | (none) | Orn: {name} (type note) [1 param] | ACCEPT |
| zamzama | (none) | Zamzama: {count} notes (Enter to apply) [1 param] | ACCEPT |

### toolbar.script  (3 entries)

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| devanagari | (none) | Devanagari | PORT→desk |
| devanagari.desktop | Devanagari (Hindi) | (none) | PORT→web |
| tooltip | Change notation script | (none) | PORT→web |

### toolbar.section  (6 entries)

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| add.tooltip | (none) | Add Section | PORT→desk |
| add.tooltip.desktop | Add a new section to the composition | (none) | PORT→web |
| remove.tooltip | (none) | Remove current section | PORT→desk |
| remove.tooltip.desktop | Remove the current section | (none) | PORT→web |
| rename.tooltip | (none) | Rename current section | PORT→desk |
| rename.tooltip.desktop | Rename the current section (F2) | (none) | PORT→web |

### toolbar.tabs  (2 entries)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| close.tooltip | (none) | Close tab | ACCEPT |
| new.tooltip | (none) | New Tab | ACCEPT |

### toolbar.theme  (1 entry)

*(All entries in this component are desktop-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| toggle.tooltip | Toggle light / dark theme | (none) | ACCEPT |

### toolbar.view  (6 entries)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| toggleKeyboardLegend | (none) | Keys | ACCEPT |
| toggleKeyboardLegend.tooltip | (none) | Keyboard Shortcuts | ACCEPT |
| toggleSahityaLine | (none) | Sahitya | ACCEPT |
| toggleSahityaLine.tooltip | (none) | Toggle Sahitya Line | ACCEPT |
| toggleStrokeLine | (none) | Strokes | ACCEPT |
| toggleStrokeLine.tooltip | (none) | Toggle Stroke Line | ACCEPT |

### view.loading  (1 entry)

*(All entries in this component are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest |
| ------- | ------- | --- | ------- |
| loading | (none) | Loading... | ACCEPT |

