# Plan 11: Documentation, UI Polish, Hosting, Appreciation, Code Quality

## Context

16 PRs have been merged since the project started. Several major features were added (tabbed editor, file browser, cut/copy/paste, starting beat/locked beats, chikari as Event) and one was removed (PDF export). The documentation (CLAUDE.md, README, design spec) is now stale. Additionally, the desktop UI needs visual polish, the web app needs a hosting plan, and recent rapid development has introduced some code quality issues.

This plan covers 8 workstreams that can be done largely in parallel.

---

## Workstream 1: Update CLAUDE.md

### Changes needed

**Remove references to PDF export:**
- Remove `Apache PDFBox` from Technology Stack
- Remove `PDF export (PDFBox)` from `format/` package description
- Remove `PDF export with Devanagari font...` from What's Built
- Remove `Ctrl+E → Export PDF` from any keyboard reference
- Remove PDFBox from Tech Stack list
- Update test counts

**Add new features to What's Built:**
- Tabbed editor with multiple compositions open simultaneously
- File browser panel with directory bookmarks and `.swar` file tree
- Session persistence (AppConfig) — restores open tabs, bookmarks, panel state on restart
- Cut/copy/paste with beat-range selection (Ctrl+X/C/V)
- Chikari promoted to `Event.Chikari` (was inline in stroke mode)
- Per-section starting beat for Gat/Bandish (locked beats before sam)
- `Event.LockedBeat` — persistent locked beat events, deletion-guarded, cycle-0 only
- Compact `.swar` format — omits default values, smaller files

**Update test counts:**
- Core: 565 (was 523)
- Server: 122 (was 112)
- Desktop: 86 TCP tests (was 95)
- Elm: 558 (was 476)
- E2E: 126 (was 110)
- Total: ~1457 (was 1316)

**Update module layout:**
- Add `config/` package under `sangeet-core` (AppConfig, ConfigStore)
- Add `TabManager.scala`, `FileBrowserPanel.scala` under `sangeet-desktop/editor/`
- Remove PDFBox from format package description

**Update keyboard reference:**
- Add `Ctrl+X` (Cut), `Ctrl+C` (Copy), `Ctrl+V` (Paste)
- Remove `Ctrl+E` (Export PDF)

---

## Workstream 2: Update README.md

### Changes needed

**Features list:**
- Remove `PDF export` bullet and all PDF mentions
- Add: Tabbed editing (multiple compositions), File browser with bookmarks, Cut/copy/paste with selection, Session restore, Per-section starting beat with locked beats, Compact .swar format
- Update test count table

**Tech Stack:**
- Remove `Apache PDFBox for PDF export`
- Update test counts in tech stack summary

**Keyboard Reference table:**
- Add Ctrl+X/C/V
- Remove Ctrl+E

**Test Coverage Summary table:**
- Update all counts to current values

**Download section:**
- Keep as-is (GitHub Releases)
- Add note: "Web version coming soon at sangeet-editor.in"

**License section:**
- Change from "All rights reserved" to MIT license
- Add LICENSE file at repo root

---

## Workstream 3: Update Design Spec

File: `docs/superpowers/specs/2026-03-28-sangeet-notes-editor-design.md`

### Changes needed

**Overview/Goals:**
- Remove "PDF export for printing clean notation sheets"
- Add "HTML export for printing and sharing"
- Update "Cross-platform desktop app" to mention web app
- Move "Web or mobile version" from Non-Goals to current status

**Section 2 (File Format):**
- Add `Event.LockedBeat` to event types
- Add `Event.Chikari` to event types
- Document compact format (omitted defaults)
- Add `startingBeat` field to Section schema

**Add new section: Desktop Studio Features:**
- Tabbed editor (multiple compositions)
- File browser with directory bookmarks
- Session persistence (AppConfig JSON at `~/.sangeet-notes-editor/config.json`)
- Panel collapse/restore

**Update Section on Export:**
- Remove PDF export section entirely
- Keep HTML export

---

## Workstream 4: In-App User Guide

### Approach

Markdown source files in `docs/user-guide/` serve as the single source of truth. Both desktop and web apps embed and render this content.

### Source structure

```
docs/user-guide/
  01-getting-started.md
  02-creating-compositions.md
  03-entering-notes.md
  04-ornaments-strokes.md
  05-sections-navigation.md
  06-editing-clipboard.md
  07-file-operations.md
  08-keyboard-reference.md
  09-taals-raags.md
  10-starting-beat.md
```

Each file uses standard Markdown. Platform-specific instructions use admonition blocks:

```markdown
> **Desktop:** Use `Ctrl+S` to save.
> **Web:** Click the Save button in the toolbar, or use `Ctrl+S`.
```

### Desktop integration

- Add `Help` menu with `User Guide` item
- Open a new tab (or dialog) with an embedded WebView rendering the Markdown as HTML
- At build time, sbt resource generator copies `docs/user-guide/*.md` into the JAR's resources
- A `UserGuideRenderer` converts Markdown → HTML using a lightweight library (flexmark-java, already JVM-compatible)
- Style the HTML with the app's warm color palette

### Web integration

- Add a "Help" / "Guide" button in the toolbar
- Open a modal or side panel rendering the guide content
- At build time, a script converts `docs/user-guide/*.md` to a JSON array of `{title, html}` objects
- Elm decodes this JSON (bundled as a static asset) and renders it in a scrollable panel
- Table of contents sidebar for navigation

### Dependencies

- Desktop: `com.vladsch:flexmark-java:0.64.8` (Markdown → HTML, BSD license)
- Web: A build-time Node script using `marked` (npm) to convert MD → JSON; no runtime dependency

---

## Workstream 5: Hosting Guide (sangeet-editor.in on GCP)

### Architecture

```
sangeet-editor.in (GoDaddy domain)
  ├── Frontend: Firebase Hosting (static Elm app, global CDN)
  │   └── sangeet-editor.in → index.html, elm.js, styles.css
  └── API: Cloud Run (Scala JVM backend)
      └── api.sangeet-editor.in → :28080
```

### Step-by-step deployment plan

**Phase 1: GCP Project Setup**
1. Create GCP project `sangeet-editor`
2. Enable Cloud Run API, Container Registry, Firebase Hosting
3. Install `gcloud` CLI, authenticate
4. Set region to `asia-south1` (Mumbai) for low latency

**Phase 2: Backend (Cloud Run)**
1. Create `Dockerfile` in project root:
   ```dockerfile
   FROM eclipse-temurin:17-jre-alpine
   COPY sangeet-server/target/scala-3.*/sangeet-server-assembly.jar /app/server.jar
   EXPOSE 28080
   ENV PORT=28080
   CMD ["java", "-jar", "/app/server.jar"]
   ```
2. Build: `sbt sangeetServer/assembly`
3. Build & push Docker image: `gcloud builds submit --tag gcr.io/sangeet-editor/server`
4. Deploy: `gcloud run deploy sangeet-server --image gcr.io/sangeet-editor/server --port 28080 --region asia-south1 --allow-unauthenticated --min-instances 0 --max-instances 2`
5. Note the Cloud Run URL (e.g., `sangeet-server-xxxxx.asia-south1.run.app`)

**Phase 3: Frontend (Firebase Hosting)**
1. `firebase init hosting` in project root
2. Set public directory to `sangeet-web/public`
3. Configure `firebase.json` rewrites to proxy `/api/*` to Cloud Run
4. Build Elm: `cd sangeet-web && ./node_modules/.bin/elm make src/Main.elm --optimize --output=public/elm.js`
5. Deploy: `firebase deploy --only hosting`

**Phase 4: Domain Setup (GoDaddy → GCP)**
1. In Firebase Console: Add custom domain `sangeet-editor.in`
2. In GoDaddy DNS: Add the A records Firebase provides (two IPs)
3. In GoDaddy DNS: Add CNAME `www` → `sangeet-editor.in`
4. Firebase auto-provisions SSL certificate (Let's Encrypt)
5. Map Cloud Run to `api.sangeet-editor.in`:
   - `gcloud run domain-mappings create --service sangeet-server --domain api.sangeet-editor.in --region asia-south1`
   - Add the CNAME record GCP provides to GoDaddy DNS

**Phase 5: CI/CD for Deployment**
1. Add GitHub Actions workflow `.github/workflows/deploy.yml`
2. Triggered on push to `main` (after CI passes)
3. Steps: build Elm → build server JAR → build Docker image → push to GCR → deploy to Cloud Run → deploy to Firebase
4. Store GCP service account key as GitHub secret

**Phase 6: Environment Config**
1. Elm app needs API base URL configurable (localhost for dev, `api.sangeet-editor.in` for prod)
2. Add `API_BASE_URL` flag to Elm build or inject via `public/config.js`

**Estimated monthly cost:** $0-5 (scale-to-zero) or ~$10 (one warm instance to avoid JVM cold start)

---

## Workstream 6: Cross-Linking Desktop ↔ Web

### Desktop: About Dialog

Add/update the About dialog (`Help → About`) to include:
- App name, version, build date
- "Web version: sangeet-editor.in" as a clickable hyperlink (opens system browser)
- "Download desktop app" link to GitHub Releases
- MIT License notice
- "Buy me a coffee" link (see Workstream 7)

Implementation: `AboutDialog.scala` in `sangeet-desktop/dialog/`

### Web: Footer/Toolbar Links

Add to the web app footer or a dedicated "About" panel:
- "Download desktop app" linking to GitHub Releases page
- Version info
- MIT License notice
- "Buy me a coffee" link

---

## Workstream 7: "Buy Me a Coffee" — Appreciation Mechanism

### Dual approach: UPI (India) + International platform

**For Indian users:**
- UPI handle displayed as text
- UPI QR code image (you generate this from any UPI app like Google Pay, PhonePe)
- Place QR code image at `docs/assets/upi-qr.png`

**For international users:**
- Set up an account on one of: Buy Me a Coffee, Ko-fi, or GitHub Sponsors
- Add the profile URL

**Where to show it:**

Desktop:
- `Help → Support This Project` menu item opens a dialog with:
  - "If you find this useful, you can buy me a coffee"
  - UPI QR code + handle (for India)
  - BuyMeACoffee/Ko-fi link (for international)
  - "This app is free and always will be. All features, no restrictions."

Web:
- Small heart/coffee icon in the toolbar or footer
- Opens a modal with the same content as the desktop dialog
- Also linked from the About section

**Implementation:**
- `SupportDialog.scala` in `sangeet-desktop/dialog/`
- `SupportModal.elm` in `sangeet-web/src/View/Dialogs/`
- UPI QR code loaded from resources (desktop JAR) or static assets (web)

**Action items for you (not code):**
1. Generate UPI QR code from your UPI app and save as PNG
2. Create account on BuyMeACoffee/Ko-fi/GitHub Sponsors
3. Provide the URLs/handles — I'll wire them into the code

---

## Workstream 8: Desktop UI Polish — IntelliJ-Inspired Hybrid Theme

### 8A: Icon Library — Material Design Icons via Ikonli

**Why Ikonli:** Renders icons as JavaFX `Text` nodes via font files. No SVG parsing, no PNG assets. Two sbt dependencies, ~200KB.

**Dependencies to add to `build.sbt`:**
```scala
"org.kordamp.ikonli" % "ikonli-javafx" % "12.4.0",
"org.kordamp.ikonli" % "ikonli-materialdesign2-pack" % "12.4.0"
```

**License:** Apache 2.0 (completely free)

**Icons to use:**
| Action | Icon code |
|--------|-----------|
| New | `MDI2_FILE_PLUS_OUTLINE` |
| Open | `MDI2_FOLDER_OPEN_OUTLINE` |
| Save | `MDI2_CONTENT_SAVE` |
| Undo | `MDI2_UNDO` |
| Redo | `MDI2_REDO` |
| Cut | `MDI2_CONTENT_CUT` |
| Copy | `MDI2_CONTENT_COPY` |
| Paste | `MDI2_CLIPBOARD_TEXT_OUTLINE` |
| Play | `MDI2_PLAY` |
| Pause | `MDI2_PAUSE` |
| Stop | `MDI2_STOP` |
| Settings | `MDI2_COG_OUTLINE` |
| Help | `MDI2_HELP_CIRCLE_OUTLINE` |

**Replace all text-based toolbar buttons** with icon buttons + tooltips.

### 8B: Theme System — Light and Dark with Warm Palette

Create two CSS stylesheets loaded by ScalaFX:
- `themes/sangeet-light.css`
- `themes/sangeet-dark.css`

**Light theme (default):**
- Background: warm cream `#FDF6EC`
- Sidebar: light saffron tint `#FFF8F0`
- Toolbar: warm gray `#F5EDE3`
- Accent: saffron/amber `#E8A317`
- Selection: soft maroon `#8B1A1A` with 20% opacity
- Text: dark warm gray `#2D2926`
- Tab active indicator: saffron `#E8A317`

**Dark theme:**
- Background: deep warm charcoal `#1E1B18`
- Sidebar: darker warm `#171411`
- Toolbar: warm dark gray `#2A2520`
- Accent: golden saffron `#E8A317`
- Selection: maroon `#8B1A1A` with 30% opacity
- Text: warm light `#E8DFD5`
- Tab active indicator: saffron `#E8A317`

**Theme toggle:**
- `View → Theme → Light / Dark` menu items
- Persist choice in AppConfig
- Restore on startup

**Implementation:**
- `ThemeManager.scala` — loads/switches CSS, stores current theme
- Modify `MainApp.scala` to apply theme CSS to the primary stage scene
- ScalaFX supports `scene.stylesheets.add()` for CSS theming

### 8C: Splash Screen

**Implementation:**
- `SplashScreen.scala` — undecorated `Stage` shown before main window
- Display for minimum 2.5 seconds, dismiss on click or when main window is ready (whichever is later)
- Background: gradient from deep warm charcoal to saffron tint
- Content: sitar image (royalty-free, see below) + app name + version

**Sitar image sourcing:**
- Browse [Unsplash sitar photos](https://unsplash.com/s/photos/sitar) and [Pixabay sitar images](https://pixabay.com/images/search/sitar/)
- Both are free for commercial use, no attribution required
- Look for: dark/moody sitar on dark background with negative space for text overlay
- Save selected image to `sangeet-desktop/src/main/resources/images/splash-sitar.jpg`
- Resize to ~800x450px for the splash window

**Layout options (pick one during implementation):**

**Option A — Centered overlay:**
```
┌──────────────────────────────────────┐
│                                      │
│         [Sitar image, dimmed]        │
│                                      │
│       ♪ Sangeet Notes Editor         │
│           Version 1.0                │
│                                      │
│    Hindustani Classical Music        │
│         Notation Editor              │
│                                      │
└──────────────────────────────────────┘
```
Sitar image fills the background, slightly dimmed. Text centered with warm golden color and subtle drop shadow.

**Option B — Side-by-side:**
```
┌────────────────────┬─────────────────┐
│                    │                 │
│   [Sitar detail    │  ♪ Sangeet      │
│    close-up,       │  Notes Editor   │
│    warm lighting]  │                 │
│                    │  Version 1.0    │
│                    │                 │
│                    │  ────────────── │
│                    │  Bhatkhande     │
│                    │  Notation       │
│                    │  for Sitar      │
│                    │                 │
└────────────────────┴─────────────────┘
```
Left half: sitar image. Right half: warm gradient background with text.

**Option C — Minimal silhouette:**
```
┌──────────────────────────────────────┐
│                                      │
│    ┌─────────────────────────┐       │
│    │                         │       │
│    │   [Sitar silhouette     │       │
│    │    in saffron/gold]     │       │
│    │                         │       │
│    └─────────────────────────┘       │
│                                      │
│       Sangeet Notes Editor           │
│           v1.0                       │
│                                      │
└──────────────────────────────────────┘
```
Solid warm background. Sitar as a monochrome silhouette in saffron/gold. Clean and iconic.

**Option D — Full-bleed cinematic:**
```
┌──────────────────────────────────────┐
│▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│
│▓▓▓▓▓▓▓▓▓ [Full sitar photo] ▓▓▓▓▓▓▓▓│
│▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│
│▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│
│━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━│
│  Sangeet Notes Editor    Version 1.0 │
└──────────────────────────────────────┘
```
Full-bleed sitar photo (dark, atmospheric). Bottom strip with app name. Like IntelliJ's recent splash style.

**Action item for you:** Browse the Unsplash/Pixabay links, pick an image, and choose a layout option.

---

## Workstream 9: Code Quality Improvements

### High Priority (duplication causing maintenance risk)

**9.1: Extract `Event.withPosition` extension method**
- File: `sangeet-core/.../model/Event.scala`
- Add:
  ```scala
  extension (e: Event)
    def withPosition(pos: BeatPosition): Event = e match
      case s: Event.Swar       => s.copy(beat = pos)
      case r: Event.Rest       => r.copy(beat = pos)
      case u: Event.Sustain    => u.copy(beat = pos)
      case c: Event.Chikari    => c.copy(beat = pos)
      case l: Event.LockedBeat => l.copy(beat = pos)
  ```
- Then replace 4 duplicated match blocks in `CompositionEditor.scala` (lines 78-83, 204-209, 294-300, 347-353) with `event.withPosition(newPos)`

**9.2: Deduplicate clipboard code in `EditorPane.scala`**
- Make `Ctrl+C` keyboard handler call `copySelection()` instead of reimplementing
- Make `Ctrl+X` keyboard handler call `cutSelection()` instead of reimplementing
- Eliminates ~30 lines of exact duplication

**9.3: Unify fast-typing grouping logic**
- Make the `setOnKeyTyped` handler in `EditorPane.scala` call `typeCharTimed()` instead of reimplementing the same grouping logic
- Eliminates ~50 lines of near-identical code

### Medium Priority (file size)

**9.4: Split `EditorPane.scala` (921 lines → 3 files)**
- `EditorPane.scala` (~300 lines) — canvas, scrolling, cursor blink, redraw, public API
- `EditorKeyHandler.scala` (~350 lines) — extract `setOnKeyPressed` + `setOnKeyTyped` handlers
- `EditorClipboard.scala` (~80 lines) — extract `copySelection`, `cutSelection`, `pasteClipboard`

**9.5: Split `MainApp.scala` (694 lines → 3 files)**
- `MainApp.scala` (~200 lines) — thin shell, `start()` assembles parts
- `ToolbarBuilder.scala` (~250 lines) — all toolbar/button creation
- `LayoutBuilder.scala` (~150 lines) — SplitPane, panel collapse, session restore

### Low Priority (cleanup)

**9.6: Remove duplicate companion methods in `CompositionEditor.scala`**
- `toPos` (companion line 338) duplicates `unflatPosition` (instance line 283) — unify into `BeatPosition.fromFlat(flat, matras)`
- `setPos` (companion line 347) duplicates `setEventPosition` (instance line 294) — after 9.1, both become `event.withPosition(pos)` so this resolves itself

**9.7: Fix redundant import in `EditorPane.scala` line 17**
- `import ...model.{Andolan, Gamak, Gitkari, MeendDirection, _}` — the `_` wildcard makes the named imports redundant
- Change to `import ...model._`

**9.8: Unify EditAction patterns**
- Toolbar methods directly call `pushEditor()`/`redraw()` while keyboard handler returns `EditAction` enum
- Make toolbar methods also go through the `EditAction` dispatcher for consistent undo behavior

---

## Workstream 10: Add MIT License

- Create `LICENSE` file at repo root with MIT License text, copyright `2026 Bharadwaj`
- Update README License section
- Add SPDX identifier to `build.sbt`: `licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT"))`

---

## Implementation Order

The workstreams are mostly independent. Suggested order:

1. **License** (Workstream 10) — 5 minutes, do first
2. **Code quality** (Workstream 9) — do before UI changes since it restructures files
3. **Doc updates** (Workstreams 1-3) — can be done in parallel with code quality
4. **Theme system + icons** (Workstream 8B, 8A) — foundation for UI polish
5. **Splash screen** (Workstream 8C) — after theme is in place, needs image selection from you
6. **Cross-linking + appreciation** (Workstreams 6-7) — needs your UPI QR code and platform choice
7. **User guide** (Workstream 4) — write content after all features are stable
8. **Hosting guide** (Workstream 5) — execute when ready to go live

Workstreams 1-3 can be a single PR. Workstream 9 is a separate PR. Workstreams 8A-8C are a third PR. Everything else gets its own PR.

---

## Verification

- `sbt compile` — all modules compile after code quality changes
- `sbt test` — all 565 + 122 + 86 = 773 Scala tests pass
- `elm-test` — all 558 Elm tests pass
- `make lint` — formatting and linting pass
- Visual check — desktop app launches with splash, theme toggles work, icons render
- Manual check — user guide renders in both desktop Help and web Help
