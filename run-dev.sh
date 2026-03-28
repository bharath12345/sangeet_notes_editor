#!/bin/bash
# Launch Sangeet Notes Editor as a proper macOS app (with dock name and icon)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
open "$SCRIPT_DIR/dev-app/Sangeet Notes Editor.app"
