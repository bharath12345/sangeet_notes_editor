# Observability & Bug Reporting — Implementation Tracker

Companion to [`docs/plans/plan-12-observability-and-replay.md`](plans/plan-12-observability-and-replay.md). The plan is the design; this is the **living record** of what's actually deployed, what configuration exists in external services, what's still pending, and gotchas hit along the way. Updated after every meaningful change.

**Last updated:** 2026-06-11 (Phases 1, 2, 3, 4 MVP, 4b, 5a, 5b all shipped in a single day; CI path-filter interlude cut iteration time from ~8 min to ~2-3 min per backend-only PR; PRs #25–#39 + #40 action bumps)

---

## Quick status

| Phase | Status | Notes |
|---|---|---|
| 1. Backend metrics infrastructure (Micrometer → Cloud Monitoring) | 🟢 done | JVM time series flowing every 60s. PRs #25–#30. |
| 2. Custom backend metrics (HTTP requests via Tapir interceptor) | 🟢 done | `tapir.request.{total,duration,active}` series keyed by route template, method, status_code. PR #32. |
| Interlude — CI path filters | 🟢 done | Docs PRs run in ~2 min instead of ~8. Backend-internal PRs skip elm-tests + e2e. PR #33. |
| 3. Web frontend metrics (PostHog "Sangeet Web") | 🟢 done | `click` + `keystroke` + `$pageview` events with region/element tags. PR #34. |
| 4. Web session replay buffer (rrweb, no UI) | 🟢 done | 5-min rolling RAM buffer; `window.__replay.{events,stats,clear}`. PR #35. |
| 4b. Web Report Bug button + modal + POST | 🟢 done | End-to-end web reporting live. PR #37. |
| 5a. Backend `POST /api/v1/bug-reports` + GCS storage | 🟢 done | Any JSON body → `gs://sangeet-bug-reports/<uuid>.json`. PR #36. |
| 5b. GitHub Issue auto-creation + Secret Manager PAT | 🟢 done | Fire-and-forget fiber files issue with body + GCS console link after each GCS write. PR #39. |
| 6. Replay viewer | ⬜ not started | |
| 7. Polish + privacy notes (web stack) | ⬜ not started | |
| 8. Desktop rolling buffer + Report a Bug | ⬜ not started | |
| 9. Desktop auto-crash capture + recovery dialog | ⬜ not started | |
| 10. Desktop usage metrics (PostHog-Java "Sangeet Desktop") | ⬜ not started | |

Legend: 🟢 done · 🟡 in progress / known issue · ⬜ not started · 🔴 blocked

---

## Cloud / external service inventory

Everything that has to exist outside this repo for the system to work.

### GCP (project: `sangeet-editor`)

| Resource | Identifier / config | Created via | Purpose |
|---|---|---|---|
| Cloud Run service | `sangeet-server` in `asia-south1` | Plan 11 Phase 5, ongoing | Hosts the backend |
| Artifact Registry repo | `asia-south1-docker.pkg.dev/sangeet-editor/sangeet` | Plan 11 Phase 2 | Container image registry |
| GitHub-actions SA | `github-deployer@sangeet-editor.iam.gserviceaccount.com` | Plan 11 Phase 9 | Used by `deploy-backend.yml` |
| Cloud Run runtime SA | `729103223940-compute@developer.gserviceaccount.com` (default compute SA) | Auto-created | What `sangeet-server` runs as on Cloud Run; pushes metrics |
| Cloud Monitoring API | `monitoring.googleapis.com` | Phase 1 setup, `gcloud services enable` | Receives metrics from the runtime |
| Runtime SA role binding | `roles/monitoring.metricWriter` on Compute default SA | Phase 1 setup, project-level binding | Allows the runtime to write metrics + descriptors |
| Cloud Run env var | `GCP_PROJECT_ID=sangeet-editor` on the service | Phase 1 setup, `gcloud run services update` | Enables `MetricsRegistry.stackdriver` registry on startup |
| Cloud Run CPU allocation | `cpu-throttling: false` (always-allocated) | Phase 1 debug | So Micrometer push thread can run between requests |
| GCS bucket | `gs://sangeet-bug-reports` in `asia-south1`, uniform-bucket-level-access | Phase 5a setup, `gcloud storage buckets create` | Stores bug-report JSON payloads from web + (eventually) desktop |
| Bucket lifecycle policy | Delete objects > 90 days | Phase 5a setup, `gcloud storage buckets update --lifecycle-file` | Auto-prune old reports |
| Bucket SA binding | `roles/storage.objectAdmin` on Compute default SA, scoped to bucket only | Phase 5a setup, `gcloud storage buckets add-iam-policy-binding` | Cloud Run runtime can write reports; least-privilege (bucket-scoped, not project-wide) |
| Cloud Run env var | `BUG_REPORTS_BUCKET=sangeet-bug-reports` on the service | Phase 5a setup, `gcloud run services update --update-env-vars` | Activates `GcsBugReportStorage`; without it endpoint returns 503 |
| Secret Manager API | `secretmanager.googleapis.com` | Phase 5b setup, `gcloud services enable` | Required before any secret operations on the project |
| Secret Manager secret | `github-issues-token` (single version, the fine-grained PAT) | Phase 5b setup, `gcloud secrets create` + `versions add --data-file=-` | Stores the GitHub PAT outside source / outside env var history |
| Secret IAM binding | `roles/secretmanager.secretAccessor` on Compute default SA, scoped to `github-issues-token` only | Phase 5b setup, `gcloud secrets add-iam-policy-binding` | Cloud Run can read the secret at runtime; least-privilege (secret-scoped) |
| Cloud Run env var `GITHUB_TOKEN` | Sourced from `secretKeyRef: github-issues-token:latest` on the service | Phase 5b setup, `gcloud run services update --update-secrets` | Mounted as env var inside the container; `HttpGitHubIssuesClient` reads from `sys.env` |
| Cloud Run env var `GITHUB_REPO` | `bharath12345/sangeet_notes_editor` on the service | Phase 5b setup, `gcloud run services update --update-env-vars` | Tells the client which repo to file issues against |

### Future GCP resources (planned, not yet created)

| Resource | Phase | Purpose |
|---|---|---|
| Secret Manager secret `replay-viewer-password` | 6 | HTTP Basic Auth password for the replay viewer |

### Non-GCP services

| Service | Plan / cost | Status |
|---|---|---|
| GitHub Pages | Free; hosts the Elm frontend | 🟢 live since Plan 11 |
| Grafana Cloud Free (viewer only, reading from Cloud Monitoring) | Free forever; dashboards only — no metric data stored there | ⬜ deferred; signup happens when we build the first real dashboard |
| PostHog Cloud project "Sangeet Web" | Free 1M events/month | 🟢 live — events flowing (Phase 3) |
| PostHog Cloud project "Sangeet Desktop" | Free 1M events/month (separate project from Web for clean separation per decision #8) | ⬜ not created yet (Phase 10) |
| rrweb 2.0.0-alpha.4 via jsDelivr CDN | Free; in-browser session recording | 🟢 live — buffer recording, payload POSTs to backend on Report Bug |

---

## Phase 1 — detailed status

### What's deployed
- `MetricsRegistry` singleton at `sangeet-server/src/main/scala/com/varpas/sangeet/server/metrics/MetricsRegistry.scala`
- Tapir `GET /metrics` endpoint serving Prometheus text exposition format (works — returns 200 with ~80 series)
- `MicrometerVersion = 1.13.0` in `build.sbt`
- Dependencies: `micrometer-core`, `micrometer-registry-prometheus`, `micrometer-registry-stackdriver`
- JVM bindings attached at startup: heap, GC, classes, threads, CPU, file descriptors, uptime
- Startup log lines report metrics URL and Cloud Monitoring push status

### What's verified working
- ✅ Local `sbt sangeetServer/run` → `curl localhost:28080/metrics` returns Prometheus text
- ✅ All 122 server tests pass
- ✅ Cloud Run revision `sangeet-server-00012-277` is live (the first revision pushing successful time series)
- ✅ Live `https://sangeet-server-729103223940.asia-south1.run.app/metrics` returns 200
- ✅ Container starts cleanly: log says "Cloud Monitoring push: enabled (pushing to project sangeet-editor every 60s)"
- ✅ ~60 `custom.googleapis.com/jvm/*` metric descriptors registered with the correct schema (`service`, `version` as metric labels, not resource labels)
- ✅ Time series data points landing at 60s cadence — verified via Monitoring REST API: `custom.googleapis.com/jvm/threads/live` returns three consecutive points (07:42:38, 07:43:38, 07:44:38 UTC on 2026-06-11) with `doubleValue` matching live thread counts
- ✅ No `failed to send metrics` log lines after the first successful push cycle

### The debug journey (chronological)
Four distinct issues, each masking the next. Recording them here because each was non-obvious:

1. **Signed-JAR shrapnel from Google Cloud client libs** (PR #26) — fat-jar wouldn't load at all. Misleading "Could not find or load main class" error.
2. **SLF4J no-op binding silently swallowing all Micrometer/library errors** (PR #27) — for hours we couldn't tell *anything* was wrong from logs.
3. **gRPC default 10 KiB HTTP/2 inbound header limit too small for Cloud Monitoring's response headers** (PR #28, then PR #29 raising again to 1 MiB) — Cloud Run's OAuth token plus Cloud Monitoring's `grpc-status-details-bin` rejection details collectively exceed Netty's default.
4. **Cloud Monitoring `global` resource type rejects `service`/`version` as resource labels** (PR #30) — only `project_id` is allowed there. Service/version belong in metric labels, attached via `commonTags` on the composite registry. Once fixed, an additional first-write transient `INTERNAL` error self-resolved on the second push cycle (a normal feature of GCP's "create-descriptor-and-write-data" first-call semantics).

---

## Phase 2 — detailed status

### What's deployed
- `HttpMetrics` at `sangeet-server/src/main/scala/com/varpas/sangeet/server/metrics/HttpMetrics.scala` — custom `MetricsRequestInterceptor[IO]` backed by Micrometer
- Wired into `Main.scala` via `Http4sServerOptions.customiseInterceptors[IO].metricsInterceptor(HttpMetrics.requestInterceptor)`
- Emits three meter families on every Tapir-handled request:
  - `tapir.request.total{method, path, status_code}` — counter
  - `tapir.request.duration{method, path, status_code}` — timer (count/sum/max)
  - `tapir.request.active` — single global gauge (no labels — keeps cardinality flat)

### What's verified working
- ✅ Production `custom.googleapis.com/tapir/request/total` shows 6 distinct series after live traffic on Cloud Run revision `sangeet-server-00015-pqc`
- ✅ Path labels are *templates*, not literal URLs — `/api/v1/raags/{name}` collapses both `Yaman` hits and `NonExistent` 404 misses onto the same template, just split by `status_code`
- ✅ Service/version common tags from Phase 1 propagate onto these meters automatically

### Design note: why custom (not `tapir-prometheus-metrics`)
Tapir ships `tapir-prometheus-metrics`, `tapir-opentelemetry-metrics`, `tapir-otel4s-metrics`, `tapir-datadog-metrics`, `tapir-zio-metrics` — but **not** a Micrometer one. The Prometheus module backs onto a separate `io.prometheus.client.CollectorRegistry`; meters there wouldn't reach Cloud Monitoring at all. ~70 lines of custom code against Tapir's `Metric` SPI was the right trade-off to keep everything in the existing composite registry.

---

## Interlude — CI path filters (PR #33)

### What's deployed
A new `changes` job at the top of `.github/workflows/ci.yml` uses `dorny/paths-filter@v3` to classify the diff into four flags (`scala`, `elm`, `e2e`, `always`). Downstream jobs gate on them.

### Measured savings
| Change shape | Before | After |
|---|---|---|
| Docs-only edit | ~8 min | ~2 min |
| Backend metrics internal | ~8 min | ~3 min |
| Backend endpoint change | ~8 min | ~8 min (legit) |
| Pure Elm change | ~8 min | ~6 min |

### Note
- Wall-clock savings, not billable-Actions savings — skipped jobs still pay runner-startup overhead
- A skipped need is treated as success by default; e2e's `if:` makes this explicit (`needs.scala-tests.result == 'success' || needs.scala-tests.result == 'skipped'`)
- `dorny/paths-filter` compares against merge-base on PRs and previous commit on push-to-main; squash merges correctly scope to the squashed diff

---

## Phase 3 — detailed status

### What's deployed
- PostHog project **"Sangeet Web"** on US Cloud, public project API key `phc_B8gMXdb8...` (write-only, safe to commit)
- `posthog-js` loaded via inline CDN snippet in `sangeet-web/public/index.html`, init config: `autocapture: false`, `capture_pageview: true`, `respect_dnt: true`
- Two global capture handlers in `sangeet-web/public/ports.js` (ANALYTICS section):
  - `click` events — region derived from existing CSS class names (`.toolbar`, `.file-browser-panel`, `.canvas-area`, etc.) via `closest()` walk; element from nearest button/link with text-snippet fallback
  - `keystroke` events — modifier-only and `e.repeat=true` filtered; 25 ms burst debounce

### What's verified working
- ✅ Live events show up in PostHog → Sangeet Web → Activity → Live events within seconds of interaction in Chrome
- ✅ Region tags correctly attribute clicks to UI areas

### Caveats
- Ad blockers (uBlock, Brave Shields, AdGuard) and `Do Not Track`-enabled browsers will silently drop events — this is by design (`respect_dnt: true`). Users won't see PostHog records from those browsers; not a bug.

---

## Phase 4 + 4b — detailed status

### What's deployed
- **rrweb 2.0.0-alpha.4** loaded from jsDelivr CDN in `index.html`
- **5-min rolling buffer** in `ports.js` (REPLAY BUFFER section): time-based eviction is the primary policy; 10 MiB hard cap is defensive
- Per-event size tracked via `event.__sz = JSON.stringify(event).length` on ingest so the byte counter is O(1) per emit instead of re-serializing the whole buffer
- Dev hooks: `window.__replay.{events,stats,clear}`
- **Report Bug UI** (Phase 4b): 🐞 button in toolbar between Properties and About → modal with description (required) + email (optional) + privacy disclosure → Send wires through `Ports.submitBugReport` outbound, then JS gathers buffer + browser metadata + POSTs to `/api/v1/bug-reports`, then `Ports.bugReportResult` inbound delivers `{success, message}` back to Elm which surfaces it in the status bar

### What's verified working
- ✅ After ~30s of interaction, `window.__replay.stats()` shows `count > 0` and `sizeBytes` in the tens of KB
- ✅ Buffer events ageMs grows toward 5 min then ages out
- ✅ Click 🐞 → fill in → Send → status bar shows "Bug report sent — thanks! (report id …)" — round-trip works

### Design note: replay buffer travels through JS, not Elm
A 5-min rrweb buffer can be several MB. Round-tripping through Elm would require two extra JSON serialization passes (Elm decode → re-encode for outbound port → re-encode for HTTP). Instead, Elm sends only `{description, email, apiBaseUrl}` outbound; JS reads `window.__replay.events()` locally, assembles the full payload, and POSTs. Inbound port carries back only `{success, message}`.

---

## Phase 5b — detailed status

### What's deployed
- `GitHubIssuesClient` trait + three impls at `sangeet-server/src/main/scala/com/varpas/sangeet/server/bugreports/GitHubIssuesClient.scala`:
  - `HttpGitHubIssuesClient` — uses JDK 11's `java.net.http.HttpClient`, no new dep
  - `DisabledGitHubIssuesClient` — no-op returning `Left(...)`, used when env vars are absent
  - `fromEnv` factory picks based on presence of `GITHUB_REPO` + `GITHUB_TOKEN`
- `IssueBuilder` pure function at the same package: payload JSON → `(title, body, labels)`. Title = `Bug report — <first 60 chars of description>`. Body = markdown summary (description, email, platform, browser metadata, replay-event count, GCS console link).
- `BugReportRoutes.createBugReport(storage, issues, gcsBucket)` now takes both deps; after a successful GCS write, spawns a fiber (`fileIssue.start`) that posts to GitHub. The user's POST returns as soon as GCS succeeds — GitHub latency does not affect the response.
- Labels emitted: `bug`, `from-user`, `platform-web` (the platform label is derived from the payload's `type` field, so `platform-desktop` will land cleanly when Phase 8 ships).

### What's verified working
- ✅ Live revision `sangeet-server-00016-5hn` has `GITHUB_TOKEN=secret://github-issues-token:latest` + `GITHUB_REPO=bharath12345/sangeet_notes_editor` env vars set
- ✅ End-to-end: POST to live `/api/v1/bug-reports` → 200 with reportId, GCS object lands (byte-exact), GitHub issue #41 created with correct title, all 3 labels, markdown body, and working GCS console link in the "Full payload" section
- ✅ Two new labels (`from-user`, `platform-web`) were auto-created by GitHub on first use — no pre-seeding required
- ✅ 13 new tests (9 `IssueBuilderSpec` + 4 `BugReportRoutesSpec`) bring the server suite to 136 passing

---

## Phase 5a — detailed status

### What's deployed
- `POST /api/v1/bug-reports` Tapir endpoint accepts arbitrary JSON body (schema intentionally open while web rrweb-shape and desktop action-log-shape clients are still being designed)
- `BugReportStorage` trait + two impls:
  - `GcsBugReportStorage` writes to GCS via google-cloud-storage 2.40.1, ADC-authenticated (same auth path Phase 1 already validated)
  - `UnconfiguredBugReportStorage` returns `Left("not configured")` when `BUG_REPORTS_BUCKET` env var is unset — explicit error, not a silent black hole
- Response shape: `{"reportId": "<uuid>", "status": "received"}` on success, 503 with `{error, message}` on failure

### What's verified working
- ✅ `curl -X POST .../api/v1/bug-reports -d '{...}'` returns 200 with reportId
- ✅ `<reportId>.json` lands in `gs://sangeet-bug-reports/` with byte-exact body match
- ✅ Live revision `sangeet-server-00015-pqc` has `BUG_REPORTS_BUCKET=sangeet-bug-reports` env var set
- ✅ Bucket lifecycle: 90-day auto-delete confirmed via `gcloud storage buckets describe`

---

## Setup commands run on GCP (canonical reference)

### Phase 1 — Cloud Monitoring setup (2026-06-11)

```bash
# 1. Enable the Cloud Monitoring API
gcloud services enable monitoring.googleapis.com

# 2. Grant the Cloud Run runtime SA the role it needs to write metrics + descriptors
PROJECT_NUMBER=$(gcloud projects describe sangeet-editor --format='value(projectNumber)')
gcloud projects add-iam-policy-binding sangeet-editor \
    --member="serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com" \
    --role="roles/monitoring.metricWriter" \
    --condition=None

# 3. Tell the service which project to push to (env var triggers MetricsRegistry to construct the Stackdriver registry)
gcloud run services update sangeet-server \
    --region asia-south1 \
    --set-env-vars=GCP_PROJECT_ID=sangeet-editor

# 4. (debug) Disable CPU throttling so the Micrometer push thread can run between requests
gcloud run services update sangeet-server \
    --region asia-south1 \
    --no-cpu-throttling
```

### Phase 5a — GCS bug-report bucket setup (2026-06-11)

```bash
# 1. Create the bucket in the same region as Cloud Run, uniform IAM
gcloud storage buckets create gs://sangeet-bug-reports \
    --location=asia-south1 \
    --uniform-bucket-level-access

# 2. Auto-delete reports older than 90 days
cat > /tmp/bug-reports-lifecycle.json <<'EOF'
{ "lifecycle": { "rule": [
    { "action": {"type": "Delete"}, "condition": {"age": 90} }
] } }
EOF
gcloud storage buckets update gs://sangeet-bug-reports \
    --lifecycle-file=/tmp/bug-reports-lifecycle.json

# 3. Grant Cloud Run runtime SA write access on this bucket only (least privilege)
gcloud storage buckets add-iam-policy-binding gs://sangeet-bug-reports \
    --member="serviceAccount:729103223940-compute@developer.gserviceaccount.com" \
    --role="roles/storage.objectAdmin"

# 4. Enable the GcsBugReportStorage impl on Cloud Run
gcloud run services update sangeet-server \
    --region asia-south1 \
    --update-env-vars=BUG_REPORTS_BUCKET=sangeet-bug-reports
```

### Phase 5b — GitHub Issues integration (2026-06-11)

```bash
# 1. Enable Secret Manager API (it's off by default on new projects)
gcloud services enable secretmanager.googleapis.com --project=sangeet-editor

# 2. Create the secret container (no value yet)
gcloud secrets create github-issues-token \
    --replication-policy=automatic \
    --project=sangeet-editor

# 3. Store the fine-grained PAT as version 1 — read from stdin to keep
#    the value out of shell history. Create the PAT first at:
#    https://github.com/settings/personal-access-tokens/new
#    Repository access: only bharath12345/sangeet_notes_editor
#    Permissions: Issues = Read and write
printf '%s' "$GITHUB_PAT" | gcloud secrets versions add github-issues-token \
    --data-file=- \
    --project=sangeet-editor

# 4. Grant the Cloud Run runtime SA permission to read this one secret
gcloud secrets add-iam-policy-binding github-issues-token \
    --member="serviceAccount:729103223940-compute@developer.gserviceaccount.com" \
    --role="roles/secretmanager.secretAccessor" \
    --project=sangeet-editor

# 5. Mount the secret as GITHUB_TOKEN and set the repo env var
gcloud run services update sangeet-server \
    --region=asia-south1 \
    --project=sangeet-editor \
    --update-secrets=GITHUB_TOKEN=github-issues-token:latest \
    --update-env-vars=GITHUB_REPO=bharath12345/sangeet_notes_editor
```

---

## Gotchas + lessons learned

### `micrometer-registry-stackdriver` brings in signed jars
Google Cloud Java client libraries are signed. Their `META-INF/SIGNINGC.{SF,RSA}` files end up in the fat jar. Once sbt-assembly merges classes from other jars, the signature no longer matches the contents, and the JVM refuses to load the jar at all with the misleading error `Could not find or load main class com.varpas.sangeet.server.Main`. **Fix:** discard `*.SF`, `*.RSA`, `*.DSA`, `*.EC` in `META-INF/` in the assembly merge strategy. Resolved in PR #26.

### Cloud Run CPU throttling kills background push threads
By default Cloud Run only allocates CPU during request handling. With `min-instances=0`, the instance stays alive ~5 minutes after the last request but CPU is throttled during the idle. Micrometer's push thread can't run, so metric pushes silently get skipped. **Fix:** `gcloud run services update sangeet-server --no-cpu-throttling`. Costs are unchanged when the instance scales to zero; only incurs while warm.

### Grafana Cloud Free's 14-day "trial" is a UX confusion
What looks like a 14-day Pro trial in the signup flow is exactly that — after 14 days the account auto-downgrades to the actual Free tier (which is permanent). What ALSO exists, separately, is the Free tier's 14-day data retention — metrics ingested into Grafana Cloud's Prometheus get aged out after 2 weeks. We sidestep both by using Cloud Monitoring as the storage layer and Grafana Cloud (later) only as a viewer reading from Cloud Monitoring as a data source.

### SLF4J no-op binding silently eats library error logs (confirmed)
Without an SLF4J impl on the classpath, any logger inside Micrometer / Stackdriver / google-cloud-monitoring SDKs throws away its output. Push failures were completely invisible — we had no way to see *why* nothing was being written. **Fix:** added `org.slf4j:slf4j-simple:2.0.13` in PR #27. Made the next three layers of bugs diagnosable from `gcloud run services logs read`. Worth doing **first** on any new JVM service that uses cloud-vendor SDKs.

### gRPC default 10 KiB inbound HTTP/2 header cap is too small for Cloud Monitoring
Two layers contributed:
1. Cloud Run's metadata-server-issued OAuth tokens are unusually large (Cloud-Run-specific identity claims).
2. Cloud Monitoring's `CreateTimeSeries` partial-failure responses include `grpc-status-details-bin` — a base64-encoded protobuf enumerating every rejected time series. For ~60 JVM series, this blob alone can run tens of KiB.

The `io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2Exception: Header size exceeded max allowed size (10240)` error is what you see if either limit is hit. **Fix:** configure `StackdriverMeterRegistry.builder(cfg).metricServiceSettings(...)` with an `InstantiatingGrpcChannelProvider` whose `channelConfigurator` calls `NettyChannelBuilder.maxInboundMetadataSize(1024 * 1024)`. PRs #28 + #29 (we walked it up: 32 KiB wasn't enough, 1 MiB is plenty).

### Cloud Monitoring's `global` monitored resource type only accepts `project_id` as a label
Don't override `StackdriverConfig.resourceLabels` with arbitrary metadata like `service` or `version` — Cloud Monitoring rejects unrecognized resource labels with `INVALID_ARGUMENT: unrecognized resource label "version"`. Each monitored resource type has a *fixed* label schema (defined by GCP). For `global` it's `project_id` only.

If you want service/version dimensions on your metrics (and you do — they're how you split a dashboard by deployment), attach them as **metric labels** via `registry.config().commonTags("service", "...", "version", "...")` on the composite. They become queryable metric labels in Cloud Monitoring without colliding with the resource-type schema. PR #30.

### Cloud Monitoring first-write `INTERNAL` error is genuinely transient
On the very first push after a descriptor schema change (e.g., labels move from resource-side to metric-side), `CreateTimeSeries` can return `INTERNAL: write for resource failed: Internal error encountered. Please retry after a few seconds.` on every series in the batch — looks catastrophic. It's actually GCP reconciling the descriptor update; the next push (60s later) succeeds cleanly. Don't chase it.

### Tapir has no Micrometer metrics module — write your own
Tapir publishes integrations for Prometheus, OpenTelemetry, otel4s, Datadog, and zio-metrics, but no `tapir-micrometer-metrics` artifact exists. The Prometheus module backs onto a separate `io.prometheus.client.CollectorRegistry` — meters there *won't* reach a Micrometer composite. Implementing Tapir's `Metric` SPI directly against `io.micrometer.core.instrument.MeterRegistry` takes ~70 lines (see `HttpMetrics.scala`) and keeps everything in one composite registry, so meters flow to both Prometheus exposition and Cloud Monitoring push automatically. PR #32.

### PostHog is silently blocked by ad blockers + DNT
A page can have a working PostHog snippet and *still* show "no live events" in the dashboard if the browser:
1. Runs uBlock Origin / AdGuard / Brave Shields / Privacy Badger — all of these have PostHog domains on their blocklists by default
2. Has Do Not Track enabled — we set `respect_dnt: true` in `posthog.init`, so PostHog will silently skip capture

Diagnosis steps to share with users: open devtools → Network tab → filter for `posthog` → request status. If `blocked:other`, it's an extension. If no requests at all but `posthog.capture` returns truthy, suspect DNT.

### rrweb buffer should travel through JS, not Elm
A 5-min rrweb buffer can easily be 1-10 MB. Sending it from JS to Elm (encode-decode), back from Elm to JS via outbound port (encode), then JSON.stringify for fetch (encode) is three serialization passes through the Elm runtime. Keep the buffer in JS, have Elm only emit `{description, email, apiBaseUrl}` outbound, then JS reads the buffer locally and POSTs. Inbound port carries only `{success, message}` back. PR #37.

### Phase 5a `BugReportStorage` trait pattern — testability without real GCS
Don't `new StorageOptions.getDefaultInstance.getService` at object-load time — tests will fail at class load because there's no ADC available. Wrap in `lazy val` (so absence at load doesn't fail) AND extract behind a trait (so tests can inject an in-memory fake). The `UnconfiguredBugReportStorage` impl returning `Left("not configured")` is a deliberate choice over a silent no-op: missing local env config becomes a visible 503 instead of a black hole where reports disappear.

### Phase 5b GCS is the source of truth, GitHub is the index
The bug-report POST returns 200 as soon as the GCS write succeeds; the GitHub call runs in a background fiber and never affects the response. Rationale: a slow GitHub API or a revoked PAT must not cause user submissions to fail. GCS holds the full payload (rrweb replay + metadata); the GitHub issue is a human-friendly index that links back to the GCS console. If GitHub is down the issue just doesn't get filed — `gcloud storage ls gs://sangeet-bug-reports/` is the recovery path.

### Phase 5b storing PAT in Secret Manager — pipe via `printf '%s'` to stdin
`gcloud secrets versions add --data-file=-` reads the *raw bytes* of stdin as the secret value. `echo "$TOKEN"` adds a trailing newline that ends up inside the secret and silently breaks the GitHub Authorization header at runtime; `printf '%s' "$TOKEN"` does not. Also keeps the token out of any file on disk and out of shell history (unlike `--data-file=/tmp/token`).

### Phase 5b GitHub auto-creates labels on first use
We file issues with `labels: ["bug", "from-user", "platform-web"]`. Only `bug` existed in the repo. GitHub created the other two on the fly with default colors. No pre-seeding step needed before deploying — issue creation is idempotent on label existence.

### Phase 5b fire-and-forget = `.start.as(...)`, not `.attempt.map(...)`
For "ignore the result, don't block the caller", `cats.effect.IO` gives you `op.start` which immediately returns a `Fiber` and lets the original effect continue. Wrap the inner op in `.handleErrorWith(t => IO.println(...))` first so a thrown exception inside the fiber gets logged rather than swallowed. `.attempt.map` would still block the caller waiting for completion.

### Cleanup pattern for end-to-end verification
After verifying a new endpoint against prod, close the test issue and delete the test GCS object so they don't pollute the real triage queue. Cheap and easy to forget. Worth scripting as part of any future smoke-test job.

---

## Cost actuals

Tracked monthly. Goal: stay $0.

| Date | Item | Charge | Source |
|---|---|---|---|
| 2026-06 | Cloud Monitoring custom metric ingestion (JVM + HTTP series, ~140 series × 60s push) | $0 | Within 150 MiB/month free tier (using ~15-20 MiB/month) |
| 2026-06 | GCS bucket `sangeet-bug-reports` storage + ops | $0 | First 5 GB stored + 5 GB egress/month free |
| 2026-06 | PostHog Cloud "Sangeet Web" events | $0 | Free 1M events/month |
| 2026-06 | Cloud Run requests / CPU-second | $0 | Within free tier (low traffic) |

---

## Document update conventions

- Bump the "Last updated" date at the top whenever this file is edited
- Mark phases with their current status in the table above
- Add new `## Phase N — detailed status` sections as each phase enters in-progress
- Move resolved gotchas into the "Gotchas + lessons learned" section so future-me doesn't repeat them
- Reference PR numbers when describing fixes so the git log is one click away
