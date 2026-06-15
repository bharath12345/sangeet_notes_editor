# Cross-Platform Feature Inventory

This is the canonical ground truth for desktop vs web feature parity. Each table reconciles what exists on `origin/main` today. Known gaps that are being closed by in-flight PRs are noted with the PR number.

Last updated: 2026-06-15 (plan-17 PR-7 baseline)

---

## Toolbar Items — Main Toolbar

| Action | Desktop | Web | Notes |
|---|---|---|---|
| New | ✓ button | ✓ button | |
| Open | ✓ button | ✓ button | |
| Save | ✓ button | ✓ button | |
| Save As | ✓ button | ✓ button | |
| Cut | ✓ button | ✓ button | |
| Copy | ✓ button | ✓ button | |
| Paste | ✓ button | ✓ button | |
| Export HTML | ✓ button | ✓ button | |
| Undo | ✗ | ✓ button | **gap — desktop has keybinding only, no toolbar button** |
| Redo | ✗ | ✓ button | **gap — desktop has keybinding only, no toolbar button** |
| Edit Mode Indicator | ✗ | ✓ label ("mode: swar" / "mode: stroke") | **gap — plan-17 bug 8, to be removed in PR-6** |
| Ornament Mode Badge | ✗ | ✓ badge | **gap — desktop has no ornament-mode status indicator** |
| Properties | ✓ button | ✓ button | |
| Report Bug | ✓ button | ✓ button | |
| User Guide | ✓ button | ✓ button | |
| Support / Donate | ✓ button | ✓ button | |
| Keyboard Shortcuts | ✓ button | ✓ button | |
| About | ✓ button | ✓ button | |
| Script Selector | ✓ combo | ✓ combo | |
| Theme Toggle | ✓ button | ✓ button | Added in plan-16 follow-ups |
| Open Folder | ✓ button | ✗ | conscious asymmetry — web has no filesystem sidebar |
| Cheat Sheet | ✓ button | (implicit via kbd shortcuts btn) | Both open similar modals |

## Toolbar Items — Section Toolbar (bottom row)

| Action | Desktop | Web | Notes |
|---|---|---|---|
| Section tabs (clickable) | ✓ tabs | ✓ tabs | |
| Add Section (+) | ✓ button | ✓ button | |
| Rename Section | ✓ button | ✓ button | |
| Remove Section | ✓ button | ✓ button | |
| Move Section Up | ✗ | ✓ button | **gap — desktop missing up/down section reorder buttons** |
| Move Section Down | ✗ | ✓ button | **gap — desktop missing up/down section reorder buttons** |

## Toolbar Items — File Browser (desktop-only sidebar)

| Action | Desktop | Web | Notes |
|---|---|---|---|
| Collapse file browser | ✓ button | ✗ | conscious asymmetry — web has no file browser yet |
| Directory tree | ✓ tree view | ✗ | conscious asymmetry |

## Dialogs — Inventory

| Dialog | Desktop | Web | Notes |
|---|---|---|---|
| New Composition | ✓ | ✓ | See field comparison below |
| Properties | ✓ | ✓ | See field comparison below |
| About | ✓ | ✓ | |
| Support / Donate | ✓ | ✓ | |
| Bug Report | ✓ | ✓ | |
| Keyboard Cheat Sheet | ✓ | ✓ | |
| Command Palette | ✓ | ✗ | **gap — web missing Cmd+K palette** |
| Unsaved Changes | ✓ | ✓ | |
| Duplicate Tab | ✓ | ✓ | |
| Crash Recovery | ✓ | ✗ | conscious asymmetry — browser has no next-launch hook |

## Dialog Fields — New Composition

| Field | Desktop | Web | Notes |
|---|---|---|---|
| Title | ✓ text input | ✓ text input | |
| Composition Type | ✓ combo | ✓ combo | |
| Raag | ✓ editable combo w/ autocomplete | ✓ select | desktop supports type-to-filter |
| Taal | ✓ combo | ✓ combo | |
| Laya | ✓ combo | ✓ combo | |
| Script | ✓ combo | ✗ | **gap — plan-17 bug 6, web missing script picker** |
| Taan Count | ✓ spinner | ✓ number input | |
| Show Strokes | ✓ checkbox | ✗ | **gap — plan-17 bug 6, web missing** |
| Show Sahitya | ✓ checkbox | ✗ | **gap — plan-17 bug 6, web missing** |
| File Path | ✓ text + browse button | ✗ | **gap — plan-17 bug 6, web missing** |
| Gat Starting Beat | ✓ spinner | ✓ number input | Gat/Bandish only |
| Antara Starting Beat | ✓ spinner | ✓ number input | Gat/Bandish only |
| Taan Starting Beat | ✓ spinner | ✓ number input | Gat only |
| Thaat | ✓ text (custom raag) | ✗ | **gap — plan-17 bug 6, web missing** |
| Arohan | ✓ text (custom raag) | ✗ | **gap — plan-17 bug 6, web missing** |
| Avrohan | ✓ text (custom raag) | ✗ | **gap — plan-17 bug 6, web missing** |
| Vadi | ✓ text (custom raag) | ✗ | **gap — plan-17 bug 6, web missing** |
| Samvadi | ✓ text (custom raag) | ✗ | **gap — plan-17 bug 6, web missing** |

## Dialog Fields — Properties

| Field | Desktop | Web | Notes |
|---|---|---|---|
| Title | ✓ text | ✓ text | |
| Raag | ✓ combo | ✓ combo | |
| Taal | ✓ combo | ✓ combo | |
| Laya | ✓ combo | ✓ combo | |
| Section Starting Beats | ✓ per-section spinner | ✓ per-section number | |

## Validation Guards — New Composition Dialog

| Guard | Desktop | Web | Notes |
|---|---|---|---|
| Title required (non-empty) | ✓ | ✗ | **gap — plan-17 bug 7, web allows empty title** |
| Laya required for Gat/Bandish | ✓ | ✗ | **gap — plan-17 bug 7, web allows None laya for Gat** |
| File path required | ✓ | n/a | web auto-downloads so no file path picker |
| Starting beats in [1, matras] | ✓ | ✓ | both enforce via spinner/number min/max |

## Validation Guards — Properties Dialog

| Guard | Desktop | Web | Notes |
|---|---|---|---|
| Title required (non-empty) | ✓ | ✓ | |
| Starting beats in [1, matras] | ✓ | ✓ | |

## Keyboard Shortcuts — Global (file/app scope)

| Key | Action | Desktop | Web | Notes |
|---|---|---|---|---|
| Cmd/Ctrl+N | New Composition | ✓ | ✗ | web: browser-reserved, palette-only |
| Cmd/Ctrl+O | Open File | ✓ | ✗ | web: browser-reserved, palette-only |
| Cmd/Ctrl+Shift+O | Open Folder | ✓ | ✗ | conscious asymmetry — no web folder browser |
| Cmd/Ctrl+S | Save | ✓ | ✗ | web: browser-reserved, palette-only |
| Cmd/Ctrl+Shift+S | Save As | ✓ | ✗ | web: browser-reserved, palette-only |
| Cmd/Ctrl+E | Export HTML | ✓ | ✗ | web: no global keybinding (palette-only) |
| Cmd/Ctrl+W | Close Tab | ✓ | ✗ | conscious asymmetry — browser closes tab natively |
| Cmd/Ctrl+Tab | Next Tab | ✓ | ✗ | conscious asymmetry — browser switches tabs natively |
| Cmd/Ctrl+Shift+Tab | Previous Tab | ✓ | ✗ | conscious asymmetry — browser switches tabs natively |
| Cmd/Ctrl+, | Properties | ✓ | ✗ | web: no keybinding (toolbar-only) |
| Cmd/Ctrl+Shift+A | Add Section | ✓ | ✗ | web: no keybinding (+ button only) |
| F2 | Rename Section | ✓ | ✗ | web: no keybinding (rename button only) |
| Cmd/Ctrl+Shift+Backspace | Remove Section | ✓ | ✗ | web: no keybinding (✕ button only) |
| Cmd/Ctrl+B | Toggle File Browser | ✓ | ✗ | conscious asymmetry — no web file browser |
| Cmd/Ctrl+Shift+T | Toggle Theme | ✓ | ✗ | web: browser-reserved for reopen-tab; toolbar-only |
| Cmd/Ctrl+Shift+L | Cycle Script | ✓ | ✗ | web: no keybinding (dropdown-only) |
| F1 | User Guide | ✓ | ✗ | web: browser-reserved for Help |
| ? | Keyboard Cheat Sheet | ✓ | ✗ | web: no single-key binding (toolbar-only) |
| Cmd/Ctrl+Shift+B | Report Bug | ✓ | ✗ | web: browser-reserved for bookmarks bar |
| Cmd/Ctrl+K | Command Palette | ✓ | ✗ | **gap — web missing command palette entirely** |

## Keyboard Shortcuts — Editor Scope (swar/navigation/editing)

| Key | Action | Desktop | Web | Notes |
|---|---|---|---|---|
| s, r, g, m, p, d, n | Insert swar (shuddha) | ✓ | ✓ | |
| Shift+R, G, D, N | Insert swar (komal) | ✓ | ✓ | |
| Shift+M | Insert swar (tivra Ma) | ✓ | ✓ | |
| - | Insert rest | ✓ | ✓ | |
| = | Insert sustain | ✓ | ✓ | |
| 1 | Insert chikari | ✓ | ✓ | |
| 2-8 | Set subdivision | ✓ | ✓ | |
| [, ], \\ | Octave (mandra, taar, madhya) | ✓ | ✓ | |
| ArrowLeft, ArrowRight | Cursor nav | ✓ | ✓ | |
| Tab | Nav next sub-beat | ✓ | ✓ | |
| Shift+Tab | Toggle edit mode (swar/stroke) | ✓ | ✓ | |
| F2 | Toggle edit mode | ✓ | ✓ | |
| Backspace, Delete | Delete last | ✓ | ✓ | |
| Ctrl+Z | Undo | ✓ | ✗ | **gap — plan-17 bug 16, web has toolbar button but no keybinding** |
| Ctrl+Shift+Z, Ctrl+Y | Redo | ✓ | ✗ | **gap — plan-17 bug 16, web has toolbar button but no keybinding** |
| Ctrl+X | Cut selection | ✓ | ✓ | |
| Ctrl+C | Copy selection | ✓ | ✓ | |
| Ctrl+V | Paste | ✓ | ✓ | |
| Shift+ArrowLeft, ArrowRight | Extend selection | ✓ | ✓ | |
| Ctrl+D | Set Da stroke on cursor swar | ✓ | ✓ | |
| Ctrl+R | Set Ra stroke on cursor swar | ✓ | ✓ | |
| Alt+g, a, i, k, s, h | Ornaments (gamak, andolan, etc.) | ✓ | ✓ | |
| Alt+m, Shift+M | Meend (asc/desc) | ✓ | ✓ | |
| Alt+r | Krintan | ✓ | ✓ | |
| Alt+u | Murki | ✓ | ✓ | |
| Alt+z | Zamzama | ✓ | ✓ | |
| Enter | Finish multi-note ornament | ✓ | ✓ | |
| Escape, Alt+Escape | Cancel ornament mode | ✓ | ✓ | |

## Tab Lifecycle Behavior

| Event | Desktop | Web | Notes |
|---|---|---|---|
| New tab | ✓ Cmd+N or toolbar | ✓ toolbar + button only | |
| Close tab | ✓ Cmd+W or tab × | ✓ tab × only | |
| Close all tabs | ✓ command palette | ✗ | **gap — web has no closeAll action** |
| Switch tab | ✓ Cmd+Tab / click | ✓ click only | browser owns tab-switching keys |
| Unsaved changes on close | ✓ dialog (Save / Discard / Cancel) | ✓ dialog | |
| Duplicate tab prompt | ✓ dialog (Switch / Open New) | ✓ dialog | |
| File modified externally | ✓ auto-reload | ✗ | **gap — plan-17 task 3-D, web missing file-watch** |
| File deleted externally | ✓ notice | ✗ | **gap — plan-17 task 3-D, web missing file-watch** |
| Session restore on startup | ✓ re-opens last tabs | ✗ | conscious asymmetry — web stores nothing locally for session restore |

## Command Palette Actions (desktop-only, Cmd+K)

| Action | Desktop | Web | Notes |
|---|---|---|---|
| New Composition | ✓ | ✗ | |
| Open File | ✓ | ✗ | |
| Open Folder | ✓ | ✗ | |
| Save | ✓ | ✗ | |
| Save As | ✓ | ✗ | |
| Export HTML | ✓ | ✗ | |
| Close Active Tab | ✓ | ✗ | |
| Next Tab | ✓ | ✗ | |
| Previous Tab | ✓ | ✗ | |
| Edit Properties | ✓ | ✗ | |
| Add Section | ✓ | ✗ | |
| Rename Section | ✓ | ✗ | |
| Remove Section | ✓ | ✗ | |
| Toggle File Browser | ✓ | ✗ | |
| Toggle Theme | ✓ | ✗ | |
| Cycle Script | ✓ | ✗ | |
| User Guide | ✓ | ✗ | |
| Keyboard Shortcuts | ✓ | ✗ | |
| Report Bug | ✓ | ✗ | |
| About | ✓ | ✗ | |

**Note:** The entire command palette is missing on web (plan-17 gap).

---

## How to Use This Inventory

1. **When adding a feature**: Update the relevant table(s) with ✓ or the PR number that will ship it.
2. **When auditing parity**: The subagent at `.claude/agents/cross-platform-parity-checker.md` references this file as the source of truth.
3. **When closing a gap**: Update the "gap" note to `(closed in PR-X)` and change ✗ to ✓.
4. **When adding a conscious asymmetry**: Document it here with a note, and add it to the subagent's exclusion list.

This inventory is maintained by hand. CI does not enforce it — it's a design reference, not a test oracle.
