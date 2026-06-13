# UI Strings Parity Report — Side-by-Side

> Generated: 2026-06-13. Regenerate with `make strings-report`.

## Summary

| Bucket                              | Count |
| ----------------------------------- | ----- |
| Shared (identical, hidden below)    | 65 |
| NORMALIZE candidates                | 0 |
| PORT→desk candidates                | 0 |
| PORT→web candidates                 | 189 |
| ACCEPT candidates                   | 314 |
| **Total asymmetric concepts**       | **503** |
| Status: DONE                        | 69 |
| Status: PENDING                     | 434 |

## How to use this report

Each row in the per-component tables shows:

- **Suggest** — the authoritative disposition for this entry.
  - When set explicitly on the catalog entry (`disposition` field), the explicit value is used.
  - When the heuristic and the explicit disposition disagree, the cell reads `<explicit> (override)`.
  - When no explicit disposition is set, the cell shows the heuristic guess.
- **Status** — `DONE` when the entry has a `dispositionNote` (i.e., the port has landed
  or the decision has been recorded); `PENDING` otherwise.

Disposition vocabulary:

- **NORMALIZE** — Both platforms have the concept but with different wording; pick one to adopt.
- **PORT→desk** — Web has it, desktop doesn't, but the component exists on desktop; should be added.
- **PORT→web** — Desktop has it, web doesn't, but the component exists on web; should be added.
- **ACCEPT** — Platform-specific architectural difference; keep as-is.

Rows where Desktop and Web have identical values are hidden from this report — they're already symmetric.

## Components

### action.addSection  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| defaultName | (none) | New Section | ACCEPT | PENDING |

### app.windowTitle  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| windowTitle | Sangeet Notes Editor | (none) | ACCEPT (override) | DONE |

### appAction.addSection  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| addSection | Add section | (none) | PORT→web | PENDING |

### appAction.closeActiveTab  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| closeActiveTab | Close active tab | (none) | PORT→web | PENDING |

### appAction.cycleNotationScript  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| cycleNotationScript | Cycle notation script | (none) | PORT→web | PENDING |

### appAction.group  (2 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| sections | Sections | (none) | PORT→web | PENDING |
| tabs | Tabs | (none) | PORT→web | PENDING |

### appAction.nextTab  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| nextTab | Next tab | (none) | PORT→web | PENDING |

### appAction.openFolder  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| openFolder | Open folder | (none) | ACCEPT (override) | DONE |

### appAction.openUserGuide  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| openUserGuide | Open user guide | (none) | PORT→web | PENDING |

### appAction.previousTab  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| previousTab | Previous tab | (none) | PORT→web | PENDING |

### appAction.redo  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| redo | (none) | Redo | ACCEPT | PENDING |

### appAction.removeCurrentSection  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| removeCurrentSection | Remove current section | (none) | PORT→web | PENDING |

### appAction.renameCurrentSection  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| renameCurrentSection | Rename current section | (none) | PORT→web | PENDING |

### appAction.saveAs  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| saveAs | Save as | (none) | PORT→web | PENDING |

### appAction.supportProject  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| supportProject | (none) | Support the project | ACCEPT | PENDING |

### appAction.toggleFileBrowser  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| toggleFileBrowser | Toggle file browser | (none) | ACCEPT (override) | DONE |

### appAction.toggleKeyboardLegend  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| toggleKeyboardLegend | (none) | Toggle keyboard legend | ACCEPT | PENDING |

### appAction.toggleSahityaLine  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| toggleSahityaLine | (none) | Toggle sahitya line | ACCEPT | PENDING |

### appAction.toggleStrokeLine  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| toggleStrokeLine | (none) | Toggle stroke line | ACCEPT | PENDING |

### appAction.toggleTheme  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| toggleTheme | Toggle light / dark theme | (none) | PORT→web | PENDING |

### appAction.undo  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| undo | (none) | Undo | ACCEPT | PENDING |

### dialog.about  (23 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| betaNote | (none) | Beta release — actively iterating toward v1.0. Expect rou… | ACCEPT (override) | PENDING |
| betaNote.desktop | Beta release — actively iterating toward v1.0. Expect rou… | (none) | PORT→web | PENDING |
| copyright | (none) | © 2026 Bharadwaj.  | ACCEPT (override) | PENDING |
| description.desktop.line1 | A notation editor for Hindustani classical music in the B… | (none) | PORT→web | PENDING |
| description.desktop.line2 | Designed primarily for sitar compositions — Gat, Bandish,… | (none) | PORT→web | PENDING |
| description.paragraph1 | (none) | A notation editor for Hindustani classical music in the B… | ACCEPT (override) | PENDING |
| description.paragraph2 | (none) | Supports Devanagari, Kannada, Telugu, and English scripts. | ACCEPT (override) | PENDING |
| license | (none) | Free and open source under the MIT License. | ACCEPT (override) | PENDING |
| license.desktop | Free and open source. Copyright (c) 2026 Bharadwaj. | (none) | PORT→web | PENDING |
| links.header | (none) | Links | ACCEPT (override) | PENDING |
| links.selfHosting | (none) | Self-hosting guide | ACCEPT (override) | PENDING |
| links.userGuide | (none) | User guide | ACCEPT (override) | PENDING |
| links.userGuide.desktop | User guide & documentation | (none) | PORT→web | PENDING |
| links.webVersion | Web version: {url} [1 param] | (none) | PORT→web | PENDING |
| privacy.desktop | Anonymous usage stats (which features get touched, how lo… | (none) | PORT→web | PENDING |
| privacy.header | (none) | Privacy | ACCEPT (override) | PENDING |
| privacy.text | (none) | While you use the app, anonymous usage events (clicks, ke… | ACCEPT (override) | PENDING |
| sampleToggle | Show sample composition on startup | (none) | PORT→web | PENDING |
| support.link | (none) | Support the project | ACCEPT (override) | PENDING |
| support.suffix | (none) |  — UPI / PayPal options. | ACCEPT (override) | PENDING |
| support.text | (none) | 💖  | ACCEPT (override) | PENDING |
| tech | (none) | Desktop: Scala 3 + ScalaFX. Web: Elm + Tapir. | ACCEPT (override) | PENDING |
| tech.desktop | Built with Scala 3 + ScalaFX (desktop) and Elm + Tapir (web) | (none) | PORT→web | PENDING |

### dialog.bugReport  (8 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| button.sentSuccess | Sent ✓ | (none) | ACCEPT (override) | DONE |
| disclosure.desktop | We'll include a short replay of recent keystrokes + a scr… | (none) | PORT→web | PENDING |
| disclosure.web | (none) | We'll include a short replay of your recent actions in th… | ACCEPT (override) | PENDING |
| status.screenshotFailed | Screenshot failed ({error}) — sending without it. [1 param] | (none) | PORT→web | PENDING |
| status.sendFailed | Send failed: {error} [1 param] | (none) | PORT→web | PENDING |
| status.sending | Sending report... | (none) | PORT→web | PENDING |
| status.sendThrew | Send threw: {message} [1 param] | (none) | PORT→web | PENDING |
| status.sent | Sent. Report id: {reportId} [1 param] | (none) | PORT→web | PENDING |

### dialog.commandPalette  (4 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| noResults | (none) | No matching actions. | ACCEPT (override) | PENDING |
| searchPlaceholder | Search actions… (Esc to close) | (none) | PORT→web | PENDING |
| searchPlaceholderWeb | (none) | Search actions… (Esc to close, ↑↓ to navigate, Enter to run) | ACCEPT (override) | PENDING |
| title | Command Palette | (none) | PORT→web | PENDING |

### dialog.crashRecovery  (12 entries)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| buttonDiscard | Discard | (none) | ACCEPT (override) | DONE |
| buttonRetry | Retry send | (none) | ACCEPT (override) | DONE |
| buttonSend | Send report | (none) | ACCEPT (override) | DONE |
| descriptionLabel | Anything you remember doing right before? (optional) | (none) | ACCEPT (override) | DONE |
| descriptionPlaceholder | Optional context — what tab was open, what you'd just typ… | (none) | ACCEPT (override) | DONE |
| emailLabel | Email (optional, only if you want a reply) | (none) | ACCEPT (override) | DONE |
| explanation | The app crashed during your last session. Sending a repor… | (none) | ACCEPT (override) | DONE |
| stackTraceLabel | Stack trace: | (none) | ACCEPT (override) | DONE |
| statusSending | Sending... | (none) | ACCEPT (override) | DONE |
| statusSendingReport | Sending report... | (none) | ACCEPT (override) | DONE |
| title | Sangeet didn't shut down cleanly last time | (none) | ACCEPT (override) | DONE |
| windowTitle | Sangeet — crash recovery | (none) | ACCEPT (override) | DONE |

### dialog.keyboardCheatSheet  (69 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| action.addSection | Add section | (none) | PORT→web | PENDING |
| action.closeTab | Close tab | (none) | PORT→web | PENDING |
| action.compositionProperties | Composition properties | (none) | PORT→web | PENDING |
| action.copy | Copy | (none) | PORT→web | PENDING |
| action.cut | Cut | (none) | PORT→web | PENDING |
| action.cycleScript | Cycle notation script | (none) | PORT→web | PENDING |
| action.exportHtml | Export HTML | (none) | PORT→web | PENDING |
| action.newComposition | New composition | (none) | PORT→web | PENDING |
| action.nextTab | Next tab | (none) | PORT→web | PENDING |
| action.openFile | Open file | (none) | PORT→web | PENDING |
| action.openFolder | Open folder | (none) | PORT→web | PENDING |
| action.openUserGuide | Open user guide | (none) | PORT→web | PENDING |
| action.paste | Paste | (none) | PORT→web | PENDING |
| action.previousTab | Previous tab | (none) | PORT→web | PENDING |
| action.redo | Redo | (none) | PORT→web | PENDING |
| action.removeSection | Remove current section | (none) | PORT→web | PENDING |
| action.renameSection | Rename current section | (none) | PORT→web | PENDING |
| action.reportBug | Report a bug | (none) | PORT→web | PENDING |
| action.save | Save | (none) | PORT→web | PENDING |
| action.saveAs | Save as | (none) | PORT→web | PENDING |
| action.showCheatSheet | Show this cheat sheet | (none) | PORT→web | PENDING |
| action.toggleFileBrowser | Toggle file browser | (none) | PORT→web | PENDING |
| action.toggleTheme | Toggle theme | (none) | PORT→web | PENDING |
| action.undo | Undo | (none) | PORT→web | PENDING |
| hint.desktopFull | (none) | -shortcuts wired (browsers reserve many of them on web). … | ACCEPT (override) | PENDING |
| hint.keyboardRef | (none) | Keyboard Reference | ACCEPT (override) | PENDING |
| hint.web | (none) | Tip: most toolbar actions are accessible via the buttons … | ACCEPT (override) | PENDING |
| label.cancelOrnament | (none) | Cancel ornament mode | ACCEPT (override) | PENDING |
| label.chikari | (none) | Chikari (open strings) | ACCEPT (override) | PENDING |
| label.cutCopyPaste | (none) | Cut / Copy / Paste | ACCEPT (override) | PENDING |
| label.daRaStrokes | (none) | Da (inward) / Ra (outward) | ACCEPT (override) | PENDING |
| label.deleteEvent | (none) | Delete event | ACCEPT (override) | PENDING |
| label.doubleTapDual | (none) | Double-tap dual swar | ACCEPT (override) | PENDING |
| label.extendSelection | (none) | Extend selection | ACCEPT (override) | PENDING |
| label.fastTyping | (none) | Type 2–4 notes within 500 ms to auto-group | ACCEPT (override) | PENDING |
| label.finishOrnament | (none) | Finish multi-note ornament | ACCEPT (override) | PENDING |
| label.gamakAndolan | (none) | Gamak / Andolan / Gitkari | ACCEPT (override) | PENDING |
| label.kanSwar | (none) | Kan swar | ACCEPT (override) | PENDING |
| label.komalRe | (none) | Komal Re / Ga / Dha / Ni | ACCEPT (override) | PENDING |
| label.madhyaDefault | (none) | Madhya (default) | ACCEPT (override) | PENDING |
| label.mandraLower | (none) | Mandra (lower) | ACCEPT (override) | PENDING |
| label.meendDown | (none) | Meend ↓ | ACCEPT (override) | PENDING |
| label.meendUp | (none) | Meend ↑ | ACCEPT (override) | PENDING |
| label.moveCursor | (none) | Move cursor one beat | ACCEPT (override) | PENDING |
| label.nextSubbeat | (none) | Next sub-beat | ACCEPT (override) | PENDING |
| label.rest | (none) | Rest | ACCEPT (override) | PENDING |
| label.setNotesPerBeat | (none) | Set notes per beat | ACCEPT (override) | PENDING |
| label.showCheatSheet | (none) | Show this cheat sheet | ACCEPT (override) | PENDING |
| label.shuddhaSwaras | (none) | Shuddha swaras | ACCEPT (override) | PENDING |
| label.sparsh | (none) | Sparsh | ACCEPT (override) | PENDING |
| label.sustain | (none) | Sustain | ACCEPT (override) | PENDING |
| label.taarUpper | (none) | Taar (upper) | ACCEPT (override) | PENDING |
| label.tivraMa | (none) | Tivra Ma | ACCEPT (override) | PENDING |
| label.undoRedo | (none) | Undo / Redo | ACCEPT (override) | PENDING |
| section.edit.desktop | Edit | (none) | PORT→web | PENDING |
| section.file.desktop | File | (none) | PORT→web | PENDING |
| section.help.desktop | Help | (none) | PORT→web | PENDING |
| section.help.web | (none) | Help | ACCEPT (override) | PENDING |
| section.navigation | (none) | Navigation | ACCEPT (override) | PENDING |
| section.octave | (none) | Octave (saptak) | ACCEPT (override) | PENDING |
| section.ornaments | (none) | Ornaments | ACCEPT (override) | PENDING |
| section.sections.desktop | Sections | (none) | PORT→web | PENDING |
| section.selectionClipboard | (none) | Selection & clipboard | ACCEPT (override) | PENDING |
| section.strokes | (none) | Strokes | ACCEPT (override) | PENDING |
| section.subdivisions | (none) | Subdivisions | ACCEPT (override) | PENDING |
| section.swar | (none) | Swar (notes) | ACCEPT (override) | PENDING |
| section.tabs.desktop | Tabs | (none) | PORT→web | PENDING |
| section.view.desktop | View | (none) | PORT→web | PENDING |
| subtitle.desktop | Full reference: Help → User Guide → Keyboard Reference | (none) | PORT→web | PENDING |

### dialog.newComposition  (67 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| button.cancel | (none) | Cancel | ACCEPT (override) | PENDING |
| button.create | (none) | Create | ACCEPT (override) | PENDING |
| field.antaraStartingBeat.label | (none) | Antara Starting Beat (1-{matras}) [1 param] | ACCEPT (override) | PENDING |
| field.antaraStartingBeat.labelDesktop | Antara Starting Beat: | (none) | PORT→web | PENDING |
| field.arohan.label | Arohan: | (none) | PORT→web | PENDING |
| field.arohan.placeholder | auto-detected or enter manually | (none) | PORT→web | PENDING |
| field.avrohan.label | Avrohan: | (none) | PORT→web | PENDING |
| field.avrohan.placeholder | auto-detected or enter manually | (none) | PORT→web | PENDING |
| field.filePath.browseButton | Browse... | (none) | PORT→web | PENDING |
| field.filePath.browserTitle | Save Composition As | (none) | PORT→web | PENDING |
| field.filePath.label | Save to: | (none) | PORT→web | PENDING |
| field.filePath.placeholder | Select location to save .swar file | (none) | PORT→web | PENDING |
| field.gatStartingBeat.label | (none) | Gat Starting Beat (1-{matras}) [1 param] | ACCEPT (override) | PENDING |
| field.gatStartingBeat.labelDesktop | Gat Starting Beat: | (none) | PORT→web | PENDING |
| field.laya.atidrut | (none) | Ati-drut | ACCEPT (override) | PENDING |
| field.laya.atidrutDesktop | Ati-Drut | (none) | PORT→web | PENDING |
| field.laya.ativilambit | (none) | Ati-vilambit | ACCEPT (override) | PENDING |
| field.laya.ativilambitDesktop | Ati-Vilambit | (none) | PORT→web | PENDING |
| field.laya.label | (none) | Laya | ACCEPT (override) | PENDING |
| field.laya.labelDesktop | Laya: | (none) | PORT→web | PENDING |
| field.laya.none | (none) | None (Palta) | ACCEPT (override) | PENDING |
| field.laya.noneDesktop | (none) | (none) | PORT→web | PENDING |
| field.raag.label | (none) | Raag | ACCEPT (override) | PENDING |
| field.raag.labelDesktop | Raag: | (none) | PORT→web | PENDING |
| field.raag.placeholder | Type to search or enter custom raag | (none) | PORT→web | PENDING |
| field.samvadi.label | Samvadi: | (none) | PORT→web | PENDING |
| field.samvadi.placeholder | auto-detected | (none) | PORT→web | PENDING |
| field.script.label | Script: | (none) | PORT→web | PENDING |
| field.showSahitya.checkboxDesktop | Show lyrics row below swar | (none) | PORT→web | PENDING |
| field.showSahitya.label | (none) | Show Sahitya Line (Lyrics) | ACCEPT (override) | PENDING |
| field.showSahitya.labelDesktop | Sahitya line: | (none) | PORT→web | PENDING |
| field.showStrokes.checkboxDesktop | Show Da/Ra stroke indicators below swar | (none) | PORT→web | PENDING |
| field.showStrokes.label | (none) | Show Stroke Line (Da/Ra) | ACCEPT (override) | PENDING |
| field.showStrokes.labelDesktop | Stroke line: | (none) | PORT→web | PENDING |
| field.sthayiStartingBeat.label | (none) | Sthayi Starting Beat (1-{matras}) [1 param] | ACCEPT (override) | PENDING |
| field.sthayiStartingBeat.labelDesktop | Sthayi Starting Beat: | (none) | PORT→web | PENDING |
| field.taal.label | (none) | Taal | ACCEPT (override) | PENDING |
| field.taal.labelDesktop | Taal: | (none) | PORT→web | PENDING |
| field.taanCount.label | (none) | Taan Count | ACCEPT (override) | PENDING |
| field.taanCount.labelDesktop | Taans: | (none) | PORT→web | PENDING |
| field.taanStartingBeat.label | (none) | Taan Starting Beat (1-{matras}) [1 param] | ACCEPT (override) | PENDING |
| field.taanStartingBeat.labelDesktop | Taan Starting Beat: | (none) | PORT→web | PENDING |
| field.thaat.label | Thaat: | (none) | PORT→web | PENDING |
| field.thaat.placeholder | auto-detected or enter manually | (none) | PORT→web | PENDING |
| field.title.label | (none) | Title | ACCEPT (override) | PENDING |
| field.title.labelDesktop | Title: | (none) | PORT→web | PENDING |
| field.title.placeholder | (none) | Enter composition title | ACCEPT (override) | PENDING |
| field.title.placeholderDesktop | e.g. Yaman Vilambit Gat | (none) | PORT→web | PENDING |
| field.type.bandish | (none) | Bandish (Vocal) | ACCEPT (override) | PENDING |
| field.type.bandishDesktop | Bandish | (none) | PORT→web | PENDING |
| field.type.gat | (none) | Gat (Instrumental) | ACCEPT (override) | PENDING |
| field.type.gatDesktop | Gat | (none) | PORT→web | PENDING |
| field.type.label | (none) | Type | ACCEPT (override) | PENDING |
| field.type.labelDesktop | Type: | (none) | PORT→web | PENDING |
| field.type.palta | (none) | Palta (Practice) | ACCEPT (override) | PENDING |
| field.type.paltaDesktop | Palta | (none) | PORT→web | PENDING |
| field.type.sargam | (none) | Sargam (Practice) | ACCEPT (override) | PENDING |
| field.type.sargamDesktop | Sargam | (none) | PORT→web | PENDING |
| field.vadi.label | Vadi: | (none) | PORT→web | PENDING |
| field.vadi.placeholder | auto-detected | (none) | PORT→web | PENDING |
| header | Create a new composition | (none) | PORT→web | PENDING |
| raagDetected | Raag {name} recognized [1 param] | (none) | PORT→web | PENDING |
| raagNotFound | (raag not in database -- enter details manually) | (none) | PORT→web | PENDING |
| validation.filePathRequired | File path is required | (none) | PORT→web | PENDING |
| validation.layaRequired | Laya is required for Gat | (none) | PORT→web | PENDING |
| validation.raagRequired | Raag is required | (none) | PORT→web | PENDING |
| validation.titleRequired | Title is required | (none) | PORT→web | PENDING |

### dialog.properties  (16 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| button.cancel | (none) | Cancel | ACCEPT (override) | PENDING |
| button.save | (none) | Save | ACCEPT (override) | PENDING |
| field.antaraStartingBeat.labelDesktop | Antara Starting Beat: | (none) | PORT→web | PENDING |
| field.gatStartingBeat.labelDesktop | Gat Starting Beat: | (none) | PORT→web | PENDING |
| field.raag.label | Raag: | (none) | PORT→web | PENDING |
| field.sectionStartingBeat.label | (none) | {name} Starting Beat (1-{matras}) [2 params] | ACCEPT (override) | PENDING |
| field.sthayiStartingBeat.labelDesktop | Sthayi Starting Beat: | (none) | PORT→web | PENDING |
| field.taal.label | (none) | Taal | ACCEPT (override) | PENDING |
| field.taal.labelDesktop | Taal: | (none) | PORT→web | PENDING |
| field.taanStartingBeat.labelDesktop | Taan Starting Beat: | (none) | PORT→web | PENDING |
| field.title.label | (none) | Title | ACCEPT (override) | PENDING |
| field.title.labelDesktop | Title: | (none) | PORT→web | PENDING |
| field.title.placeholder | (none) | Composition title | ACCEPT (override) | PENDING |
| field.type.label | Type: | (none) | PORT→web | PENDING |
| header | Edit composition details | (none) | PORT→web | PENDING |
| validation.beatsClamped | Starting beats clamped to new taal range (1-{matras}) [1 … | (none) | PORT→web | PENDING |

### dialog.support  (8 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| international.paypalLink | (none) | Support via PayPal | ACCEPT (override) | PENDING |
| international.platformLink | Support via {platform} [1 param] | (none) | PORT→web | PENDING |
| upi.handle | (none) | bharath12345-1@oksbi | ACCEPT (override) | PENDING |
| upi.handleLabel | (none) | UPI handle:  | ACCEPT (override) | PENDING |
| upi.handleLabelWithValue | UPI handle: {handle} [1 param] | (none) | PORT→web | PENDING |
| upi.qrAlt | (none) | UPI QR code | ACCEPT (override) | PENDING |
| upi.qrPlaceholder | (QR code image will appear here) | (none) | PORT→web | PENDING |
| windowTitle | Support — Sangeet Notes Editor | (none) | PORT→web | PENDING |

### editor.sampleWarning  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| sampleWarning | This is a read-only sample showing Yaman Vilambit Gat. | (none) | ACCEPT (override) | DONE |

### fileBrowser.addFolderDialogTitle  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| addFolderDialogTitle | Add Folder | (none) | ACCEPT (override) | DONE |

### fileBrowser.addFolderTooltip  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| addFolderTooltip | Add a folder | (none) | ACCEPT (override) | DONE |

### fileBrowser.bookmarkTooltip  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| bookmarkTooltip | (none) | Bookmark | ACCEPT | PENDING |

### fileBrowser.connectDrive  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| connectDrive | (none) | Connect Google Drive | ACCEPT | PENDING |

### fileBrowser.connecting  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| connecting | (none) | Connecting... | ACCEPT | PENDING |

### fileBrowser.deleteDialogPrompt  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| deleteDialogPrompt | Delete {filename}? | (none) | ACCEPT (override) | DONE |

### fileBrowser.deleteDialogTitle  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| deleteDialogTitle | Delete File | (none) | ACCEPT (override) | DONE |

### fileBrowser.deleteDialogWarning  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| deleteDialogWarning | This action cannot be undone. | (none) | ACCEPT (override) | DONE |

### fileBrowser.deleteTooltip  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| deleteTooltip | (none) | Delete | ACCEPT | PENDING |

### fileBrowser.driveConnected  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| driveConnected | (none) | Drive connected | ACCEPT | PENDING |

### fileBrowser.emptyState  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| emptyState | (none) | Connect Drive to browse files | ACCEPT | PENDING |

### fileBrowser.errorFileExists  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| errorFileExists | File already exists: {name} | (none) | ACCEPT (override) | DONE |

### fileBrowser.errorFolderExists  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| errorFolderExists | Folder already exists: {name} | (none) | ACCEPT (override) | DONE |

### fileBrowser.errorFolderOpen  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| errorFolderOpen | Folder already open: {name} | (none) | ACCEPT (override) | DONE |

### fileBrowser.errorMoveExists  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| errorMoveExists | A file named {name} already exists in the destination | (none) | ACCEPT (override) | DONE |

### fileBrowser.errorNotDirectory  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| errorNotDirectory | Not a directory: {path} | (none) | ACCEPT (override) | DONE |

### fileBrowser.errorRenameExists  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| errorRenameExists | A file with that name already exists | (none) | ACCEPT (override) | DONE |

### fileBrowser.headerLabel  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| headerLabel | FILES | (none) | ACCEPT (override) | DONE |

### fileBrowser.hideFilesTooltip  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| hideFilesTooltip | (none) | Hide Files | ACCEPT | PENDING |

### fileBrowser.logAddedFolder  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| logAddedFolder | Added folder: {name} | (none) | ACCEPT (override) | DONE |

### fileBrowser.logBookmarked  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| logBookmarked | Bookmarked: {name} | (none) | ACCEPT (override) | DONE |

### fileBrowser.logCreatedFile  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| logCreatedFile | Created: {name} | (none) | ACCEPT (override) | DONE |

### fileBrowser.logCreatedFolder  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| logCreatedFolder | Created folder: {name} | (none) | ACCEPT (override) | DONE |

### fileBrowser.logDeleted  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| logDeleted | Deleted: {name} | (none) | ACCEPT (override) | DONE |

### fileBrowser.logMoved  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| logMoved | Moved: {name} -> {dest} | (none) | ACCEPT (override) | DONE |

### fileBrowser.logRemovedBookmark  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| logRemovedBookmark | Removed bookmark: {name} | (none) | ACCEPT (override) | DONE |

### fileBrowser.logRemovedFolder  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| logRemovedFolder | Removed folder: {name} | (none) | ACCEPT (override) | DONE |

### fileBrowser.logRenamed  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| logRenamed | Renamed: {old} -> {new} | (none) | ACCEPT (override) | DONE |

### fileBrowser.menuBookmark  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| menuBookmark | Bookmark | (none) | ACCEPT (override) | DONE |

### fileBrowser.menuDelete  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| menuDelete | Delete | (none) | ACCEPT (override) | DONE |

### fileBrowser.menuMoveTo  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| menuMoveTo | Move to... | (none) | ACCEPT (override) | DONE |

### fileBrowser.menuNewFile  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| menuNewFile | New .swar File | (none) | ACCEPT (override) | DONE |

### fileBrowser.menuNewFolder  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| menuNewFolder | New Folder | (none) | ACCEPT (override) | DONE |

### fileBrowser.menuOpen  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| menuOpen | Open | (none) | ACCEPT (override) | DONE |

### fileBrowser.menuRefresh  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| menuRefresh | Refresh | (none) | ACCEPT (override) | DONE |

### fileBrowser.menuRemoveBookmark  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| menuRemoveBookmark | Remove Bookmark | (none) | ACCEPT (override) | DONE |

### fileBrowser.menuRemoveFromBrowser  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| menuRemoveFromBrowser | Remove from Browser | (none) | ACCEPT (override) | DONE |

### fileBrowser.menuRename  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| menuRename | Rename | (none) | ACCEPT (override) | DONE |

### fileBrowser.moveToDialogTitle  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| moveToDialogTitle | Move to... | (none) | ACCEPT (override) | DONE |

### fileBrowser.newFileDialogPrompt  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| newFileDialogPrompt | Enter filename (without .swar extension) | (none) | ACCEPT (override) | DONE |

### fileBrowser.newFileDialogTitle  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| newFileDialogTitle | New Composition File | (none) | ACCEPT (override) | DONE |

### fileBrowser.newFolder  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| defaultName | (none) | New Folder | ACCEPT | PENDING |

### fileBrowser.newFolderDialogPrompt  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| newFolderDialogPrompt | Enter folder name | (none) | ACCEPT (override) | DONE |

### fileBrowser.newFolderDialogTitle  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| newFolderDialogTitle | New Folder | (none) | ACCEPT (override) | DONE |

### fileBrowser.refreshTooltip  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| refreshTooltip | (none) | Refresh | ACCEPT | PENDING |

### fileBrowser.removeBookmarkTooltip  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| removeBookmarkTooltip | (none) | Remove bookmark | ACCEPT | PENDING |

### fileBrowser.renameDialogPrompt  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| renameDialogPrompt | Enter new name | (none) | ACCEPT (override) | DONE |

### fileBrowser.renameDialogTitle  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| renameDialogTitle | Rename | (none) | ACCEPT (override) | DONE |

### fileBrowser.showFilesTooltip  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| showFilesTooltip | (none) | Show Files | ACCEPT | PENDING |

### header.arohanLabel  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| arohanLabel | Arohan | (none) | PORT→web | PENDING |

### header.avrohanLabel  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| avrohanLabel | Avrohan | (none) | PORT→web | PENDING |

### header.beatPrefix  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| beatPrefix | (none) | Beat  | ACCEPT | PENDING |

### header.cyclePrefix  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| cyclePrefix | (none) | Cycle  | ACCEPT | PENDING |

### header.layaLabel  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| layaLabel | Laya | (none) | PORT→web | PENDING |

### header.modeLabel  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| modeLabel | (none) | Mode:  | ACCEPT | PENDING |

### header.modeStroke  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| modeStroke | (none) | Stroke | ACCEPT | PENDING |

### header.modeSwar  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| modeSwar | (none) | Swar | ACCEPT | PENDING |

### header.octaveAtiMandra  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| octaveAtiMandra | (none) | Ati-Mandra | ACCEPT | PENDING |

### header.octaveAtiTaar  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| octaveAtiTaar | (none) | Ati-Taar | ACCEPT | PENDING |

### header.octaveLabel  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| octaveLabel | (none) | Octave:  | ACCEPT | PENDING |

### header.octaveMadhya  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| octaveMadhya | (none) | Madhya | ACCEPT | PENDING |

### header.octaveMandra  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| octaveMandra | (none) | Mandra | ACCEPT | PENDING |

### header.octaveTaar  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| octaveTaar | (none) | Taar | ACCEPT | PENDING |

### header.raagLabel  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| raagLabel | Raag | (none) | PORT→web | PENDING |

### header.samvadiLabel  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| samvadiLabel | Samvadi | (none) | PORT→web | PENDING |

### header.subPrefix  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| subPrefix | (none) | Sub  | ACCEPT | PENDING |

### header.taalLabel  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| taalLabel | Taal | (none) | PORT→web | PENDING |

### header.thaatLabel  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| thaatLabel | Thaat | (none) | PORT→web | PENDING |

### header.vadiLabel  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| vadiLabel | Vadi | (none) | PORT→web | PENDING |

### keyboardLegend.nav  (6 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| backspace.web | (none) | Delete last | ACCEPT (override) | PENDING |
| enter | Next cycle | (none) | PORT→web | PENDING |
| moveCursor | Move cursor | (none) | PORT→web | PENDING |
| prevNextBeat.web | (none) | Previous / Next beat | ACCEPT (override) | PENDING |
| tab.desktop | Next beat | (none) | PORT→web | PENDING |
| tab.web | (none) | Next sub-beat | ACCEPT (override) | PENDING |

### keyboardLegend.octave  (6 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| backToMadhya | Back to madhya | (none) | PORT→web | PENDING |
| madhya.web | (none) | Madhya (middle) | ACCEPT (override) | PENDING |
| mandra.desktop | Next note in mandra | (none) | PORT→web | PENDING |
| mandra.web | (none) | Mandra (lower) | ACCEPT (override) | PENDING |
| taar.desktop | Next note in taar | (none) | PORT→web | PENDING |
| taar.web | (none) | Taar (upper) | ACCEPT (override) | PENDING |

### keyboardLegend.ornamentKeys  (3 entries)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| multiNote | ..↵ = type notes, press Enter | (none) | PORT→web | PENDING |
| oneNote | ♪  = type one swar key | (none) | PORT→web | PENDING |
| twoNotes | ♪♪ = type start, then end note | (none) | PORT→web | PENDING |

### keyboardLegend.ornaments  (22 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| andolan | (none) | Andolan | ACCEPT (override) | PENDING |
| andolan.desktop | Andolan (gentle oscillation) | (none) | PORT→web | PENDING |
| gamak | (none) | Gamak | ACCEPT (override) | PENDING |
| gamak.desktop | Gamak (heavy oscillation) | (none) | PORT→web | PENDING |
| ghaseet | (none) | Ghaseet (then type note) | ACCEPT (override) | PENDING |
| ghaseet.desktop | Ghaseet (heavy pull) | (none) | PORT→web | PENDING |
| gitkari | (none) | Gitkari | ACCEPT (override) | PENDING |
| gitkari.desktop | Gitkari (hammer/pull trill) | (none) | PORT→web | PENDING |
| kan | (none) | Kan Swar (then type note) | ACCEPT (override) | PENDING |
| kan.desktop | Kan Swar (grace note) | (none) | PORT→web | PENDING |
| krintan.desktop | Krintan (pull-off seq.) | (none) | PORT→web | PENDING |
| krintan.web | (none) | Krintan (type notes, Enter) | ACCEPT (override) | PENDING |
| meendAsc | (none) | Meend Asc (type start, end) | ACCEPT (override) | PENDING |
| meendAsc.desktop | Meend ↑ (ascending glide) | (none) | PORT→web | PENDING |
| meendDesc | (none) | Meend Desc | ACCEPT (override) | PENDING |
| meendDesc.desktop | Meend ↓ (descending glide) | (none) | PORT→web | PENDING |
| murki | (none) | Murki (type notes, Enter) | ACCEPT (override) | PENDING |
| murki.desktop | Murki (ornamental turn) | (none) | PORT→web | PENDING |
| sparsh | (none) | Sparsh (then type note) | ACCEPT (override) | PENDING |
| sparsh.desktop | Sparsh (light touch) | (none) | PORT→web | PENDING |
| zamzama | (none) | Zamzama (type notes, Enter) | ACCEPT (override) | PENDING |
| zamzama.desktop | Zamzama (rapid cluster) | (none) | PORT→web | PENDING |

### keyboardLegend.redo  (2 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| desktop | Redo | (none) | PORT→web | PENDING |
| web | (none) | Redo | ACCEPT (override) | PENDING |

### keyboardLegend.scriptLabel  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| scriptLabel | Script: {scriptName} | (none) | PORT→web | PENDING |

### keyboardLegend.section  (14 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| ornamentKeys | Ornament Keys | (none) | PORT→web | PENDING |
| ornaments | (none) | Ornaments (Alt+key) | ACCEPT (override) | PENDING |
| ornamentsMultiNote | Ornaments -- Multi-Note | (none) | PORT→web | PENDING |
| ornamentsOneNote | Ornaments -- One Note | (none) | PORT→web | PENDING |
| ornamentsSimple | Ornaments -- Simple | (none) | PORT→web | PENDING |
| ornamentsTwoNotes | Ornaments -- Two Notes | (none) | PORT→web | PENDING |
| strokes | (none) | Strokes | ACCEPT (override) | PENDING |
| strokesMizrab | Strokes (Mizrab) | (none) | PORT→web | PENDING |
| subdivisions | Subdivisions | (none) | PORT→web | PENDING |
| swarInput | (none) | Swar Input | ACCEPT (override) | PENDING |
| swarNotes | Swar (Notes) | (none) | PORT→web | PENDING |
| tips | Tips | (none) | PORT→web | PENDING |
| undoRedo | (none) | Undo/Redo | ACCEPT (override) | PENDING |
| undoRedoDesktop | Undo / Redo | (none) | PORT→web | PENDING |

### keyboardLegend.special  (4 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| deleteLast | Delete last note | (none) | PORT→web | PENDING |
| rest | Rest (silence) | (none) | PORT→web | PENDING |
| subdivisions | (none) | Set subdivisions per beat | ACCEPT (override) | PENDING |
| sustain | Sustain (hold) | (none) | PORT→web | PENDING |

### keyboardLegend.strokes  (4 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| da | Da (inward stroke) | (none) | PORT→web | PENDING |
| keys.web | (none) | Da / Ra / Jod (in stroke mode) | ACCEPT (override) | PENDING |
| ra | Ra (outward stroke) | (none) | PORT→web | PENDING |
| toggleMode.web | (none) | Toggle Swar/Stroke mode | ACCEPT (override) | PENDING |

### keyboardLegend.subdivisions  (2 entries)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| doubleTap | Double-tap for dual swar | (none) | PORT→web | PENDING |
| setPerBeat | Set notes per beat (2-8) | (none) | PORT→web | PENDING |

### keyboardLegend.swar  (6 entries)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| dualSwar | (none) | Dual swar (double-tap) | ACCEPT | PENDING |
| komal | (none) | Komal variants | ACCEPT | PENDING |
| rest | (none) | Rest | ACCEPT | PENDING |
| shuddha | (none) | Shuddha notes | ACCEPT | PENDING |
| sustain | (none) | Sustain | ACCEPT | PENDING |
| tivraMa | (none) | Tivra Ma | ACCEPT | PENDING |

### keyboardLegend.tips  (3 entries)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| applyToLast | Strokes & ornaments apply to the last entered note | (none) | PORT→web | PENDING |
| octaveReset | . and ' affect only the next note, then reset to madhya | (none) | PORT→web | PENDING |
| shiftVariant | Shift = komal/tivra variant | (none) | PORT→web | PENDING |

### keyboardLegend.title  (2 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| desktop | Keyboard Reference | (none) | PORT→web | PENDING |
| web | (none) | Keyboard Shortcuts | ACCEPT (override) | PENDING |

### mainApp.openFolderDialogTitle  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| openFolderDialogTitle | Open Folder | (none) | ACCEPT (override) | DONE |

### status.apiError  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| apiError | (none) | API error: {message} | ACCEPT | PENDING |

### status.badBody  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| badBody | (none) | Bad body: {error} | ACCEPT | PENDING |

### status.badStatus  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| badStatus | (none) | Bad status: {code} | ACCEPT | PENDING |

### status.badUrl  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| badUrl | (none) | Bad URL: {url} | ACCEPT | PENDING |

### status.bugReportFailed  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| bugReportFailed | (none) | Bug report failed: {message} | ACCEPT | PENDING |

### status.bugReportSent  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| bugReportSent | (none) | Bug report sent — thanks! ({message}) | ACCEPT | PENDING |

### status.clipboardEmpty  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| clipboardEmpty | Clipboard is empty | (none) | PORT→web | PENDING |

### status.clipboardNotSangeetData  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| clipboardNotSangeetData | Clipboard does not contain Sangeet data | (none) | PORT→web | PENDING |

### status.closedTabSwitched  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| closedTabSwitched | (none) | Closed tab, switched to {filename} | ACCEPT | PENDING |

### status.colorsLoaded  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| colorsLoaded | (none) | Colors loaded | ACCEPT | PENDING |

### status.connectedToDrive  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| connectedToDrive | (none) | Connected to Google Drive | ACCEPT | PENDING |

### status.copiedEvents  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| copiedEvents | Copied {count} event(s) | (none) | PORT→web | PENDING |

### status.created  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| created | (none) | Created: {title} | ACCEPT | PENDING |

### status.cursorPlaced  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| cursorPlaced | Cursor placed at cycle {cycle}, beat {beat} | (none) | ACCEPT (override) | DONE |

### status.cutEvents  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| cutEvents | Cut {count} event(s) | (none) | PORT→web | PENDING |

### status.driveAuthFailed  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| driveAuthFailed | (none) | Drive authentication failed | ACCEPT | PENDING |

### status.driveError  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| driveError | (none) | Drive error: {message} | ACCEPT | PENDING |

### status.errorOpeningFile  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| errorOpeningFile | Error opening file: {message} | (none) | ACCEPT (override) | DONE |

### status.errorOpeningHtml  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| errorOpeningHtml | Error opening HTML: {message} | (none) | ACCEPT (override) | DONE |

### status.errorReloading  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| errorReloading | Error reloading: {message} | (none) | ACCEPT (override) | DONE |

### status.exportingHtml  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| exportingHtml | (none) | Exporting HTML... | ACCEPT | PENDING |

### status.failedToParseDriveFileContent  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| failedToParseDriveFileContent | (none) | Failed to parse Drive file content | ACCEPT | PENDING |

### status.failedToParseDriveFolderListing  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| failedToParseDriveFolderListing | (none) | Failed to parse Drive folder listing | ACCEPT | PENDING |

### status.fileSavedToDrive  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| fileSavedToDrive | (none) | File saved to Drive | ACCEPT | PENDING |

### status.fileSelected  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| fileSelected | (none) | File selected: {filename} | ACCEPT | PENDING |

### status.fileWasDeleted  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| fileWasDeleted | File was deleted: {title} | (none) | ACCEPT (override) | DONE |

### status.httpError  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| httpError | (none) | HTTP error: {message} | ACCEPT | PENDING |

### status.lastTabClosedNewCreated  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| lastTabClosedNewCreated | (none) | Last tab closed — new tab created | ACCEPT | PENDING |

### status.loadedRaags  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| loadedRaags | (none) | Loaded {count} raags | ACCEPT | PENDING |

### status.loadedTaals  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| loadedTaals | (none) | Loaded {count} taals | ACCEPT | PENDING |

### status.loadingFileFromDrive  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| loadingFileFromDrive | (none) | Loading file from Drive: {filename} | ACCEPT | PENDING |

### status.networkError  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| networkError | (none) | Network error | ACCEPT | PENDING |

### status.newTab  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| newTab | (none) | New tab | ACCEPT | PENDING |

### status.noEventsInSelection  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| noEventsInSelection | No events in selection | (none) | PORT→web | PENDING |

### status.noSelection  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| noSelection | No selection | (none) | PORT→web | PENDING |

### status.noSelectionToCopy  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| noSelectionToCopy | (none) | No selection to copy | ACCEPT | PENDING |

### status.noSelectionToCut  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| noSelectionToCut | (none) | No selection to cut | ACCEPT | PENDING |

### status.nothingToRedo  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| nothingToRedo | (none) | Nothing to redo | ACCEPT | PENDING |

### status.nothingToUndo  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| nothingToUndo | (none) | Nothing to undo | ACCEPT | PENDING |

### status.opened  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| opened | (none) | Opened: {title} | ACCEPT | PENDING |

### status.openedDesktop  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| openedDesktop | Opened: {filename} | (none) | PORT→web | PENDING |

### status.openingFromDrive  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| openingFromDrive | (none) | Opening from Drive: {filename} | ACCEPT | PENDING |

### status.ornamentCancelled  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| ornamentCancelled | (none) | Ornament mode cancelled | ACCEPT | PENDING |

### status.ornamentCollecting  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| ornamentCollecting | (none) | Collecting ornament notes... | ACCEPT | PENDING |

### status.ornamentGhaseet  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| ornamentGhaseet | (none) | Ghaseet: type the target note | ACCEPT | PENDING |

### status.ornamentKanSwar  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| ornamentKanSwar | (none) | Kan Swar: type the grace note | ACCEPT | PENDING |

### status.ornamentKrintan  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| ornamentKrintan | (none) | Krintan: type notes, then Enter | ACCEPT | PENDING |

### status.ornamentMeendAsc  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| ornamentMeendAsc | (none) | Meend (ascending): type start note | ACCEPT | PENDING |

### status.ornamentMeendDesc  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| ornamentMeendDesc | (none) | Meend (descending): type start note | ACCEPT | PENDING |

### status.ornamentMurki  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| ornamentMurki | (none) | Murki: type notes, then Enter | ACCEPT | PENDING |

### status.ornamentSparsh  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| ornamentSparsh | (none) | Sparsh: type the touch note | ACCEPT | PENDING |

### status.ornamentZamzama  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| ornamentZamzama | (none) | Zamzama: type notes, then Enter | ACCEPT | PENDING |

### status.pastedEvents  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| pastedEvents | Pasted {count} event(s) | (none) | PORT→web | PENDING |

### status.pleaseSelectValidTaalRaag  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| pleaseSelectValidTaalRaag | (none) | Please select a valid taal and raag | ACCEPT | PENDING |

### status.preview  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| preview | Preview: {filename} | (none) | ACCEPT (override) | DONE |

### status.propertiesUpdatedTaal  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| propertiesUpdatedTaal | (none) | Properties updated — taal: {taalName} | ACCEPT | PENDING |

### status.propertiesUpdatedTaalNotFound  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| propertiesUpdatedTaalNotFound | (none) | Properties updated (taal not found, kept previous) | ACCEPT | PENDING |

### status.redo  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| redo | (none) | Redo | ACCEPT | PENDING |

### status.reloaded  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| reloaded | Reloaded: {filename} | (none) | ACCEPT (override) | DONE |

### status.requestTimeout  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| requestTimeout | (none) | Request timed out | ACCEPT | PENDING |

### status.sahityaLineHidden  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| sahityaLineHidden | (none) | Sahitya line hidden | ACCEPT | PENDING |

### status.sahityaLineShown  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| sahityaLineShown | (none) | Sahitya line shown | ACCEPT | PENDING |

### status.sampleDismissed  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| sampleDismissed | Sample dismissed — won't appear on next launch | (none) | ACCEPT (override) | DONE |

### status.sampleLoaded  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| sampleLoaded | Uneditable sample loaded | (none) | ACCEPT (override) | DONE |

### status.samplePrompt  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| samplePrompt | To start, click New to create a composition | (none) | ACCEPT (override) | DONE |

### status.savingComposition  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| savingComposition | (none) | Saving composition... | ACCEPT | PENDING |

### status.scriptChanged  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| scriptChanged | (none) | Script changed to {scriptName} | ACCEPT | PENDING |

### status.sectionAdded  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| sectionAdded | (none) | Section added | ACCEPT | PENDING |

### status.sectionRemoved  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| sectionRemoved | (none) | Section removed | ACCEPT | PENDING |

### status.sectionRenamed  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| sectionRenamed | (none) | Section renamed | ACCEPT | PENDING |

### status.sectionsReordered  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| sectionsReordered | (none) | Sections reordered | ACCEPT | PENDING |

### status.startingBeatsUpdated  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| startingBeatsUpdated | (none) | Starting beats updated | ACCEPT | PENDING |

### status.strokeLineHidden  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| strokeLineHidden | (none) | Stroke line hidden | ACCEPT | PENDING |

### status.strokeLineShown  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| strokeLineShown | (none) | Stroke line shown | ACCEPT | PENDING |

### status.switchedToSection  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| switchedToSection | (none) | Switched to section {number} | ACCEPT | PENDING |

### status.switchedToSectionDesktop  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| switchedToSectionDesktop | Switched to section: {name} | (none) | PORT→web | PENDING |

### status.switchedToTab  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| switchedToTab | (none) | Switched to {filename} | ACCEPT | PENDING |

### status.tabClosed  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| tabClosed | (none) | Tab closed | ACCEPT | PENDING |

### status.undo  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| undo | (none) | Undo | ACCEPT | PENDING |

### statusBar.logLabel  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| logLabel | Log | (none) | ACCEPT (override) | DONE |

### toolbar.edit  (4 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| redo | (none) | Redo | ACCEPT (override) | PENDING |
| redo.tooltip | (none) | Redo (Ctrl+Y) | ACCEPT (override) | PENDING |
| redo.tooltip.desktop | Redo (Ctrl+Shift+Z) | (none) | PORT→web | PENDING |
| undo | (none) | Undo | ACCEPT (override) | PENDING |

### toolbar.file  (14 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| exportHtml | (none) | HTML | ACCEPT (override) | PENDING |
| exportHtml.tooltip | (none) | Export HTML | ACCEPT (override) | PENDING |
| exportHtml.tooltip.desktop | Export composition as HTML | (none) | PORT→web | PENDING |
| new | (none) | New | ACCEPT (override) | PENDING |
| new.tooltip | (none) | New Composition (Ctrl+N) | ACCEPT (override) | PENDING |
| new.tooltip.desktop | Create a new composition | (none) | PORT→web | PENDING |
| open | (none) | Open | ACCEPT (override) | PENDING |
| open.tooltip | (none) | Open File | ACCEPT (override) | PENDING |
| open.tooltip.desktop | Open a .swar file | (none) | PORT→web | PENDING |
| openFolder.tooltip | Open a folder in the file browser | (none) | ACCEPT (override) | DONE |
| save | (none) | Save | ACCEPT (override) | PENDING |
| save.tooltip | (none) | Save File (Ctrl+S) | ACCEPT (override) | PENDING |
| save.tooltip.desktop | Save composition to current file | (none) | PORT→web | PENDING |
| saveAs.tooltip | Save composition as a new .swar file | (none) | ACCEPT (override) | DONE |

### toolbar.help  (8 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| about | (none) | About | ACCEPT (override) | PENDING |
| keyboardShortcuts | (none) | ? | ACCEPT (override) | PENDING |
| properties | (none) | Properties | ACCEPT (override) | PENDING |
| reportBug | (none) | 🐞 Report bug | ACCEPT (override) | PENDING |
| reportBug.tooltip | (none) | Report a bug — includes a short replay so it can be repro… | ACCEPT (override) | PENDING |
| reportBug.tooltip.desktop | Report a bug — includes a screenshot + recent keystrokes … | (none) | PORT→web | PENDING |
| support | (none) | 💖 | ACCEPT (override) | PENDING |
| userGuide.tooltip | Open the user guide (F1) | (none) | PORT→web | PENDING |

### toolbar.mode  (2 entries)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| stroke | (none) | Mode: Stroke | ACCEPT | PENDING |
| swar | (none) | Mode: Swar | ACCEPT | PENDING |

### toolbar.ornament  (7 entries)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| krintanEnd | (none) | Krintan: type end note / Enter | ACCEPT | PENDING |
| krintanStart | (none) | Krintan: type start note | ACCEPT | PENDING |
| meendEnd | (none) | Meend: type end note | ACCEPT | PENDING |
| meendStart | (none) | Meend: type start note | ACCEPT | PENDING |
| murki | (none) | Murki: {count} notes (Enter to apply) [1 param] | ACCEPT | PENDING |
| singleNote | (none) | Orn: {name} (type note) [1 param] | ACCEPT | PENDING |
| zamzama | (none) | Zamzama: {count} notes (Enter to apply) [1 param] | ACCEPT | PENDING |

### toolbar.script  (1 entry)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| tooltip | Change notation script | (none) | PORT→web | PENDING |

### toolbar.section  (2 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| add.tooltip | (none) | Add Section | ACCEPT (override) | PENDING |
| add.tooltip.desktop | Add a new section to the composition | (none) | PORT→web | PENDING |

### toolbar.tabs  (2 entries)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| close.tooltip | (none) | Close tab | ACCEPT | PENDING |
| new.tooltip | (none) | New Tab | ACCEPT | PENDING |

### toolbar.theme  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| toggle.tooltip | Toggle light / dark theme | (none) | PORT→web | PENDING |

### toolbar.view  (6 entries)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| toggleKeyboardLegend | (none) | Keys | ACCEPT | PENDING |
| toggleKeyboardLegend.tooltip | (none) | Keyboard Shortcuts | ACCEPT | PENDING |
| toggleSahityaLine | (none) | Sahitya | ACCEPT | PENDING |
| toggleSahityaLine.tooltip | (none) | Toggle Sahitya Line | ACCEPT | PENDING |
| toggleStrokeLine | (none) | Strokes | ACCEPT | PENDING |
| toggleStrokeLine.tooltip | (none) | Toggle Stroke Line | ACCEPT | PENDING |

### view.loading  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| loading | (none) | Loading... | ACCEPT | PENDING |

