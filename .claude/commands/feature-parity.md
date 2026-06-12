---
description: Audit desktop vs web for feature drift. Runs the cross-platform-parity-checker subagent.
---

Spawn the `cross-platform-parity-checker` subagent and have it produce a parity report between `sangeet-desktop/` (Scala 3 + ScalaFX) and `sangeet-web/` (Elm) for the current working tree.

The agent surveys toolbars, key handlers, dialogs, and Api consumers in both stacks, lists asymmetries with file paths, and ignores the conscious-asymmetry list documented in its system prompt.

After the agent reports back, surface the punch list verbatim to the user. Do **not** propose fixes or start implementing them — let the user decide what to address.
