.PHONY: web-dev web-build server server-test core-test desktop-compile elm-test e2e-test test-web test-all all clean help
.PHONY: format format-scala format-elm format-ts lint lint-scala lint-elm lint-ts coverage check-all

# Default target
help:
	@echo "Sangeet Notes Editor - Build Targets"
	@echo ""
	@echo "Development:"
	@echo "  web-dev           - Run Elm frontend with live reload (opens browser)"
	@echo "  server            - Run Scala backend server on localhost:28080"
	@echo ""
	@echo "Testing:"
	@echo "  core-test         - Run sangeet-core tests"
	@echo "  server-test       - Run sangeet-server tests"
	@echo "  elm-test          - Run Elm program tests"
	@echo "  e2e-test          - Run Playwright E2E tests (requires server on :28080)"
	@echo "  test-web          - Run all web tests (elm + server + e2e)"
	@echo "  desktop-compile   - Compile sangeet-desktop (ScalaFX)"
	@echo "  test-all          - Run all sbt tests (core + server + desktop)"
	@echo ""
	@echo "Quality:"
	@echo "  format            - Auto-format all code (Scala + Elm + TS/JS)"
	@echo "  lint              - Check formatting and linting (all languages)"
	@echo "  coverage          - Run tests with coverage report (80% minimum)"
	@echo "  check-all         - Run lint + test-all + coverage"
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

elm-test:
	cd sangeet-web && ./node_modules/.bin/elm-test

e2e-test:
	cd e2e && ./node_modules/.bin/playwright test

test-web: elm-test server-test

desktop-compile:
	sbt sangeetDesktop/compile

test-all:
	sbt test

# Formatting
format: gen-strings format-scala format-elm format-ts

format-scala:
	sbt scalafmtAll

format-elm:
	cd sangeet-web && ./node_modules/.bin/elm-format src/ tests/ --yes

format-ts:
	./node_modules/.bin/prettier --write e2e/ sangeet-web/public/ports.js sangeet-web/public/styles.css

# Linting
lint: lint-scala lint-elm lint-ts

lint-scala:
	sbt scalafmtCheckAll "scalafixAll --check"

lint-elm:
	cd sangeet-web && ./node_modules/.bin/elm-format src/ tests/ --validate
	cd sangeet-web && ./node_modules/.bin/elm-review

lint-ts:
	./node_modules/.bin/prettier --check e2e/ sangeet-web/public/ports.js sangeet-web/public/styles.css
	cd e2e && ./node_modules/.bin/eslint .

# Coverage
coverage:
	sbt coverage sangeetCore/test sangeetServer/test coverageReport coverageAggregate

# Full quality check
check-all: lint test-all coverage

# Build
all: web-build
	sbt compile

# Clean
clean:
	rm -f sangeet-web/public/elm.js
	rm -rf sangeet-web/elm-stuff
	sbt clean

# UI Strings catalog codegen
.PHONY: gen-strings check-strings find-untracked-strings strings-report

gen-strings: ## Regenerate UiStrings.scala and UiStrings.elm from ui-strings.json
	sbt sangeetCore/genUiStrings
	cd scripts && npm install --silent && npm run gen

check-strings: ## Run cross-platform UI strings parity check
	cd scripts && npm install --silent && npm run parity

find-untracked-strings: ## Heuristic sweep for English-looking literals not in the catalog
	cd scripts && npm install --silent && npm run find-untracked

strings-report: ## Generate docs/strings-parity-report.md
	cd scripts && npm install --silent && npm run report
