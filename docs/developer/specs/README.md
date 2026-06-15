# Specs

Auto-generated machine-readable specifications.

## Files

- `openapi.yaml` — OpenAPI 3.0 spec for the sangeet-server REST API. Generated
  from the Tapir endpoint definitions in
  `sangeet-server/src/main/scala/com/varpas/sangeet/server/endpoints/` (plus
  the `/health` and `/metrics` shapes mirrored from `Main.scala`).
- `swar.schema.json` — JSON Schema (draft-07) for `.swar` composition files.
  Generated from the `Composition` case class in
  `sangeet-core/src/main/scala/com/varpas/sangeet/core/model/`, with the on-wire
  shape (discriminators, rationals-as-pairs, omitted defaults) mirrored from
  the circe codecs in `sangeet-core/.../format/`.

## Regenerating

```bash
make gen-specs
```

This runs `sbt generateOpenApi generateSwarSchema`. CI's `check-specs` job
runs the same command and fails if either output drifts from what's checked
in.

## Source of truth

The Scala source files are authoritative. Do NOT hand-edit `openapi.yaml`
or `swar.schema.json` — regenerate instead.

If you need to change the spec content, change the source:

- Tapir endpoint changes → edit the relevant
  `sangeet-server/.../endpoints/*.scala` file, then `make gen-specs`.
- `.swar` shape changes → edit the model + codec in `sangeet-core/.../model/`
  and `sangeet-core/.../format/`, then update the matching definition in
  `SwarSchemaExporter.scala` and `make gen-specs`. The JSON Schema is
  hand-written (see the doc comment on `SwarSchemaExporter.scala` for why),
  so it does not auto-track codec changes.

## Human-readable design

See `../../specs/` for the human-readable design docs:

- `backend-api-spec.md` — narrative description of the REST API
- `frontend-spec.md` — Elm SPA design notes

And `../../superpowers/specs/2026-03-28-sangeet-notes-editor-design.md`
remains the project-wide source of truth for design decisions.
