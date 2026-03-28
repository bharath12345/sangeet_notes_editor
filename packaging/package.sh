#!/usr/bin/env bash
set -euo pipefail

# Sangeet Notes Editor — Packaging Script
# Uses sbt-assembly for fat JAR + jpackage for native installer with bundled JVM
#
# Prerequisites:
#   - JDK 17+ with jpackage (included in standard JDK since 14)
#   - sbt
#
# Usage:
#   ./packaging/package.sh           # Build for current platform
#   ./packaging/package.sh --type dmg  # macOS disk image
#   ./packaging/package.sh --type pkg  # macOS installer
#   ./packaging/package.sh --type msi  # Windows installer
#   ./packaging/package.sh --type deb  # Debian/Ubuntu package
#   ./packaging/package.sh --type rpm  # Red Hat/Fedora package

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_DIR"

APP_NAME="Sangeet Notes Editor"
APP_VERSION="1.0.0"
APP_VENDOR="Sangeet Project"
APP_DESC="Desktop notation editor for Hindustani classical music (Bhatkhande style)"
MAIN_CLASS="sangeet.editor.MainApp"
FAT_JAR="target/scala-3.4.2/sangeet-notes-editor.jar"
OUTPUT_DIR="$PROJECT_DIR/dist"
ICONS_DIR="$SCRIPT_DIR/icons"

# Detect platform
OS="$(uname -s)"
case "$OS" in
    Darwin*)  PLATFORM="mac" ;;
    Linux*)   PLATFORM="linux" ;;
    MINGW*|CYGWIN*|MSYS*) PLATFORM="windows" ;;
    *)        echo "Unknown platform: $OS"; exit 1 ;;
esac

# Determine package type
if [ "${1:-}" = "--type" ] && [ -n "${2:-}" ]; then
    PKG_TYPE="$2"
else
    case "$PLATFORM" in
        mac)     PKG_TYPE="dmg" ;;
        linux)   PKG_TYPE="deb" ;;
        windows) PKG_TYPE="msi" ;;
    esac
fi

# Select icon file
case "$PLATFORM" in
    mac)     ICON_FILE="$ICONS_DIR/sangeet-icon.icns" ;;
    windows) ICON_FILE="$ICONS_DIR/sangeet-icon.ico" ;;
    linux)   ICON_FILE="$ICONS_DIR/sangeet-icon-256.png" ;;
esac

echo "╔══════════════════════════════════════════════╗"
echo "║   Sangeet Notes Editor — Packaging           ║"
echo "╠══════════════════════════════════════════════╣"
echo "║  Platform: $PLATFORM"
echo "║  Package:  $PKG_TYPE"
echo "║  Version:  $APP_VERSION"
echo "╚══════════════════════════════════════════════╝"
echo ""

# Step 1: Build fat JAR
echo "▶ Step 1: Building fat JAR with sbt assembly..."
sbt assembly
echo "  ✓ Fat JAR built: $FAT_JAR"
echo ""

# Verify fat JAR exists
if [ ! -f "$FAT_JAR" ]; then
    echo "✗ Error: Fat JAR not found at $FAT_JAR"
    exit 1
fi

# Step 2: Create output directory
mkdir -p "$OUTPUT_DIR"

# Step 3: Run jpackage
echo "▶ Step 2: Creating native package with jpackage..."
echo "  This bundles a complete JVM runtime (~150MB) so users don't need Java installed."
echo ""

JPACKAGE_CMD=(
    jpackage
    --name "$APP_NAME"
    --app-version "$APP_VERSION"
    --vendor "$APP_VENDOR"
    --description "$APP_DESC"
    --input "$(dirname "$FAT_JAR")"
    --main-jar "$(basename "$FAT_JAR")"
    --main-class "$MAIN_CLASS"
    --type "$PKG_TYPE"
    --dest "$OUTPUT_DIR"
    --icon "$ICON_FILE"
    --java-options "--add-opens=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED"
    --java-options "--add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED"
    --java-options "--add-exports=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED"
    --java-options "-Xmx512m"
)

# Platform-specific options
case "$PLATFORM" in
    mac)
        JPACKAGE_CMD+=(
            --mac-package-name "Sangeet Notes Editor"
            --mac-package-identifier "com.sangeet.noteseditor"
        )
        ;;
    windows)
        JPACKAGE_CMD+=(
            --win-dir-chooser
            --win-shortcut
            --win-menu
            --win-menu-group "Sangeet"
        )
        ;;
    linux)
        JPACKAGE_CMD+=(
            --linux-shortcut
            --linux-menu-group "AudioVideo;Music"
            --linux-app-category "music"
        )
        ;;
esac

echo "  Running: ${JPACKAGE_CMD[*]}"
echo ""

"${JPACKAGE_CMD[@]}"

echo ""
echo "═══════════════════════════════════════════════"
echo "  ✓ Package created in: $OUTPUT_DIR/"
ls -lh "$OUTPUT_DIR/"
echo ""
echo "  Distribute this file — it includes everything"
echo "  needed to run (JVM + app + dependencies)."
echo "═══════════════════════════════════════════════"
