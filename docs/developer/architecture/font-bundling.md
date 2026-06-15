# Font bundling on desktop

> Status: shipped in plan-18 PR-1b. PR-1a will relocate this note to
> `docs/developer/architecture/font-bundling.md`.

## Why bundle instead of relying on system fonts

The web app loads four Google-hosted Noto families (`Noto Sans`, `Noto Sans
Devanagari`, `Noto Sans Kannada`, `Noto Sans Telugu`) via the stylesheet link
in `sangeet-web/public/index.html`. The desktop renderer's `FontCache` looks up
the same family names through `scalafx.scene.text.Font(name, size)`.

JavaFX's `Font(name, …)` constructor silently falls back to a platform default
when the requested family isn't installed. On a machine without these Noto
families pre-installed (Windows, vanilla Linux distros, macOS without
Devanagari/Kannada/Telugu language packs), Devanagari/Kannada/Telugu sahitya
would render with whatever JavaFX picks — often a tofu box (`▯▯▯`) or
incorrect glyph shaping. The web app, meanwhile, always renders the same
glyphs because the CDN guarantees the fonts are present.

Bundling the `.ttf` files inside the desktop JAR removes that platform skew:
every install ships the exact same font binaries that the web app loads.

## License obligation

The bundled fonts are released under the **SIL Open Font License v1.1** (OFL).
OFL §2 requires that each redistribution include the copyright notice and
license text. We satisfy this by shipping `OFL.txt` alongside the `.ttf` files
in `sangeet-desktop/src/main/resources/fonts/`. The file is therefore on the
classpath of any installer-built JAR/DMG/MSI/DEB and gets bundled
automatically — no extra packaging step needed.

If you ever distribute the fonts standalone (separate from the app), include
`OFL.txt` in that distribution too.

## How it works at runtime

`FontCache.init()`, called from `MainApp.start()` right after `AppLogger`
initializes, iterates the six bundled font resource paths and calls
`javafx.scene.text.Font.loadFont(stream, 10.0)` on each. That registers the
family with JavaFX's internal font database. Subsequent `Font("Noto Sans
Devanagari", 16.0)` lookups (from `SwarGlyphRenderer`, `GridRendererFX`,
`OrnamentRendererFX`) resolve to the bundled font.

The call is idempotent via a `private var initialized` flag, so tests that
spin the desktop subsystem up multiple times won't double-register.

If a resource is missing or `loadFont` returns null, we log a warning via
`AppLogger.info` but do not throw — callers fall back to whichever font
JavaFX picks, matching pre-PR-1b behavior.

## Adding a new bundled font

1. Drop the `.ttf` file in `sangeet-desktop/src/main/resources/fonts/`.
2. Add the resource path to the `bundledFonts` list in `FontCache.init()`.
3. If the font is OFL-licensed, ensure `OFL.txt` covers the copyright holder
   (append a copyright line at the top if it's a new author).
4. Use the family name (as JavaFX sees it) via `FontCache.font(name, size)`
   from the renderer.

Total current bundle delta: ~2.1 MB across six files (Latin Regular+Bold,
Devanagari Regular+Bold, Kannada Regular, Telugu Regular).
