.PHONY: web-dev web-build server server-test core-test desktop-compile test-all all clean help

# Default target
help:
	@echo "Sangeet Notes Editor - Build Targets"
	@echo ""
	@echo "Development:"
	@echo "  web-dev           - Run Elm frontend with live reload (opens browser)"
	@echo "  server            - Run Scala backend server on localhost:8080"
	@echo ""
	@echo "Testing:"
	@echo "  core-test         - Run sangeet-core tests"
	@echo "  server-test       - Run sangeet-server tests"
	@echo "  desktop-compile   - Compile sangeet-desktop (ScalaFX)"
	@echo "  test-all          - Run all sbt tests (core + server + desktop)"
	@echo ""
	@echo "Build:"
	@echo "  web-build         - Build optimized Elm frontend"
	@echo "  all               - Build web frontend + compile all Scala modules"
	@echo ""
	@echo "Cleanup:"
	@echo "  clean             - Remove build artifacts"

# Development
web-dev:
	cd sangeet-web && npx elm-live src/Main.elm --open --dir=public -- --output=public/elm.js

web-build:
	cd sangeet-web && npx elm make src/Main.elm --optimize --output=public/elm.js

server:
	sbt "sangeetServer / run"

# Testing
core-test:
	sbt sangeetCore/test

server-test:
	sbt sangeetServer/test

desktop-compile:
	sbt sangeetDesktop/compile

test-all:
	sbt test

# Build
all: web-build
	sbt compile

# Clean
clean:
	rm -f sangeet-web/public/elm.js
	rm -rf sangeet-web/elm-stuff
	sbt clean
