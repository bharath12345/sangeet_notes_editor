#!/usr/bin/env bash
set -euo pipefail

# Sangeet Notes Editor — Packaging Script
# Creates native installer with bundled JVM (~64MB total)
#
# Optimizations applied:
#   - JavaFX WebKit/Media/Swing/FXML excluded (saves ~80MB from fat JAR)
#   - Custom jlink runtime with only needed JVM modules (saves ~70MB)
#   - Staging directory prevents bundling extra files
#
# Usage:
#   ./packaging/package.sh              # Build for current platform
#   ./packaging/package.sh --type dmg   # macOS disk image
#   ./packaging/package.sh --type pkg   # macOS installer
#   ./packaging/package.sh --type msi   # Windows installer
#   ./packaging/package.sh --type deb   # Debian/Ubuntu package
#   ./packaging/package.sh --type rpm   # Red Hat/Fedora package

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
STAGING_DIR="$(mktemp -d)"
RUNTIME_DIR="$(mktemp -d)/sangeet-runtime"

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

trap "rm -rf '$STAGING_DIR' '$(dirname "$RUNTIME_DIR")'" EXIT

echo "╔══════════════════════════════════════════════╗"
echo "║   Sangeet Notes Editor — Packaging           ║"
echo "╠══════════════════════════════════════════════╣"
echo "║  Platform: $PLATFORM"
echo "║  Package:  $PKG_TYPE"
echo "║  Version:  $APP_VERSION"
echo "╚══════════════════════════════════════════════╝"
echo ""

# Step 1: Build fat JAR
echo "▶ Step 1/3: Building fat JAR..."
sbt assembly
echo "  ✓ Fat JAR: $(du -h "$FAT_JAR" | cut -f1)"
echo ""

if [ ! -f "$FAT_JAR" ]; then
    echo "✗ Error: Fat JAR not found at $FAT_JAR"
    exit 1
fi

# Step 2: Create stripped JVM runtime
echo "▶ Step 2/3: Creating minimal JVM runtime with jlink..."
JLINK_MODULES="java.base,java.desktop,java.logging,java.net.http,java.scripting,java.xml,jdk.jfr,jdk.jsobject,jdk.unsupported,jdk.unsupported.desktop,jdk.xml.dom"

jlink \
  --add-modules "$JLINK_MODULES" \
  --strip-debug \
  --no-man-pages \
  --no-header-files \
  --compress zip-9 \
  --output "$RUNTIME_DIR"

echo "  ✓ Runtime: $(du -sh "$RUNTIME_DIR" | cut -f1) (stripped from $(du -sh "$JAVA_HOME" | cut -f1) full JDK)"
echo ""

# Step 3: Stage only the fat JAR
cp "$FAT_JAR" "$STAGING_DIR/"
mkdir -p "$OUTPUT_DIR"

echo "▶ Step 3/3: Creating native package with jpackage..."

JPACKAGE_CMD=(
    jpackage
    --name "$APP_NAME"
    --app-version "$APP_VERSION"
    --vendor "$APP_VENDOR"
    --description "$APP_DESC"
    --input "$STAGING_DIR"
    --main-jar "sangeet-notes-editor.jar"
    --main-class "$MAIN_CLASS"
    --type "$PKG_TYPE"
    --dest "$OUTPUT_DIR"
    --icon "$ICON_FILE"
    --runtime-image "$RUNTIME_DIR"
    --java-options "--add-opens=java.base/java.lang=ALL-UNNAMED"
    --java-options "-Xmx512m"
)

case "$PLATFORM" in
    mac)
        JPACKAGE_CMD+=(
            --mac-package-name "SangeetEditor"
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

"${JPACKAGE_CMD[@]}"

echo ""
echo "═══════════════════════════════════════════════"
echo "  ✓ Package created:"
ls -lh "$OUTPUT_DIR/"*."$PKG_TYPE" 2>/dev/null || ls -lh "$OUTPUT_DIR/"
echo ""
echo "  No Java installation needed — JVM is bundled."
echo "═══════════════════════════════════════════════"
