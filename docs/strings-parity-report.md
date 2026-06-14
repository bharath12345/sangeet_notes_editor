# UI Strings Parity Report — Side-by-Side

> Generated: 2026-06-13. Regenerate with `make strings-report`.

## Summary

| Bucket                              | Count |
| ----------------------------------- | ----- |
| Shared (identical, hidden below)    | 77 |
| NORMALIZE candidates                | 0 |
| PORT→desk candidates                | 0 |
| PORT→web candidates                 | 177 |
| ACCEPT candidates                   | 309 |
| **Total asymmetric concepts**       | **486** |
| Status: DONE                        | 486 |
| Status: PENDING                     | 0 |

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
| defaultName | (none) | New Section | ACCEPT | DONE |

### app.windowTitle  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| windowTitle | Sangeet Notes Editor | (none) | ACCEPT (override) | DONE |

### appAction.closeActiveTab  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| closeActiveTab | Close active tab | (none) | PORT→web | DONE |

### appAction.cycleNotationScript  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| cycleNotationScript | Cycle notation script | (none) | PORT→web | DONE |

### appAction.group  (2 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| sections | Sections | (none) | PORT→web | DONE |
| tabs | Tabs | (none) | PORT→web | DONE |

### appAction.nextTab  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| nextTab | Next tab | (none) | PORT→web | DONE |

### appAction.openFolder  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| openFolder | Open folder | (none) | ACCEPT (override) | DONE |

### appAction.openUserGuide  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| openUserGuide | Open user guide | (none) | PORT→web | DONE |

### appAction.previousTab  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| previousTab | Previous tab | (none) | PORT→web | DONE |

### appAction.redo  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| redo | (none) | Redo | ACCEPT | DONE |

### appAction.removeCurrentSection  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| removeCurrentSection | Remove current section | (none) | PORT→web | DONE |

### appAction.renameCurrentSection  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| renameCurrentSection | Rename current section | (none) | PORT→web | DONE |

### appAction.saveAs  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| saveAs | Save as | (none) | PORT→web | DONE |

### appAction.supportProject  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| supportProject | (none) | Support the project | ACCEPT | DONE |

### appAction.toggleFileBrowser  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| toggleFileBrowser | Toggle file browser | (none) | ACCEPT (override) | DONE |

### appAction.toggleKeyboardLegend  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| toggleKeyboardLegend | (none) | Toggle keyboard legend | ACCEPT | DONE |

### appAction.toggleSahityaLine  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| toggleSahityaLine | (none) | Toggle sahitya line | ACCEPT | DONE |

### appAction.toggleStrokeLine  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| toggleStrokeLine | (none) | Toggle stroke line | ACCEPT | DONE |

### appAction.toggleTheme  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| toggleTheme | Toggle light / dark theme | (none) | PORT→web | DONE |

### appAction.undo  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| undo | (none) | Undo | ACCEPT | DONE |

### dialog.about  (23 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| betaNote | (none) | Beta release — actively iterating toward v1.0. Expect rou… | ACCEPT (override) | DONE |
| betaNote.desktop | Beta release — actively iterating toward v1.0. Expect rou… | (none) | PORT→web | DONE |
| copyright | (none) | © 2026 Bharadwaj.  | ACCEPT (override) | DONE |
| description.desktop.line1 | A notation editor for Hindustani classical music in the B… | (none) | PORT→web | DONE |
| description.desktop.line2 | Designed primarily for sitar compositions — Gat, Bandish,… | (none) | PORT→web | DONE |
| description.paragraph1 | (none) | A notation editor for Hindustani classical music in the B… | ACCEPT (override) | DONE |
| description.paragraph2 | (none) | Supports Devanagari, Kannada, Telugu, and English scripts. | ACCEPT (override) | DONE |
| license | (none) | Free and open source under the MIT License. | ACCEPT (override) | DONE |
| license.desktop | Free and open source. Copyright (c) 2026 Bharadwaj. | (none) | PORT→web | DONE |
| links.header | (none) | Links | ACCEPT (override) | DONE |
| links.selfHosting | (none) | Self-hosting guide | ACCEPT (override) | DONE |
| links.userGuide | (none) | User guide | ACCEPT (override) | DONE |
| links.userGuide.desktop | User guide & documentation | (none) | PORT→web | DONE |
| links.webVersion | Web version: {url} [1 param] | (none) | PORT→web | DONE |
| privacy.desktop | Anonymous usage stats (which features get touched, how lo… | (none) | PORT→web | DONE |
| privacy.header | (none) | Privacy | ACCEPT (override) | DONE |
| privacy.text | (none) | While you use the app, anonymous usage events (clicks, ke… | ACCEPT (override) | DONE |
| sampleToggle | Show sample composition on startup | (none) | PORT→web | DONE |
| support.link | (none) | Support the project | ACCEPT (override) | DONE |
| support.suffix | (none) |  — UPI / PayPal options. | ACCEPT (override) | DONE |
| support.text | (none) | 💖  | ACCEPT (override) | DONE |
| tech | (none) | Desktop: Scala 3 + ScalaFX. Web: Elm + Tapir. | ACCEPT (override) | DONE |
| tech.desktop | Built with Scala 3 + ScalaFX (desktop) and Elm + Tapir (web) | (none) | PORT→web | DONE |

### dialog.bugReport  (8 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| button.sentSuccess | Sent ✓ | (none) | ACCEPT (override) | DONE |
| disclosure.desktop | We'll include a short replay of recent keystrokes + a scr… | (none) | PORT→web | DONE |
| disclosure.web | (none) | We'll include a short replay of your recent actions in th… | ACCEPT (override) | DONE |
| status.screenshotFailed | Screenshot failed ({error}) — sending without it. [1 param] | (none) | PORT→web | DONE |
| status.sendFailed | Send failed: {error} [1 param] | (none) | PORT→web | DONE |
| status.sending | Sending report... | (none) | PORT→web | DONE |
| status.sendThrew | Send threw: {message} [1 param] | (none) | PORT→web | DONE |
| status.sent | Sent. Report id: {reportId} [1 param] | (none) | PORT→web | DONE |

### dialog.commandPalette  (4 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| noResults | (none) | No matching actions. | ACCEPT (override) | DONE |
| searchPlaceholder | Search actions… (Esc to close) | (none) | PORT→web | DONE |
| searchPlaceholderWeb | (none) | Search actions… (Esc to close, ↑↓ to navigate, Enter to run) | ACCEPT (override) | DONE |
| title | Command Palette | (none) | PORT→web | DONE |

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
| action.addSection | Add section | (none) | PORT→web | DONE |
| action.closeTab | Close tab | (none) | PORT→web | DONE |
| action.compositionProperties | Composition properties | (none) | PORT→web | DONE |
| action.copy | Copy | (none) | PORT→web | DONE |
| action.cut | Cut | (none) | PORT→web | DONE |
| action.cycleScript | Cycle notation script | (none) | PORT→web | DONE |
| action.exportHtml | Export HTML | (none) | PORT→web | DONE |
| action.newComposition | New composition | (none) | PORT→web | DONE |
| action.nextTab | Next tab | (none) | PORT→web | DONE |
| action.openFile | Open file | (none) | PORT→web | DONE |
| action.openFolder | Open folder | (none) | PORT→web | DONE |
| action.openUserGuide | Open user guide | (none) | PORT→web | DONE |
| action.paste | Paste | (none) | PORT→web | DONE |
| action.previousTab | Previous tab | (none) | PORT→web | DONE |
| action.redo | Redo | (none) | PORT→web | DONE |
| action.removeSection | Remove current section | (none) | PORT→web | DONE |
| action.renameSection | Rename current section | (none) | PORT→web | DONE |
| action.reportBug | Report a bug | (none) | PORT→web | DONE |
| action.save | Save | (none) | PORT→web | DONE |
| action.saveAs | Save as | (none) | PORT→web | DONE |
| action.showCheatSheet | Show this cheat sheet | (none) | PORT→web | DONE |
| action.toggleFileBrowser | Toggle file browser | (none) | PORT→web | DONE |
| action.toggleTheme | Toggle theme | (none) | PORT→web | DONE |
| action.undo | Undo | (none) | PORT→web | DONE |
| hint.desktopFull | (none) | -shortcuts wired (browsers reserve many of them on web). … | ACCEPT (override) | DONE |
| hint.keyboardRef | (none) | Keyboard Reference | ACCEPT (override) | DONE |
| hint.web | (none) | Tip: most toolbar actions are accessible via the buttons … | ACCEPT (override) | DONE |
| label.cancelOrnament | (none) | Cancel ornament mode | ACCEPT (override) | DONE |
| label.chikari | (none) | Chikari (open strings) | ACCEPT (override) | DONE |
| label.cutCopyPaste | (none) | Cut / Copy / Paste | ACCEPT (override) | DONE |
| label.daRaStrokes | (none) | Da (inward) / Ra (outward) | ACCEPT (override) | DONE |
| label.deleteEvent | (none) | Delete event | ACCEPT (override) | DONE |
| label.doubleTapDual | (none) | Double-tap dual swar | ACCEPT (override) | DONE |
| label.extendSelection | (none) | Extend selection | ACCEPT (override) | DONE |
| label.fastTyping | (none) | Type 2–4 notes within 500 ms to auto-group | ACCEPT (override) | DONE |
| label.finishOrnament | (none) | Finish multi-note ornament | ACCEPT (override) | DONE |
| label.gamakAndolan | (none) | Gamak / Andolan / Gitkari | ACCEPT (override) | DONE |
| label.kanSwar | (none) | Kan swar | ACCEPT (override) | DONE |
| label.komalRe | (none) | Komal Re / Ga / Dha / Ni | ACCEPT (override) | DONE |
| label.madhyaDefault | (none) | Madhya (default) | ACCEPT (override) | DONE |
| label.mandraLower | (none) | Mandra (lower) | ACCEPT (override) | DONE |
| label.meendDown | (none) | Meend ↓ | ACCEPT (override) | DONE |
| label.meendUp | (none) | Meend ↑ | ACCEPT (override) | DONE |
| label.moveCursor | (none) | Move cursor one beat | ACCEPT (override) | DONE |
| label.nextSubbeat | (none) | Next sub-beat | ACCEPT (override) | DONE |
| label.rest | (none) | Rest | ACCEPT (override) | DONE |
| label.setNotesPerBeat | (none) | Set notes per beat | ACCEPT (override) | DONE |
| label.showCheatSheet | (none) | Show this cheat sheet | ACCEPT (override) | DONE |
| label.shuddhaSwaras | (none) | Shuddha swaras | ACCEPT (override) | DONE |
| label.sparsh | (none) | Sparsh | ACCEPT (override) | DONE |
| label.sustain | (none) | Sustain | ACCEPT (override) | DONE |
| label.taarUpper | (none) | Taar (upper) | ACCEPT (override) | DONE |
| label.tivraMa | (none) | Tivra Ma | ACCEPT (override) | DONE |
| label.undoRedo | (none) | Undo / Redo | ACCEPT (override) | DONE |
| section.edit.desktop | Edit | (none) | PORT→web | DONE |
| section.file.desktop | File | (none) | PORT→web | DONE |
| section.help.desktop | Help | (none) | PORT→web | DONE |
| section.help.web | (none) | Help | ACCEPT (override) | DONE |
| section.navigation | (none) | Navigation | ACCEPT (override) | DONE |
| section.octave | (none) | Octave (saptak) | ACCEPT (override) | DONE |
| section.ornaments | (none) | Ornaments | ACCEPT (override) | DONE |
| section.sections.desktop | Sections | (none) | PORT→web | DONE |
| section.selectionClipboard | (none) | Selection & clipboard | ACCEPT (override) | DONE |
| section.strokes | (none) | Strokes | ACCEPT (override) | DONE |
| section.subdivisions | (none) | Subdivisions | ACCEPT (override) | DONE |
| section.swar | (none) | Swar (notes) | ACCEPT (override) | DONE |
| section.tabs.desktop | Tabs | (none) | PORT→web | DONE |
| section.view.desktop | View | (none) | PORT→web | DONE |
| subtitle.desktop | Full reference: Help → User Guide → Keyboard Reference | (none) | PORT→web | DONE |

### dialog.newComposition  (67 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| button.cancel | (none) | Cancel | ACCEPT (override) | DONE |
| button.create | (none) | Create | ACCEPT (override) | DONE |
| field.antaraStartingBeat.label | (none) | Antara Starting Beat (1-{matras}) [1 param] | ACCEPT (override) | DONE |
| field.antaraStartingBeat.labelDesktop | Antara Starting Beat: | (none) | PORT→web | DONE |
| field.arohan.label | Arohan: | (none) | PORT→web | DONE |
| field.arohan.placeholder | auto-detected or enter manually | (none) | PORT→web | DONE |
| field.avrohan.label | Avrohan: | (none) | PORT→web | DONE |
| field.avrohan.placeholder | auto-detected or enter manually | (none) | PORT→web | DONE |
| field.filePath.browseButton | Browse... | (none) | PORT→web | DONE |
| field.filePath.browserTitle | Save Composition As | (none) | PORT→web | DONE |
| field.filePath.label | Save to: | (none) | PORT→web | DONE |
| field.filePath.placeholder | Select location to save .swar file | (none) | PORT→web | DONE |
| field.gatStartingBeat.label | (none) | Gat Starting Beat (1-{matras}) [1 param] | ACCEPT (override) | DONE |
| field.gatStartingBeat.labelDesktop | Gat Starting Beat: | (none) | PORT→web | DONE |
| field.laya.atidrut | (none) | Ati-drut | ACCEPT (override) | DONE |
| field.laya.atidrutDesktop | Ati-Drut | (none) | PORT→web | DONE |
| field.laya.ativilambit | (none) | Ati-vilambit | ACCEPT (override) | DONE |
| field.laya.ativilambitDesktop | Ati-Vilambit | (none) | PORT→web | DONE |
| field.laya.label | (none) | Laya | ACCEPT (override) | DONE |
| field.laya.labelDesktop | Laya: | (none) | PORT→web | DONE |
| field.laya.none | (none) | None (Palta) | ACCEPT (override) | DONE |
| field.laya.noneDesktop | (none) | (none) | PORT→web | DONE |
| field.raag.label | (none) | Raag | ACCEPT (override) | DONE |
| field.raag.labelDesktop | Raag: | (none) | PORT→web | DONE |
| field.raag.placeholder | Type to search or enter custom raag | (none) | PORT→web | DONE |
| field.samvadi.label | Samvadi: | (none) | PORT→web | DONE |
| field.samvadi.placeholder | auto-detected | (none) | PORT→web | DONE |
| field.script.label | Script: | (none) | PORT→web | DONE |
| field.showSahitya.checkboxDesktop | Show lyrics row below swar | (none) | PORT→web | DONE |
| field.showSahitya.label | (none) | Show Sahitya Line (Lyrics) | ACCEPT (override) | DONE |
| field.showSahitya.labelDesktop | Sahitya line: | (none) | PORT→web | DONE |
| field.showStrokes.checkboxDesktop | Show Da/Ra stroke indicators below swar | (none) | PORT→web | DONE |
| field.showStrokes.label | (none) | Show Stroke Line (Da/Ra) | ACCEPT (override) | DONE |
| field.showStrokes.labelDesktop | Stroke line: | (none) | PORT→web | DONE |
| field.sthayiStartingBeat.label | (none) | Sthayi Starting Beat (1-{matras}) [1 param] | ACCEPT (override) | DONE |
| field.sthayiStartingBeat.labelDesktop | Sthayi Starting Beat: | (none) | PORT→web | DONE |
| field.taal.label | (none) | Taal | ACCEPT (override) | DONE |
| field.taal.labelDesktop | Taal: | (none) | PORT→web | DONE |
| field.taanCount.label | (none) | Taan Count | ACCEPT (override) | DONE |
| field.taanCount.labelDesktop | Taans: | (none) | PORT→web | DONE |
| field.taanStartingBeat.label | (none) | Taan Starting Beat (1-{matras}) [1 param] | ACCEPT (override) | DONE |
| field.taanStartingBeat.labelDesktop | Taan Starting Beat: | (none) | PORT→web | DONE |
| field.thaat.label | Thaat: | (none) | PORT→web | DONE |
| field.thaat.placeholder | auto-detected or enter manually | (none) | PORT→web | DONE |
| field.title.label | (none) | Title | ACCEPT (override) | DONE |
| field.title.labelDesktop | Title: | (none) | PORT→web | DONE |
| field.title.placeholder | (none) | Enter composition title | ACCEPT (override) | DONE |
| field.title.placeholderDesktop | e.g. Yaman Vilambit Gat | (none) | PORT→web | DONE |
| field.type.bandish | (none) | Bandish (Vocal) | ACCEPT (override) | DONE |
| field.type.bandishDesktop | Bandish | (none) | PORT→web | DONE |
| field.type.gat | (none) | Gat (Instrumental) | ACCEPT (override) | DONE |
| field.type.gatDesktop | Gat | (none) | PORT→web | DONE |
| field.type.label | (none) | Type | ACCEPT (override) | DONE |
| field.type.labelDesktop | Type: | (none) | PORT→web | DONE |
| field.type.palta | (none) | Palta (Practice) | ACCEPT (override) | DONE |
| field.type.paltaDesktop | Palta | (none) | PORT→web | DONE |
| field.type.sargam | (none) | Sargam (Practice) | ACCEPT (override) | DONE |
| field.type.sargamDesktop | Sargam | (none) | PORT→web | DONE |
| field.vadi.label | Vadi: | (none) | PORT→web | DONE |
| field.vadi.placeholder | auto-detected | (none) | PORT→web | DONE |
| header | Create a new composition | (none) | PORT→web | DONE |
| raagDetected | Raag {name} recognized [1 param] | (none) | PORT→web | DONE |
| raagNotFound | (raag not in database -- enter details manually) | (none) | PORT→web | DONE |
| validation.filePathRequired | File path is required | (none) | PORT→web | DONE |
| validation.layaRequired | Laya is required for Gat | (none) | PORT→web | DONE |
| validation.raagRequired | Raag is required | (none) | PORT→web | DONE |
| validation.titleRequired | Title is required | (none) | PORT→web | DONE |

### dialog.properties  (16 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| button.cancel | (none) | Cancel | ACCEPT (override) | DONE |
| button.save | (none) | Save | ACCEPT (override) | DONE |
| field.antaraStartingBeat.labelDesktop | Antara Starting Beat: | (none) | PORT→web | DONE |
| field.gatStartingBeat.labelDesktop | Gat Starting Beat: | (none) | PORT→web | DONE |
| field.raag.label | Raag: | (none) | PORT→web | DONE |
| field.sectionStartingBeat.label | (none) | {name} Starting Beat (1-{matras}) [2 params] | ACCEPT (override) | DONE |
| field.sthayiStartingBeat.labelDesktop | Sthayi Starting Beat: | (none) | PORT→web | DONE |
| field.taal.label | (none) | Taal | ACCEPT (override) | DONE |
| field.taal.labelDesktop | Taal: | (none) | PORT→web | DONE |
| field.taanStartingBeat.labelDesktop | Taan Starting Beat: | (none) | PORT→web | DONE |
| field.title.label | (none) | Title | ACCEPT (override) | DONE |
| field.title.labelDesktop | Title: | (none) | PORT→web | DONE |
| field.title.placeholder | (none) | Composition title | ACCEPT (override) | DONE |
| field.type.label | Type: | (none) | PORT→web | DONE |
| header | Edit composition details | (none) | PORT→web | DONE |
| validation.beatsClamped | Starting beats clamped to new taal range (1-{matras}) [1 … | (none) | PORT→web | DONE |

### dialog.support  (8 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| international.paypalLink | (none) | Support via PayPal | ACCEPT (override) | DONE |
| international.platformLink | Support via {platform} [1 param] | (none) | PORT→web | DONE |
| upi.handle | (none) | bharath12345-1@oksbi | ACCEPT (override) | DONE |
| upi.handleLabel | (none) | UPI handle:  | ACCEPT (override) | DONE |
| upi.handleLabelWithValue | UPI handle: {handle} [1 param] | (none) | PORT→web | DONE |
| upi.qrAlt | (none) | UPI QR code | ACCEPT (override) | DONE |
| upi.qrPlaceholder | (QR code image will appear here) | (none) | PORT→web | DONE |
| windowTitle | Support — Sangeet Notes Editor | (none) | PORT→web | DONE |

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
| bookmarkTooltip | (none) | Bookmark | ACCEPT | DONE |

### fileBrowser.connectDrive  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| connectDrive | (none) | Connect Google Drive | ACCEPT | DONE |

### fileBrowser.connecting  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| connecting | (none) | Connecting... | ACCEPT | DONE |

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
| deleteTooltip | (none) | Delete | ACCEPT | DONE |

### fileBrowser.driveConnected  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| driveConnected | (none) | Drive connected | ACCEPT | DONE |

### fileBrowser.emptyState  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| emptyState | (none) | Connect Drive to browse files | ACCEPT | DONE |

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
| hideFilesTooltip | (none) | Hide Files | ACCEPT | DONE |

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
| defaultName | (none) | New Folder | ACCEPT | DONE |

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
| refreshTooltip | (none) | Refresh | ACCEPT | DONE |

### fileBrowser.removeBookmarkTooltip  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| removeBookmarkTooltip | (none) | Remove bookmark | ACCEPT | DONE |

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
| showFilesTooltip | (none) | Show Files | ACCEPT | DONE |

### header.beatPrefix  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| beatPrefix | (none) | Beat  | ACCEPT | DONE |

### header.cyclePrefix  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| cyclePrefix | (none) | Cycle  | ACCEPT | DONE |

### header.modeLabel  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| modeLabel | (none) | Mode:  | ACCEPT | DONE |

### header.modeStroke  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| modeStroke | (none) | Stroke | ACCEPT | DONE |

### header.modeSwar  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| modeSwar | (none) | Swar | ACCEPT | DONE |

### header.octaveAtiMandra  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| octaveAtiMandra | (none) | Ati-Mandra | ACCEPT | DONE |

### header.octaveAtiTaar  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| octaveAtiTaar | (none) | Ati-Taar | ACCEPT | DONE |

### header.octaveLabel  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| octaveLabel | (none) | Octave:  | ACCEPT | DONE |

### header.octaveMadhya  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| octaveMadhya | (none) | Madhya | ACCEPT | DONE |

### header.octaveMandra  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| octaveMandra | (none) | Mandra | ACCEPT | DONE |

### header.octaveTaar  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| octaveTaar | (none) | Taar | ACCEPT | DONE |

### header.samvadiLabel  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| samvadiLabel | Samvadi | (none) | PORT→web | DONE |

### header.subPrefix  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| subPrefix | (none) | Sub  | ACCEPT | DONE |

### header.thaatLabel  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| thaatLabel | Thaat | (none) | PORT→web | DONE |

### header.vadiLabel  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| vadiLabel | Vadi | (none) | PORT→web | DONE |

### keyboardLegend.nav  (6 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| backspace.web | (none) | Delete last | ACCEPT (override) | DONE |
| enter | Next cycle | (none) | PORT→web | DONE |
| moveCursor | Move cursor | (none) | PORT→web | DONE |
| prevNextBeat.web | (none) | Previous / Next beat | ACCEPT (override) | DONE |
| tab.desktop | Next beat | (none) | PORT→web | DONE |
| tab.web | (none) | Next sub-beat | ACCEPT (override) | DONE |

### keyboardLegend.octave  (6 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| backToMadhya | Back to madhya | (none) | PORT→web | DONE |
| madhya.web | (none) | Madhya (middle) | ACCEPT (override) | DONE |
| mandra.desktop | Next note in mandra | (none) | PORT→web | DONE |
| mandra.web | (none) | Mandra (lower) | ACCEPT (override) | DONE |
| taar.desktop | Next note in taar | (none) | PORT→web | DONE |
| taar.web | (none) | Taar (upper) | ACCEPT (override) | DONE |

### keyboardLegend.ornamentKeys  (3 entries)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| multiNote | ..↵ = type notes, press Enter | (none) | PORT→web | DONE |
| oneNote | ♪  = type one swar key | (none) | PORT→web | DONE |
| twoNotes | ♪♪ = type start, then end note | (none) | PORT→web | DONE |

### keyboardLegend.ornaments  (22 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| andolan | (none) | Andolan | ACCEPT (override) | DONE |
| andolan.desktop | Andolan (gentle oscillation) | (none) | PORT→web | DONE |
| gamak | (none) | Gamak | ACCEPT (override) | DONE |
| gamak.desktop | Gamak (heavy oscillation) | (none) | PORT→web | DONE |
| ghaseet | (none) | Ghaseet (then type note) | ACCEPT (override) | DONE |
| ghaseet.desktop | Ghaseet (heavy pull) | (none) | PORT→web | DONE |
| gitkari | (none) | Gitkari | ACCEPT (override) | DONE |
| gitkari.desktop | Gitkari (hammer/pull trill) | (none) | PORT→web | DONE |
| kan | (none) | Kan Swar (then type note) | ACCEPT (override) | DONE |
| kan.desktop | Kan Swar (grace note) | (none) | PORT→web | DONE |
| krintan.desktop | Krintan (pull-off seq.) | (none) | PORT→web | DONE |
| krintan.web | (none) | Krintan (type notes, Enter) | ACCEPT (override) | DONE |
| meendAsc | (none) | Meend Asc (type start, end) | ACCEPT (override) | DONE |
| meendAsc.desktop | Meend ↑ (ascending glide) | (none) | PORT→web | DONE |
| meendDesc | (none) | Meend Desc | ACCEPT (override) | DONE |
| meendDesc.desktop | Meend ↓ (descending glide) | (none) | PORT→web | DONE |
| murki | (none) | Murki (type notes, Enter) | ACCEPT (override) | DONE |
| murki.desktop | Murki (ornamental turn) | (none) | PORT→web | DONE |
| sparsh | (none) | Sparsh (then type note) | ACCEPT (override) | DONE |
| sparsh.desktop | Sparsh (light touch) | (none) | PORT→web | DONE |
| zamzama | (none) | Zamzama (type notes, Enter) | ACCEPT (override) | DONE |
| zamzama.desktop | Zamzama (rapid cluster) | (none) | PORT→web | DONE |

### keyboardLegend.redo  (2 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| desktop | Redo | (none) | PORT→web | DONE |
| web | (none) | Redo | ACCEPT (override) | DONE |

### keyboardLegend.scriptLabel  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| scriptLabel | Script: {scriptName} | (none) | PORT→web | DONE |

### keyboardLegend.section  (14 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| ornamentKeys | Ornament Keys | (none) | PORT→web | DONE |
| ornaments | (none) | Ornaments (Alt+key) | ACCEPT (override) | DONE |
| ornamentsMultiNote | Ornaments -- Multi-Note | (none) | PORT→web | DONE |
| ornamentsOneNote | Ornaments -- One Note | (none) | PORT→web | DONE |
| ornamentsSimple | Ornaments -- Simple | (none) | PORT→web | DONE |
| ornamentsTwoNotes | Ornaments -- Two Notes | (none) | PORT→web | DONE |
| strokes | (none) | Strokes | ACCEPT (override) | DONE |
| strokesMizrab | Strokes (Mizrab) | (none) | PORT→web | DONE |
| subdivisions | Subdivisions | (none) | PORT→web | DONE |
| swarInput | (none) | Swar Input | ACCEPT (override) | DONE |
| swarNotes | Swar (Notes) | (none) | PORT→web | DONE |
| tips | Tips | (none) | PORT→web | DONE |
| undoRedo | (none) | Undo/Redo | ACCEPT (override) | DONE |
| undoRedoDesktop | Undo / Redo | (none) | PORT→web | DONE |

### keyboardLegend.special  (4 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| deleteLast | Delete last note | (none) | PORT→web | DONE |
| rest | Rest (silence) | (none) | PORT→web | DONE |
| subdivisions | (none) | Set subdivisions per beat | ACCEPT (override) | DONE |
| sustain | Sustain (hold) | (none) | PORT→web | DONE |

### keyboardLegend.strokes  (4 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| da | Da (inward stroke) | (none) | PORT→web | DONE |
| keys.web | (none) | Da / Ra / Jod (in stroke mode) | ACCEPT (override) | DONE |
| ra | Ra (outward stroke) | (none) | PORT→web | DONE |
| toggleMode.web | (none) | Toggle Swar/Stroke mode | ACCEPT (override) | DONE |

### keyboardLegend.subdivisions  (2 entries)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| doubleTap | Double-tap for dual swar | (none) | PORT→web | DONE |
| setPerBeat | Set notes per beat (2-8) | (none) | PORT→web | DONE |

### keyboardLegend.swar  (6 entries)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| dualSwar | (none) | Dual swar (double-tap) | ACCEPT | DONE |
| komal | (none) | Komal variants | ACCEPT | DONE |
| rest | (none) | Rest | ACCEPT | DONE |
| shuddha | (none) | Shuddha notes | ACCEPT | DONE |
| sustain | (none) | Sustain | ACCEPT | DONE |
| tivraMa | (none) | Tivra Ma | ACCEPT | DONE |

### keyboardLegend.tips  (3 entries)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| applyToLast | Strokes & ornaments apply to the last entered note | (none) | PORT→web | DONE |
| octaveReset | . and ' affect only the next note, then reset to madhya | (none) | PORT→web | DONE |
| shiftVariant | Shift = komal/tivra variant | (none) | PORT→web | DONE |

### keyboardLegend.title  (2 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| desktop | Keyboard Reference | (none) | PORT→web | DONE |
| web | (none) | Keyboard Shortcuts | ACCEPT (override) | DONE |

### mainApp.openFolderDialogTitle  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| openFolderDialogTitle | Open Folder | (none) | ACCEPT (override) | DONE |

### status.apiError  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| apiError | (none) | API error: {message} | ACCEPT | DONE |

### status.badBody  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| badBody | (none) | Bad body: {error} | ACCEPT | DONE |

### status.badStatus  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| badStatus | (none) | Bad status: {code} | ACCEPT | DONE |

### status.badUrl  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| badUrl | (none) | Bad URL: {url} | ACCEPT | DONE |

### status.bugReportFailed  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| bugReportFailed | (none) | Bug report failed: {message} | ACCEPT | DONE |

### status.bugReportSent  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| bugReportSent | (none) | Bug report sent — thanks! ({message}) | ACCEPT | DONE |

### status.clipboardEmpty  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| clipboardEmpty | Clipboard is empty | (none) | PORT→web | DONE |

### status.clipboardNotSangeetData  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| clipboardNotSangeetData | Clipboard does not contain Sangeet data | (none) | PORT→web | DONE |

### status.closedTabSwitched  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| closedTabSwitched | (none) | Closed tab, switched to {filename} | ACCEPT | DONE |

### status.colorsLoaded  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| colorsLoaded | (none) | Colors loaded | ACCEPT | DONE |

### status.connectedToDrive  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| connectedToDrive | (none) | Connected to Google Drive | ACCEPT | DONE |

### status.copiedEvents  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| copiedEvents | Copied {count} event(s) | (none) | PORT→web | DONE |

### status.created  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| created | (none) | Created: {title} | ACCEPT | DONE |

### status.cursorPlaced  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| cursorPlaced | Cursor placed at cycle {cycle}, beat {beat} | (none) | ACCEPT (override) | DONE |

### status.cutEvents  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| cutEvents | Cut {count} event(s) | (none) | PORT→web | DONE |

### status.driveAuthFailed  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| driveAuthFailed | (none) | Drive authentication failed | ACCEPT | DONE |

### status.driveError  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| driveError | (none) | Drive error: {message} | ACCEPT | DONE |

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
| exportingHtml | (none) | Exporting HTML... | ACCEPT | DONE |

### status.failedToParseDriveFileContent  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| failedToParseDriveFileContent | (none) | Failed to parse Drive file content | ACCEPT | DONE |

### status.failedToParseDriveFolderListing  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| failedToParseDriveFolderListing | (none) | Failed to parse Drive folder listing | ACCEPT | DONE |

### status.fileSavedToDrive  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| fileSavedToDrive | (none) | File saved to Drive | ACCEPT | DONE |

### status.fileSelected  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| fileSelected | (none) | File selected: {filename} | ACCEPT | DONE |

### status.fileWasDeleted  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| fileWasDeleted | File was deleted: {title} | (none) | ACCEPT (override) | DONE |

### status.httpError  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| httpError | (none) | HTTP error: {message} | ACCEPT | DONE |

### status.lastTabClosedNewCreated  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| lastTabClosedNewCreated | (none) | Last tab closed — new tab created | ACCEPT | DONE |

### status.loadedRaags  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| loadedRaags | (none) | Loaded {count} raags | ACCEPT | DONE |

### status.loadedTaals  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| loadedTaals | (none) | Loaded {count} taals | ACCEPT | DONE |

### status.loadingFileFromDrive  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| loadingFileFromDrive | (none) | Loading file from Drive: {filename} | ACCEPT | DONE |

### status.networkError  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| networkError | (none) | Network error | ACCEPT | DONE |

### status.newTab  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| newTab | (none) | New tab | ACCEPT | DONE |

### status.noEventsInSelection  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| noEventsInSelection | No events in selection | (none) | PORT→web | DONE |

### status.noSelection  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| noSelection | No selection | (none) | PORT→web | DONE |

### status.noSelectionToCopy  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| noSelectionToCopy | (none) | No selection to copy | ACCEPT | DONE |

### status.noSelectionToCut  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| noSelectionToCut | (none) | No selection to cut | ACCEPT | DONE |

### status.nothingToRedo  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| nothingToRedo | (none) | Nothing to redo | ACCEPT | DONE |

### status.nothingToUndo  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| nothingToUndo | (none) | Nothing to undo | ACCEPT | DONE |

### status.opened  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| opened | (none) | Opened: {title} | ACCEPT | DONE |

### status.openedDesktop  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| openedDesktop | Opened: {filename} | (none) | PORT→web | DONE |

### status.openingFromDrive  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| openingFromDrive | (none) | Opening from Drive: {filename} | ACCEPT | DONE |

### status.ornamentCancelled  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| ornamentCancelled | (none) | Ornament mode cancelled | ACCEPT | DONE |

### status.ornamentCollecting  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| ornamentCollecting | (none) | Collecting ornament notes... | ACCEPT | DONE |

### status.ornamentGhaseet  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| ornamentGhaseet | (none) | Ghaseet: type the target note | ACCEPT | DONE |

### status.ornamentKanSwar  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| ornamentKanSwar | (none) | Kan Swar: type the grace note | ACCEPT | DONE |

### status.ornamentKrintan  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| ornamentKrintan | (none) | Krintan: type notes, then Enter | ACCEPT | DONE |

### status.ornamentMeendAsc  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| ornamentMeendAsc | (none) | Meend (ascending): type start note | ACCEPT | DONE |

### status.ornamentMeendDesc  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| ornamentMeendDesc | (none) | Meend (descending): type start note | ACCEPT | DONE |

### status.ornamentMurki  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| ornamentMurki | (none) | Murki: type notes, then Enter | ACCEPT | DONE |

### status.ornamentSparsh  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| ornamentSparsh | (none) | Sparsh: type the touch note | ACCEPT | DONE |

### status.ornamentZamzama  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| ornamentZamzama | (none) | Zamzama: type notes, then Enter | ACCEPT | DONE |

### status.pastedEvents  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| pastedEvents | Pasted {count} event(s) | (none) | PORT→web | DONE |

### status.pleaseSelectValidTaalRaag  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| pleaseSelectValidTaalRaag | (none) | Please select a valid taal and raag | ACCEPT | DONE |

### status.preview  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| preview | Preview: {filename} | (none) | ACCEPT (override) | DONE |

### status.propertiesUpdatedTaal  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| propertiesUpdatedTaal | (none) | Properties updated — taal: {taalName} | ACCEPT | DONE |

### status.propertiesUpdatedTaalNotFound  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| propertiesUpdatedTaalNotFound | (none) | Properties updated (taal not found, kept previous) | ACCEPT | DONE |

### status.redo  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| redo | (none) | Redo | ACCEPT | DONE |

### status.reloaded  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| reloaded | Reloaded: {filename} | (none) | ACCEPT (override) | DONE |

### status.requestTimeout  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| requestTimeout | (none) | Request timed out | ACCEPT | DONE |

### status.sahityaLineHidden  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| sahityaLineHidden | (none) | Sahitya line hidden | ACCEPT | DONE |

### status.sahityaLineShown  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| sahityaLineShown | (none) | Sahitya line shown | ACCEPT | DONE |

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
| savingComposition | (none) | Saving composition... | ACCEPT | DONE |

### status.scriptChanged  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| scriptChanged | (none) | Script changed to {scriptName} | ACCEPT | DONE |

### status.sectionAdded  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| sectionAdded | (none) | Section added | ACCEPT | DONE |

### status.sectionRemoved  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| sectionRemoved | (none) | Section removed | ACCEPT | DONE |

### status.sectionRenamed  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| sectionRenamed | (none) | Section renamed | ACCEPT | DONE |

### status.sectionsReordered  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| sectionsReordered | (none) | Sections reordered | ACCEPT | DONE |

### status.startingBeatsUpdated  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| startingBeatsUpdated | (none) | Starting beats updated | ACCEPT | DONE |

### status.strokeLineHidden  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| strokeLineHidden | (none) | Stroke line hidden | ACCEPT | DONE |

### status.strokeLineShown  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| strokeLineShown | (none) | Stroke line shown | ACCEPT | DONE |

### status.switchedToSection  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| switchedToSection | (none) | Switched to section {number} | ACCEPT | DONE |

### status.switchedToSectionDesktop  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| switchedToSectionDesktop | Switched to section: {name} | (none) | PORT→web | DONE |

### status.switchedToTab  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| switchedToTab | (none) | Switched to {filename} | ACCEPT | DONE |

### status.tabClosed  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| tabClosed | (none) | Tab closed | ACCEPT | DONE |

### status.undo  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| undo | (none) | Undo | ACCEPT | DONE |

### statusBar.logLabel  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| logLabel | Log | (none) | ACCEPT (override) | DONE |

### toolbar.edit  (4 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| redo | (none) | Redo | ACCEPT (override) | DONE |
| redo.tooltip | (none) | Redo (Ctrl+Y) | ACCEPT (override) | DONE |
| redo.tooltip.desktop | Redo (Ctrl+Shift+Z) | (none) | PORT→web | DONE |
| undo | (none) | Undo | ACCEPT (override) | DONE |

### toolbar.file  (6 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| exportHtml | (none) | HTML | ACCEPT (override) | DONE |
| new | (none) | New | ACCEPT (override) | DONE |
| open | (none) | Open | ACCEPT (override) | DONE |
| openFolder.tooltip | Open a folder in the file browser | (none) | ACCEPT (override) | DONE |
| save | (none) | Save | ACCEPT (override) | DONE |
| saveAs.tooltip | Save composition as a new .swar file | (none) | ACCEPT (override) | DONE |

### toolbar.help  (8 entries)

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| about | (none) | About | ACCEPT (override) | DONE |
| keyboardShortcuts | (none) | ? | ACCEPT (override) | DONE |
| properties | (none) | Properties | ACCEPT (override) | DONE |
| reportBug | (none) | 🐞 Report bug | ACCEPT (override) | DONE |
| reportBug.tooltip | (none) | Report a bug — includes a short replay so it can be repro… | ACCEPT (override) | DONE |
| reportBug.tooltip.desktop | Report a bug — includes a screenshot + recent keystrokes … | (none) | PORT→web | DONE |
| support | (none) | 💖 | ACCEPT (override) | DONE |
| userGuide.tooltip | Open the user guide (F1) | (none) | PORT→web | DONE |

### toolbar.mode  (2 entries)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| stroke | (none) | Mode: Stroke | ACCEPT | DONE |
| swar | (none) | Mode: Swar | ACCEPT | DONE |

### toolbar.ornament  (7 entries)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| krintanEnd | (none) | Krintan: type end note / Enter | ACCEPT | DONE |
| krintanStart | (none) | Krintan: type start note | ACCEPT | DONE |
| meendEnd | (none) | Meend: type end note | ACCEPT | DONE |
| meendStart | (none) | Meend: type start note | ACCEPT | DONE |
| murki | (none) | Murki: {count} notes (Enter to apply) [1 param] | ACCEPT | DONE |
| singleNote | (none) | Orn: {name} (type note) [1 param] | ACCEPT | DONE |
| zamzama | (none) | Zamzama: {count} notes (Enter to apply) [1 param] | ACCEPT | DONE |

### toolbar.tabs  (2 entries)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| close.tooltip | (none) | Close tab | ACCEPT | DONE |
| new.tooltip | (none) | New Tab | ACCEPT | DONE |

### toolbar.theme  (1 entry)

*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| toggle.tooltip | Toggle light / dark theme | (none) | PORT→web | DONE |

### toolbar.view  (6 entries)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| toggleKeyboardLegend | (none) | Keys | ACCEPT | DONE |
| toggleKeyboardLegend.tooltip | (none) | Keyboard Shortcuts | ACCEPT | DONE |
| toggleSahityaLine | (none) | Sahitya | ACCEPT | DONE |
| toggleSahityaLine.tooltip | (none) | Toggle Sahitya Line | ACCEPT | DONE |
| toggleStrokeLine | (none) | Strokes | ACCEPT | DONE |
| toggleStrokeLine.tooltip | (none) | Toggle Stroke Line | ACCEPT | DONE |

### view.loading  (1 entry)

*(All entries are web-only architectural — consider bulk ACCEPT.)*

| Concept | Desktop | Web | Suggest | Status |
| ------- | ------- | --- | ------- | ------ |
| loading | (none) | Loading... | ACCEPT | DONE |

