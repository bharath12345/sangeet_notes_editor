# Plan 12: Cross-Platform Observability & Session Replay

## Context

The web app is now live — frontend on GitHub Pages, backend on Cloud Run, both wired into auto-deploy CI. The desktop app has been the primary platform throughout and now has real users too. Both audiences are non-technical (sitar students, music teachers, learners). They can't be asked to send HAR files, screen recordings are painful to view and reproduce from, and most won't write a useful bug report on their own.

We need four things:

1. **Backend metrics** — to know what the API is doing under load: which endpoints are hot, which are slow, JVM health, request volume by category.
2. **Frontend metrics (web + desktop)** — to understand actual product usage: which UI regions get used, which features are abandoned, what keys users press, how fast actions feel.
3. **Web session replay** — when a web user hits a bug, capture enough to reproduce the issue without needing them to articulate it: their inputs *and* what the DOM rendered in response.
4. **Desktop bug reporting + crash capture** — an equivalent for desktop users: rolling buffer of user actions + state snapshots + screenshot, plus auto-capture on JVM crashes so we don't lose the bugs that kill the app before the user can report them.

All four route bug reports to the same backend endpoint and into the same GitHub Issues tracker. Web and desktop diverge only in *how* they capture data (browser rrweb vs. JavaFX event log) — everything downstream of the upload is shared.

This plan covers all four as separate but coordinated workstreams. Each can ship independently. Recommended execution order is sequential (web backend metrics → web frontend metrics → web session replay → desktop bug reporting → desktop crash capture → desktop metrics) because complexity escalates and earlier wins build the muscle for later phases.

---

## Goals

- **Visibility**: I can answer "how is the app doing right now?" without SSHing anywhere or reading logs.
- **Diagnosis**: when a user reports a bug, I have enough data to reproduce it without going back and forth.
- **Per-feature insight**: I can see which UI features are used vs. ignored — informs future feature work.
- **Free or near-free**: stays in free tiers for the foreseeable future (low-traffic personal project).
- **GCP-friendly**: leverages the existing project where it makes sense; avoids spinning up infra we don't need.

## Non-goals

- Multi-tenant analytics (just me viewing the data, not a team).
- HIPAA / GDPR-strict compliance (not collecting PII beyond what users voluntarily type into their compositions).
- Real-time alerting paging me at 3am (the app is non-critical — daily check is fine).
- Replacing logging — Cloud Run already collects structured logs; we're adding metrics + replay on top.

---

## Decisions locked during brainstorming

These seven foundational choices were made before writing this plan. They constrain everything below.

| # | Decision | Rationale |
|---|---|---|
| 1 | **Hybrid data storage.** Session replays + desktop bug reports live in our own GCS bucket; aggregate metrics (clicks, counters, response times) live in SaaS free tiers. | Replays are the most sensitive (full DOM + inputs); keeping them in GCS means we own them forever, control retention, can switch viewers anytime. Aggregate metrics are low-sensitivity and getting hosted dashboards saves us from running Grafana ourselves. |
| 2 | **Best-tool-per-domain for metrics.** Backend → Cloud Monitoring (storage) + Grafana Cloud as a viewer-only data source. Web frontend → PostHog. Desktop → PostHog-Java. | User explicitly wants Prometheus-style metrics + Grafana dashboards for backend. Cloud Monitoring stores the metrics (longer retention, fully GCP-native, no external vendor for the data itself); Grafana Cloud Free reads from Cloud Monitoring as a data source — Grafana hosts only the dashboard config, not the metric data, sidestepping its 14-day Free-tier retention limit. Frontend/desktop are product-analytics-shaped (clicks/events/funnels/menu usage) — PostHog is built for that and saves writing every chart from scratch in Grafana. PostHog-Java is the natural counterpart for the desktop side. |
| 3 | **Always-on rolling buffer + Report Bug button (web).** rrweb records continuously into a 5-min browser-memory buffer; nothing leaves the browser until the user clicks Report a Bug. | Pure record buttons have a fatal flaw: users hit a bug, *then* think to record. Rolling buffer captures the pre-bug context. Still user-triggered for the upload — nothing leaves the browser without explicit consent. |
| 4 | **GitHub Issue auto-created on bug report.** Backend writes replay/report payload to GCS, then files a GitHub Issue with the user's description + a link to the viewer. Same endpoint serves web and desktop. | Issues live where the code lives — same triage tool I already use, comments + labels + close-on-fix. Free. The replay/report link is one click from the issue. |
| 5 | **Desktop rolling buffer = action log + state snapshots + ONE screenshot at report time.** EventLogger wraps EditorApi calls + JavaFX scene events; Composition snapshots every 30s; `Scene.snapshot()` captures the JavaFX window at the moment user clicks Report a Bug. | EditorApi is pure → action log is replayable *as state* in my IDE (not just watchable). Screenshot covers visual bugs that state-replay misses (rendering glitches, layout). Continuous video would be 20× larger for marginal extra signal. Payload stays under 700 KB typical. |
| 6 | **Auto-capture on desktop crashes.** A `Thread.setDefaultUncaughtExceptionHandler` dumps the current state + buffer + stack trace to `~/.sangeet/crash-pending/<UUID>.json` before the JVM exits. On next launch, the app detects pending crash files and prompts the user: *"Sangeet didn't shut down cleanly last time — would you like to send a report?"* | The bugs that kill the app are exactly the ones we most want to know about, and they're exactly the ones the user can't manually report (the app is dead). User retains full control — auto-capture only triggers the prompt; user decides Send / View / Discard. |
| 7 | **Desktop usage metrics via PostHog-Java.** Telemetry hooks at menu/shortcut/dialog entry points capture which features get used. | Free tier covers it. Gives us parity with web product analytics. Without it, we'd have no signal on desktop feature adoption — the existing user count is small enough that "which features do they actually touch?" is a real open question. |
| 8 | **Web and desktop metrics live in separate PostHog projects** ("Sangeet Web" and "Sangeet Desktop"), each with its own API key, dashboards, and free tier allocation. **Bug reports stay shared** in one GCS bucket + one GitHub Issues stream, but each report is labeled `platform-web` or `platform-desktop` for filtering. | Metrics get genuinely separate views (no accidental "all events" cross-platform mixing in dashboards) which is the right call when you'll ask platform-specific questions often. Bug reports are inherently small and clearly labeled — keeping them co-located gives one triage queue, and the label makes filtering trivial. |

---

## Architecture

### Web

```
                            ┌─────────────────────────────────────────────────┐
                            │   BROWSER (Elm + JS, served from GitHub Pages)  │
                            │                                                 │
                            │  ┌────────────┐  ┌────────────┐  ┌────────────┐ │
                            │  │ rrweb      │  │ PostHog    │  │ Bug-report │ │
                            │  │ recorder   │  │ events SDK │  │ modal      │ │
                            │  │ (5min buf) │  │            │  │            │ │
                            │  └─────┬──────┘  └─────┬──────┘  └─────┬──────┘ │
                            └────────│───────────────│───────────────│────────┘
                                     │               │               │
                                     │               │               │ JSON
                                     │  (only on     │               │ blob +
                                     │   bug report) │               │ metadata
                                     │               │               │
                                     │               ▼               ▼
                                     │      ┌────────────────┐  ┌────────────────────┐
                                     │      │  PostHog Cloud │  │ Cloud Run          │
                                     │      │  (free tier)   │  │ sangeet-server     │
                                     │      │                │  │                    │
                                     │      │ events/clicks  │  │ POST /bug-reports  │
                                     │      │ → dashboards   │  │ ──────┬──────────  │
                                     │      └────────────────┘  │       │            │
                                     │                          │       ▼            │
                                     └─────────────────────────►│  GCS bucket        │
                                                                │  sangeet-bug-      │
                                                                │  reports/<uuid>    │
                                                                │       │            │
                                                                │       ▼            │
                                                                │  GitHub Issues API │
                                                                │  (auto-file issue) │
                                                                │                    │
                                                                │  /metrics endpoint │
                                                                │  (Micrometer       │
                                                                │   Prometheus, for  │
                                                                │   local debugging) │
                                                                │       │            │
                                                                └───────│────────────┘
                                                                        │ Stackdriver push every 60s
                                                                        ▼
                                                                ┌──────────────────┐
                                                                │ GCP Cloud        │
                                                                │ Monitoring       │
                                                                │ (storage,        │
                                                                │  6 wks-24 mo     │
                                                                │  retention)      │
                                                                └─────────┬────────┘
                                                                          │ data source query
                                                                          ▼
                                                                ┌──────────────────┐
                                                                │ Grafana Cloud    │
                                                                │ (free tier;      │
                                                                │ dashboards only, │
                                                                │ reads from Cloud │
                                                                │ Monitoring)      │
                                                                └──────────────────┘
```

### Desktop

```
       ┌─────────────────────────────────────────────────────────────────────┐
       │  DESKTOP APP (Scala 3 + ScalaFX, runs locally on user's machine)    │
       │                                                                     │
       │  ┌─────────────────────┐  ┌────────────┐  ┌─────────────────────┐   │
       │  │ EventLogger         │  │ PostHog-   │  │ Bug-report dialog   │   │
       │  │ (wraps EditorApi +  │  │ Java SDK   │  │ + crash-recovery    │   │
       │  │  JavaFX scene       │  │            │  │  dialog on startup  │   │
       │  │  events; 5min       │  │            │  │                     │   │
       │  │  rolling ring)      │  │            │  │                     │   │
       │  └──────────┬──────────┘  └─────┬──────┘  └──────────┬──────────┘   │
       │             │                   │                    │              │
       │             │                   │                    │ JSON blob    │
       │             │                   │                    │ + Composition│
       │             │ (only on Report   │                    │ + screenshot │
       │             │  Bug click OR     │                    │ + stack trace│
       │             │  next-launch      │                    │ (if crash)   │
       │             │  after crash)     │                    │              │
       │             │                   │                    │              │
       │      ┌──────┴──────┐            │                    │              │
       │      │ ~/.sangeet/ │            │                    │              │
       │      │ crash-      │            │                    │              │
       │      │ pending/    │            │                    │              │
       │      │ <UUID>.json │            │                    │              │
       │      │ (only on    │            │                    │              │
       │      │  crash)     │            │                    │              │
       │      └──────┬──────┘            │                    │              │
       └─────────────│───────────────────│────────────────────│──────────────┘
                     │                   │                    │
                     │     ┌─────────────▼──────────┐         │
                     │     │  PostHog project       │         │
                     │     │  "Sangeet Desktop"     │         │
                     │     │  (separate from the    │         │
                     │     │  web project; own key, │         │
                     │     │  own dashboards)       │         │
                     │     └────────────────────────┘         │
                     │                                        │
                     └─────────► POST /api/v1/bug-reports ◄───┘
                                 (same endpoint as web)
                                          │
                                          ▼
                                 (same GCS + GitHub Issues flow
                                  as web — see diagram above)
```

The backend `/api/v1/bug-reports` endpoint and everything downstream (GCS, GitHub Issues, replay viewer) are **shared** between web and desktop. Only the payload format differs: web sends `rrwebEvents[]`, desktop sends `actionLog[] + compositionSnapshots[] + screenshotPng`. The endpoint handles both shapes via a `type: "web" | "desktop"` discriminator.

---

## Phase 1 — Backend metrics infrastructure (Micrometer → Cloud Monitoring)

**Goal:** sangeet-server exposes `/metrics` in Prometheus text format (for local debugging) and pushes the same data to GCP Cloud Monitoring (for production storage). Verify metrics land in Cloud Monitoring after deploy.

> **Why Cloud Monitoring as storage, not Grafana Cloud:** Grafana Cloud Free has a 14-day retention limit on metrics ingested into its Prometheus. Cloud Monitoring's free tier stores metrics 6 weeks to 24 months depending on type — much longer. Grafana Cloud comes in later (Phase 3-equivalent for backend) as a *viewer only* via Cloud Monitoring data source; no metric data flows through Grafana's storage, so the 14-day limit becomes irrelevant.

### Tasks

1. **Add dependencies** to `build.sbt` (`sangeetServer` block):
   - `io.micrometer:micrometer-core` (counters/timers/gauges API)
   - `io.micrometer:micrometer-registry-prometheus` (Prometheus text format for `/metrics`)
   - `io.micrometer:micrometer-registry-stackdriver` (push to Cloud Monitoring)

2. **Create `sangeet-server/src/main/scala/.../metrics/MetricsRegistry.scala`** — singleton holding:
   - A `PrometheusMeterRegistry` (always on — for `/metrics` local debugging)
   - A `StackdriverMeterRegistry` (constructed only if `GCP_PROJECT_ID` env var is set; pushes to Cloud Monitoring every 60s — 60s is the GCP-side minimum)
   - A `CompositeMeterRegistry` combining them so any custom instrumentation lands in both
   - Standard JVM bindings: `JvmMemoryMetrics`, `JvmGcMetrics`, `JvmThreadMetrics`, `ClassLoaderMetrics`, `ProcessorMetrics`, `FileDescriptorMetrics`, `UptimeMetrics`

3. **Add `GET /metrics` endpoint** in `Main.scala`:
   - Returns `MetricsRegistry.scrape()` with `Content-Type: text/plain; version=0.0.4`
   - Used for local sanity checks; also lets future GMP-on-Cloud-Run scrape if we ever want pull-based ingestion as a backup

4. **Cloud Monitoring push setup**:
   - Auth is automatic on Cloud Run via Application Default Credentials (the metadata server provides a token); no env-var secret needed
   - Only env var required: `GCP_PROJECT_ID=sangeet-editor`
   - No-op locally when the env var is absent (so dev / tests don't try to talk to GCP)
   - Cloud Run runtime SA needs `roles/monitoring.metricWriter` — grant once during setup

5. **GCP-side setup (one-time)**:
   - Enable the Cloud Monitoring API: `gcloud services enable monitoring.googleapis.com`
   - Grant the Cloud Run runtime SA write access:
     ```bash
     PROJECT_NUMBER=$(gcloud projects describe sangeet-editor --format='value(projectNumber)')
     gcloud projects add-iam-policy-binding sangeet-editor \
         --member="serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com" \
         --role="roles/monitoring.metricWriter" \
         --condition=None
     ```
   - Add `GCP_PROJECT_ID=sangeet-editor` to Cloud Run env vars:
     ```bash
     gcloud run services update sangeet-server \
         --region asia-south1 \
         --set-env-vars=GCP_PROJECT_ID=sangeet-editor
     ```

6. **Verify**:
   - Local: `sbt sangeetServer/run`, `curl localhost:28080/metrics` returns Prometheus text (note startup log says "Cloud Monitoring push: disabled" since `GCP_PROJECT_ID` isn't set locally — that's correct)
   - Production: after deploy + env-var update, in GCP Console → Monitoring → Metrics Explorer → search "custom" → should see metrics like `custom.googleapis.com/jvm/memory/used` within ~2 min of first push
   - Or via gcloud: `gcloud monitoring metrics list --filter="metric.type:custom.googleapis.com/jvm"`

### Success criteria
- `/metrics` returns valid Prometheus text format with 30+ default metric series (we get ~80)
- Cloud Monitoring shows JVM heap and request-count metrics within 2 minutes of a deploy
- One sample Cloud Monitoring chart shows live JVM heap usage

### Risks
- **Custom-metric naming on Cloud Monitoring**: Stackdriver registry prefixes everything with `custom.googleapis.com/`. Cardinality limits apply: 100k active time series per project on free tier (no chance of hitting it at our scale).
- **Cloud Run scale-to-zero kills the push thread during idle** — when the instance shuts down, the push thread dies. Data is missing for the idle period; metrics show "gaps", not "zero". Fine for a low-traffic app; flag it on the dashboard ("expect gaps when idle").
- **60s push interval is the GCP minimum** for custom metrics — finer granularity gets rejected. Not a problem for our use case.

---

## Phase 2 — Custom backend metrics

**Goal:** track the five backend asks (API call count by path, by param, by method, by category, system stats).

### The cardinality trap (read this first)

Prometheus metrics are cheap until they aren't. Every unique combination of label values creates a new "series". 10 paths × 4 methods = 40 series — fine. 10 paths × 4 methods × every-distinct-user-input = millions — your bill explodes and queries time out.

**Rule:** label values must be a small enumerable set. Path names ✓. HTTP methods ✓. Raag names (we have 26) ✓. Free-text fields (composition title, sahitya) ✗.

### Tasks

1. **HTTP request middleware** in `sangeet-server` — wrap all Tapir endpoints:
   - Increment counter `http_requests_total{path, method, category, status}` per request
   - Record timer `http_request_duration_seconds{path, method, category}` per request
   - **`path`** = the *route template* (e.g., `/api/v1/raags/{name}`), NOT the actual URL — keeps cardinality bounded
   - **`category`** = derived from path prefix: `taals`, `raags`, `rendering`, `compositions`, `editor`, `cursor`, `editor.section`, etc.
   - **`status`** = "2xx", "4xx", "5xx" (buckets, not raw codes — saves cardinality)

2. **Per-param tracking** for high-value params (the user's ask #2):
   - For `/api/v1/raags/{name}` — counter `raag_requests_total{name}` (26 raags, fine)
   - For `/api/v1/taals/{name}` — counter `taal_requests_total{name}` (11 taals, fine)
   - For `/api/v1/compositions/parse` — count by composition type if available (4-5 types, fine)
   - **Don't** label by user-supplied strings, IDs, or content

3. **System metrics** — already covered by JVM bindings in Phase 1. Verify dashboards show: heap used/committed, GC pause time, thread count, CPU process load, file descriptors. Disk util is N/A on Cloud Run (ephemeral filesystem).

4. **Dashboard** with 5 panels matching the user's asks — built in Grafana Cloud (set up Cloud Monitoring as a data source: Connections → Add data source → Google Cloud Monitoring → auth via a small read-only SA), or directly in Cloud Monitoring console if you prefer skipping Grafana entirely:
   - "API calls per path" (PromQL: `sum(rate(http_requests_total[5m])) by (path)`)
   - "Calls per param" (one panel per param-tracked endpoint)
   - "Calls per HTTP method" (`sum(rate(http_requests_total[5m])) by (method)`)
   - "Calls per category" (`sum(rate(http_requests_total[5m])) by (category)`)
   - "JVM health" (heap, GC, threads — split into 3 sub-panels)

### Success criteria
- All 5 dashboard panels render with real data within minutes of a deploy
- Cardinality stays under 200 series total (cheap, fast queries)
- I can answer "is anyone using the editor endpoints today?" by glancing at the dashboard

### Risks
- **Middleware ordering matters** — the metrics middleware must wrap the route table to see status codes. Putting it before/after CORS or the root-redirect can change what it measures. Test locally with a few curl commands.
- **Path-template extraction is Tapir-specific** — need to either use Tapir's `metricsInterceptor` (already exists for prometheus integration) or write a custom middleware. The former is the right tool.

---

## Phase 3 — Frontend metrics with PostHog

**Goal:** every user click is tagged with region + element; every keystroke counted; response time per user-perceived action measured. Dashboards in PostHog.

### Tasks

1. **Sign up for PostHog Cloud free** at posthog.com:
   - Free tier: 1M events/month, 1 year retention. Way more than we'd ever use.
   - **Create a project named "Sangeet Web"** in the account. The desktop will get its own project later (Phase 10) — strict separation, no mixing.
   - Note the project API key (starts with `phc_...`) — needs no GitHub secret; safe to put in client code.

2. **Add posthog-js**:
   - `cd sangeet-web && npm install posthog-js --save`
   - Or load via CDN: `<script>!function(t,e){...}</script>` in `index.html`
   - Initialize once in `index.html`: `posthog.init('phc_...', { api_host: 'https://app.posthog.com' })`

3. **Click tracking — by region + element**:
   - Add `data-region` (e.g., `toolbar`, `editor`, `file-browser`, `dialog`) on each top-level container in the Elm app
   - Add `data-element` (e.g., `save`, `new-composition`, `theme-toggle`) on every interactive element
   - Add a single global click handler in `ports.js`:
     ```js
     document.addEventListener('click', (e) => {
       const region = e.target.closest('[data-region]')?.dataset.region || 'unknown';
       const element = e.target.closest('[data-element]')?.dataset.element || e.target.tagName.toLowerCase();
       posthog.capture('click', { region, element });
     }, { capture: true });
     ```
   - The `closest()` walk handles clicks on inner text/icons inside a tagged button

4. **Keystroke tracking — by key**:
   - In `ports.js`, add `document.addEventListener('keydown', (e) => posthog.capture('keystroke', { key: e.key, region: ... }))`
   - Group `region` same way as clicks (which area was focused)
   - Drop high-frequency repeats (e.g., holding a key) — debounce or only capture key-down (not key-repeat)

5. **Response time per action**:
   - Wrap the API call functions in `sangeet-web/src/Api/*.elm` to emit a port-based timing event: action name, start time, end time
   - On Elm side: just call a `Ports.captureTiming` port at start and on response received
   - On JS side: `posthog.capture('action_timing', { action: name, durationMs: ... })`
   - Wrap the highest-value actions only (insert swar, save, load file, change starting beat) — not every API call

6. **PostHog dashboards** — create 4:
   - Clicks by region (pie chart, last 7 days)
   - Clicks by element within each region (table grouped by region)
   - Keystrokes by key (table, top 50)
   - Action timing histogram (p50/p95/p99 per action)

### Success criteria
- After clicking around the live site for 5 minutes, all 4 PostHog dashboards have real data
- I can answer "is the file-browser actually being used, or do users just open files via menus?" by glancing at the dashboard

### Risks
- **GDPR / privacy expectations** — even with anonymous events, you're logging user clicks. PostHog has a `respect_dnt` config to honor Do Not Track. Mention this in a small privacy note in the About dialog.
- **PostHog's auto-capture overlaps with our custom capture** — turn off `autocapture: false` in init so we only get our explicit events (cleaner data).
- **Identifying users across sessions** — by default PostHog uses anonymous IDs in localStorage. If users clear cookies, they look like new users. Fine for our use case (we want feature-usage trends, not user-level analytics).

---

## Phase 4 — Session replay: client side (rrweb + buffer + UI)

**Goal:** rrweb is always recording into a 5-min rolling browser-memory buffer. A "Report a Bug" button in the toolbar opens a modal that, on submit, packages the buffer + user description and POSTs to the backend.

### Tasks

1. **Add rrweb**:
   - `cd sangeet-web && npm install rrweb`
   - Or CDN: `<script src="https://cdn.jsdelivr.net/npm/rrweb@2/dist/record/rrweb-record.min.js"></script>`

2. **Recorder module** in `ports.js`:
   ```js
   let buffer = [];
   const MAX_AGE_MS = 5 * 60 * 1000; // 5 minutes
   rrweb.record({
     emit(event) {
       buffer.push(event);
       const cutoff = Date.now() - MAX_AGE_MS;
       buffer = buffer.filter(e => e.timestamp > cutoff);
     },
     // mask values inside <input type="password"> — we don't have any but defensive
     maskAllInputs: false,
     maskInputOptions: { password: true },
   });
   window.__getReplayBuffer = () => [...buffer]; // exposed for the bug-report flow
   ```

3. **Report a Bug button** in the toolbar:
   - New Elm `Msg`: `OpenBugReport`
   - Toolbar entry with a "🐞 Report a bug" label or icon (Material Design `mdi2b-bug-outline`)
   - On click → opens a modal

4. **Bug report modal** (Elm view):
   - Textarea: "What went wrong? What were you trying to do?" (required)
   - Optional email field: "Email so I can reach you with a fix? (optional)"
   - Note: "We'll include a 5-minute replay of your recent actions so I can reproduce the issue. No password fields are recorded."
   - Submit button
   - Cancel button

5. **Submit handler**:
   - Elm `BugReportSubmit` Msg → port call to JS: `Ports.submitBugReport({ description, email })`
   - JS handler:
     - Get current buffer: `const replay = window.__getReplayBuffer()`
     - Collect metadata: `{ url, userAgent, viewportW, viewportH, timestamp }`
     - POST to backend: `POST /api/v1/bug-reports`
     - Body: `{ description, email, replay, metadata }`
     - On success: show toast "Bug reported, thanks!"; on error: show toast with retry option

6. **Buffer size guardrail**:
   - Replay payloads can grow large for editing-heavy sessions (~few MB per 5 min of dense editing)
   - Hard cap: if `JSON.stringify(replay).length > 10 * 1024 * 1024` (10 MB), trim oldest events until under
   - This prevents the upload from blocking the browser for too long

### Success criteria
- Open the live site, edit a composition for 1 minute, click "Report a bug", type "test", submit → toast appears, network tab shows successful POST
- The replay payload is under 5 MB for typical 5-min sessions

### Risks
- **rrweb performance impact** — recording adds ~5-10% CPU and a few MB of memory. For an editor app this is fine (we're not Doom). Monitor for jank on slow machines.
- **DOM mutations from re-renders** — rrweb captures every DOM change. The Elm virtual DOM may produce noisy mutation streams. May need to tune `recordCanvas: false` and other options for size.
- **CSS not captured by default** — rrweb captures stylesheet links + inline styles. As long as we serve CSS from the same origin (we do), replay reproduces visual state.

---

## Phase 5 — Session replay: server side (GCS write + GitHub Issue)

**Goal:** backend endpoint receives bug reports, writes the replay to GCS, files a GitHub Issue, returns success.

### Tasks

1. **New GCS bucket** `sangeet-bug-reports` in `asia-south1`:
   - `gcloud storage buckets create gs://sangeet-bug-reports --location=asia-south1 --uniform-bucket-level-access`
   - Lifecycle policy: delete objects older than 90 days (configurable)
   - SA permissions: github-deployer needs `roles/storage.objectAdmin` on this bucket; runtime SA (Cloud Run default compute SA) needs `roles/storage.objectAdmin` to write reports

2. **GitHub Personal Access Token (fine-grained)**:
   - Create at github.com/settings/tokens?type=beta
   - Scope: just `bharath12345/sangeet_notes_editor`
   - Permission: `Issues: read and write`
   - Save to Cloud Run env var via `gcloud run services update sangeet-server --update-secrets=GITHUB_TOKEN=projects/.../secrets/github-issues-token:latest`
   - Use Secret Manager for the token (not plain env var)

3. **New Tapir endpoint** `POST /api/v1/bug-reports` in `sangeet-server`:
   - Body uses a discriminated union to handle both web and desktop shapes:
     ```
     { type: "web",     description, email?, replay: rrwebEvents[], metadata: { url, userAgent, viewport, ... } }
     { type: "desktop", description, email?, actionLog: [...], compositionSnapshots: [...],
                        screenshotPng: <base64>, metadata: { appVersion, osName, osVersion, javaVersion, displayResolution, ... } }
     ```
   - No auth (anyone can file a report; we want this open)
   - Body size limit: 15 MB (rejects oversized payloads at the http4s level — covers both web rrweb and desktop screenshot payloads with headroom)
   - Rate limit: per-IP, 5 reports per hour — protects against abuse via fail2ban-style in-memory map; if it gets gamed, escalate to Cloud Armor

4. **Implementation**:
   - Generate UUID for the report
   - Upload to GCS: `gs://sangeet-bug-reports/<UUID>.json` (full JSON: description, email, replay, metadata)
   - Generate replay viewer URL: `https://sangeet-server-729103223940.asia-south1.run.app/replay/<UUID>` (Phase 6 will serve this)
   - Call GitHub Issues API:
     - Title: `Bug report — <first 60 chars of description>`
     - Body markdown:
       ```
       **From user**: <email or "anonymous">
       **URL**: <metadata.url>
       **Browser**: <metadata.userAgent>
       **Viewport**: <w>×<h>
       **When**: <iso timestamp>
       
       ### What went wrong
       <description>
       
       ### Replay
       [▶ Watch the replay](https://.../replay/<UUID>)
       
       Report ID: <UUID>
       ```
     - Labels: `bug`, `from-user`, plus **`platform-web` or `platform-desktop`** based on the discriminator in the payload — gives one-click filtering in the Issues list
   - Return `{ reportId, status: "received" }`

5. **GCS client**:
   - Add `com.google.cloud:google-cloud-storage` to `build.sbt`
   - Use Application Default Credentials (works on Cloud Run via the metadata server — no manual setup)

6. **GitHub client**:
   - Hand-write an http4s call (small, one endpoint) rather than pulling a library — the GitHub Issues API is straightforward

### Success criteria
- Submit a bug report from the live site → file appears in GCS bucket → issue appears in the repo within 30 seconds
- Failed GitHub call (auth issue, network) — GCS write still succeeds; issue creation retries up to 3 times; failure logged but the user response is still success

### Risks
- **Secrets in env vars** — use Secret Manager, not plain `--set-env-vars`. Otherwise the token shows in `gcloud run services describe` output.
- **GitHub rate limits** — fine-grained PAT allows 5000 req/hour. We won't hit it.
- **Large payloads + Cloud Run timeout** — uploading 10 MB of replay JSON might exceed Cloud Run's 60s request timeout on slow connections. Mitigation: increase to 300s for this one endpoint, or use signed URL upload from client direct to GCS (cleaner but more code).

---

## Phase 6 — Replay viewer

**Goal:** a URL pattern (`/replay/<UUID>`) that, when visited, downloads the replay JSON from GCS and renders it in rrweb-player.

### Tasks

1. **Static HTML page** at `sangeet-server/src/main/resources/static/replay.html`:
   - Loads rrweb-player from CDN: `<script src="https://cdn.jsdelivr.net/npm/rrweb-player@1/dist/index.js"></script>`
   - Parses `?id=<UUID>` from URL
   - Fetches `/replay/<UUID>/data` (next step) for the JSON
   - Renders `new rrwebPlayer({ target, props: { events: data.replay } })`

2. **Two new endpoints**:
   - `GET /replay/<UUID>` — serves the HTML player page (or rewrite to `/replay.html?id=...`)
   - `GET /replay/<UUID>/data` — returns the replay JSON from GCS

3. **Auth on the data endpoint**:
   - Only the developer should view replays
   - Simplest: HTTP Basic Auth with a single password stored in Secret Manager as `REPLAY_VIEWER_PASSWORD`
   - The browser will prompt once, cache the credential for the session
   - More secure: GitHub OAuth — overkill for one user

4. **Signed URL alternative**:
   - Instead of streaming through Cloud Run, the data endpoint could issue a 5-min signed GCS URL and redirect
   - Skips the upload/download bandwidth but requires correct CORS on the GCS bucket
   - Either approach works; basic auth + stream is simpler

5. **Replay player UX polish**:
   - Show the user's description above the player
   - Show metadata (browser, viewport, timestamp)
   - Provide a "Mark resolved" button that closes the GitHub issue via the GitHub API (nice-to-have, can be Phase 7)

### Success criteria
- Click the replay link in a GitHub issue → password prompt → enter password → see the replay playing with the user's actions
- Replay UI shows pause, scrub bar, playback speed controls — all from rrweb-player default

### Risks
- **CORS for signed GCS URLs** — if we go the signed URL route, must configure CORS on the bucket. The streaming-through-Cloud-Run path avoids this.
- **Replay JSON over 10 MB streams slowly** — Cloud Run has 32 MB response body limits. Stream the body, don't buffer. Or fall back to signed URL for large replays.

---

## Phase 7 — Polish, dashboards, docs

**Goal:** make the system pleasant to live with.

### Tasks

1. **Dashboards as code** — export Grafana dashboards to JSON, commit to `docs/grafana-dashboards/`. Reproducible setup if we ever need to recreate them.

2. **PostHog dashboards documented** — screenshot the 4 dashboards into `docs/posthog-dashboards.md` so I remember what I built and can rebuild if the PostHog account is recreated.

3. **A small "Report a bug" tip on first visit** — the bug-report button only helps if users notice it. A small one-time tooltip (using localStorage to avoid showing twice): "If something goes wrong, click 🐞 to send me a recording."

4. **Privacy note in About dialog** — short paragraph: "When you interact with the app, we collect anonymous usage data (clicks, keystrokes) via PostHog to understand what features people use. If you click 'Report a bug', the last 5 minutes of your activity are recorded and sent to me along with your message. No password fields are recorded. Data is kept for 90 days."

5. **Cost monitoring** — add a budget alert in GCP for the bug-reports bucket (it shouldn't grow much, but a budget alert at $5 catches surprises early).

6. **Update `docs/hosting-gcp.md`** with the new infra:
   - GCS bucket setup
   - Grafana Cloud integration
   - PostHog setup
   - Secret Manager entries
   - SA role additions

### Success criteria
- Anyone reading `docs/hosting-gcp.md` can recreate the entire stack
- I can show a friend the dashboards and explain what each panel means

---

---

## Phase 8 — Desktop rolling buffer + Report a Bug

**Goal:** sangeet-desktop has a "Report a bug 🐞" entry under the Help menu. Clicking it opens a dialog: textarea + optional email + Send. On Send, the app packages the last 5 minutes of user actions + state snapshots + a screenshot of the current window, and POSTs to the same `/api/v1/bug-reports` endpoint built in Phase 5.

This phase is the desktop equivalent of web Phases 4 + the client-side bits of 5. The actual upload reuses the backend endpoint as-is.

### Tasks

1. **EventLogger module** at `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/diagnostics/EventLogger.scala`:
   - A singleton object with an internal ring buffer (`scala.collection.mutable.Queue[LoggedEvent]`)
   - `LoggedEvent` ADT:
     ```scala
     enum LoggedEvent:
       case Action(timestamp: Long, method: String, args: Json)      // EditorApi call
       case Key(timestamp: Long, code: String, modifiers: Set[String])
       case Mouse(timestamp: Long, kind: String, x: Double, y: Double, target: String)
       case Menu(timestamp: Long, path: String)                      // "File > Save"
       case Snapshot(timestamp: Long, composition: Json)             // periodic Composition state
       case Lifecycle(timestamp: Long, event: String)                // "startup", "tab-opened", etc.
     ```
   - Time-based eviction: keep events from the last 5 minutes; trim on every append
   - Memory cap: hard limit at 5000 events (defensive — sustained key spam shouldn't OOM)

2. **EditorApi wrapping**:
   - Create `sangeet-desktop/.../diagnostics/InstrumentedEditorApi.scala` that wraps the existing `EditorApi`
   - Every public method: `def insertSwar(state, swar): EditorState = { EventLogger.action("insertSwar", Json.obj(...)); delegate.insertSwar(state, swar) }`
   - `MainApp` and `EditorPane` use `InstrumentedEditorApi` instead of `EditorApi` directly
   - This gives **deterministic replay** in dev: the action log is precisely the sequence of EditorApi calls

3. **JavaFX event capture** in `MainApp`:
   - At scene root, install an `EventFilter` for `KeyEvent.KEY_PRESSED` → `EventLogger.key(...)`
   - At scene root, install an `EventFilter` for `MouseEvent.MOUSE_CLICKED` → `EventLogger.mouse(...)` with the target node's `id` or class name
   - This captures gestures that don't always reach `EditorApi` (cancel buttons, menu opens, dialog dismissals)

4. **Periodic composition snapshots**:
   - A `Timeline` running every 30s — `EventLogger.snapshot(currentComposition.asJson)`
   - Also snapshot on tab switch and major state change (these are the discontinuities)

5. **Screenshot at report time**:
   - JavaFX `Scene.snapshot(null)` returns a `WritableImage`
   - Convert to PNG bytes via `SwingFXUtils.fromFXImage(image, null)` → `BufferedImage` → `ImageIO.write(... "png", baos)`
   - Captures the JavaFX window only — not other apps on the user's screen (privacy bonus)

6. **Bug-report dialog** (new file `sangeet-desktop/.../dialog/BugReportDialog.scala`):
   - JavaFX modal with `TextArea` (description, required), `TextField` (email, optional)
   - Disclosure label: "We'll include a 5-min log of your actions, a snapshot of your current composition, and a screenshot of this window. Your file paths and screen contents outside this window are not included."
   - Send button — calls `BugReportUploader.send(...)`

7. **Help menu entry**:
   - In `MainApp` menu bar, add `Help → Report a bug...` (existing About entry stays)
   - Keyboard shortcut: `Ctrl/Cmd+Shift+R` (optional)

8. **Uploader** (`sangeet-desktop/.../diagnostics/BugReportUploader.scala`):
   - Uses JDK 11+ `java.net.http.HttpClient` — no extra dependency
   - Endpoint URL from a constant (or env var override for dev): `https://sangeet-server-729103223940.asia-south1.run.app/api/v1/bug-reports`
   - Body construction via circe — `{ type: "desktop", description, email, actionLog, compositionSnapshots, screenshotPng, metadata }`
   - **Offline fallback**: if the POST fails (network error or non-2xx), prompt the user: "Couldn't send. Save the report to a file so you can email it later?" → write to `~/Downloads/sangeet-bug-report-<UUID>.json` and show the path
   - Metadata block: `{ appVersion: BuildInfo.version, osName: System.getProperty("os.name"), osVersion: System.getProperty("os.version"), javaVersion: System.getProperty("java.version"), displayResolution: "<w>x<h>", localeTag: java.util.Locale.getDefault.toLanguageTag }`

9. **Privacy/consent toggle in Settings**:
   - Default: bug reporting enabled
   - Settings dialog gets a checkbox: "Enable bug reporting (Report a bug menu item, automatic crash capture)"
   - Persists via `AppConfig`
   - If disabled, EventLogger doesn't even buffer (saves the small amount of memory + makes the "no telemetry" claim true)

### Success criteria
- Open the desktop app, edit a composition for 1 minute, Help → Report a bug, type "test", click Send → "Report sent" notification appears
- A GitHub Issue is created within 30 seconds, labeled `platform-desktop`, with description + a link to the report
- Replay the action log in dev: load the snapshot at `actionLog[0].timestamp`, apply each Action through `EditorApi` in order, end state matches the final snapshot

### Risks
- **JavaFX threading**: EditorApi can be called from JavaFX threads. EventLogger must be thread-safe (use `synchronized` on the queue, or a `ConcurrentLinkedDeque` — the volume is low so simple is fine).
- **Composition snapshot size**: a large composition serializes to ~50-200 KB. Ten snapshots × 5 minutes = 500KB-2MB. Acceptable. If it grows, we can switch to deltas instead of full snapshots later.
- **Screenshot may include sensitive composition titles or sahitya text**: that's the user's own data they're showing to me. The dialog discloses this; if a user has a privacy concern, the Settings toggle disables the whole feature. Don't try to mask text in the screenshot — JavaFX has no concept of "secret nodes".

---

## Phase 9 — Auto-crash capture + next-launch recovery

**Goal:** when the desktop app dies from an uncaught exception, the most recent state + action buffer + stack trace are written to disk before the JVM exits. On the next launch, if a crash-pending file is found, the user is prompted to send a report.

### Tasks

1. **Crash sentinel directory** `~/.sangeet/crash-pending/`:
   - Created on first startup (`Files.createDirectories`)
   - One pending crash = one file `<UUID>.json` in this directory
   - On successful sending of a report, the file is deleted

2. **Uncaught exception handler** in `MainApp.main`:
   - `Thread.setDefaultUncaughtExceptionHandler((thread, throwable) => CrashCapture.handle(thread, throwable))`
   - Inside `CrashCapture.handle`:
     - Serialize `{ stackTrace: throwable.toString + stack frames, threadName: thread.getName, eventLogger: EventLogger.snapshot, metadata: same as Phase 8 }`
     - Write to `~/.sangeet/crash-pending/<UUID>.json`
     - Log to stderr (not stdout — JVM may have closed)
     - Do NOT call `System.exit` — let the JVM die naturally so other shutdown hooks (config save, etc.) still run

3. **Additionally hook JavaFX's uncaught handler**:
   - `Thread.currentThread.setUncaughtExceptionHandler(...)` inside `MainApp.start` — the JavaFX Application Thread has its own handler that the default doesn't always cover
   - Both delegate to the same `CrashCapture.handle`

4. **Startup scan** in `MainApp.start`:
   - On launch, check `~/.sangeet/crash-pending/` — if any files exist, show a recovery dialog **before** the main window appears
   - Dialog: "Sangeet didn't shut down cleanly last time. Would you like to send a report so I can fix the underlying issue?"
   - Buttons: **Send** / **View details** / **Discard**
   - **View details**: shows the stack trace + a summary of what was happening (last 5 actions, last opened file path)
   - **Send**: POSTs to `/api/v1/bug-reports` with `type: "desktop"` and an additional `crashTrigger: true` flag (so backend can label as `platform-desktop` + `crash`)
   - **Discard**: deletes the file
   - On Send success: also deletes the file
   - On Send failure (offline): offers the same "Save to Downloads" fallback as user-initiated reports

5. **OOM and JVM hard-crash caveat**:
   - Uncaught exceptions are caught. JVM crashes (OOM, native-library segfault) may not be — the handler can't run if the JVM is too broken
   - Mitigation: also write a periodic "I'm alive" heartbeat to `~/.sangeet/last-alive.txt` every 60s. On startup, if the file's mtime is old but there's no clean-shutdown marker, infer a hard crash and prompt anyway (with reduced info — just the last alive-time and last opened file)
   - This is a stretch goal — start without it; add if hard crashes turn out to be a real problem

6. **Clean shutdown marker**:
   - On app close (`stage.onCloseRequest`): write a tiny `~/.sangeet/clean-shutdown.marker` file
   - Startup deletes it; if it doesn't exist at startup time AND there's no crash-pending file, no recovery dialog
   - Lets us distinguish "clean exit" from "crash" reliably

### Success criteria
- Manually inject an exception (`throw new RuntimeException("test")` in a button handler) → app dies → relaunch → recovery dialog appears with the stack trace and event log → Send → GitHub Issue created with `crash` label
- Discard option works without sending anything
- Normal shutdown (close window) does NOT show the recovery dialog on next launch

### Risks
- **The crash handler itself might throw** (e.g., disk full when writing the crash file). Wrap in try/catch; the secondary failure is silent. We tried.
- **Recovery dialog timing**: must show BEFORE main window appears. JavaFX `Application.start` blocks the splash screen if we're not careful. Use a separate `Stage` for the recovery dialog with `initStyle(StageStyle.Utility)`, show modally, then proceed to normal startup.
- **Sensitive stack traces**: stack traces include class names but not user data. Safe.

---

## Phase 10 — Desktop usage metrics with PostHog-Java

**Goal:** the desktop app records anonymous feature-usage events to a *separate* PostHog project ("Sangeet Desktop"). I get a parallel set of dashboards to my web ones.

### Tasks

1. **Create a new PostHog project**:
   - In the same PostHog account from Phase 3, click "Create new project" — name it **"Sangeet Desktop"**
   - Get its own API key (also `phc_...` prefix — different from the web project's key)
   - **This is the separation guarantee** — events from this key never appear in the Web project's dashboards, and vice-versa

2. **Add the SDK dependency** to `build.sbt` (`sangeetDesktop` block):
   - `"com.posthog" % "posthog-java" % "1.0.0"` (check latest at posthog.com/docs/libraries/java)
   - Small (~150 KB), no transitive bloat
   - Bundled into the assembly JAR; included in the desktop installers via the existing packaging flow

3. **PostHogClient singleton** at `sangeet-desktop/.../diagnostics/PostHogClient.scala`:
   - Initialize on app startup with the API key (compile-time constant or env var)
   - Generate an anonymous distinct-id once per install: random UUID stored in `AppConfig` (`distinctId`); persists across runs so we can track "returning users" without knowing who they are
   - `def capture(event: String, props: Map[String, Any]): Unit` — fire and forget; SDK batches automatically

4. **Instrument the high-value events**:
   - **App lifecycle**: `app_started` (with appVersion, osName, javaVersion), `app_closed` (with sessionDurationMs)
   - **Composition flow**: `composition_created` (compositionType: gat/bandish/palta), `composition_saved` (sectionCount, eventCount), `composition_opened` (source: file-browser/menu/recent)
   - **Editor**: `editor_action` (action: insert-swar/delete/undo/redo/cut/copy/paste, perActionFrequencyTopK) — sampled at 1/10 to avoid flooding
   - **Menus + shortcuts**: `menu_action` (path: "File > Save"), `keyboard_shortcut` (shortcut: "ctrl-s")
   - **Dialogs**: `dialog_opened` (name: "new-composition" / "properties" / "about" / "support" / etc.)
   - **Crashes**: `crash_detected` (after Phase 9's recovery flow, when a report is sent)
   - **Bug reports**: `bug_report_sent` (description-length only, no description content)

5. **Wire the instrumentation**:
   - `MainApp` startup/shutdown
   - `EditorPane` editor actions
   - `MenuBar` menu items (one shared listener)
   - `KeyboardLegend` / key-handlers (one shared listener)
   - Each dialog's `show()` entry

6. **Privacy/consent in Settings**:
   - Same toggle from Phase 8 ("Enable bug reporting") gets a second sibling: "Send anonymous usage statistics to help improve the app"
   - Default: enabled (since the data is anonymous and the app is free)
   - If disabled, `PostHogClient.capture` becomes a no-op
   - First-launch one-time notice in the welcome flow: "Sangeet sends anonymous usage stats to help me understand which features are useful. You can turn this off in Settings → Privacy."

7. **PostHog dashboards for "Sangeet Desktop"** — 4 to mirror the web ones:
   - **Daily active installations** (unique distinctIds per day)
   - **Feature usage** (composition_created split by type, editor_action by action, menu_action by path)
   - **Crash rate** (crash_detected events per app_started — proxy for stability)
   - **Session length** (histogram of sessionDurationMs)

### Success criteria
- Launch the app, do some stuff, close it → events visible in PostHog "Sangeet Desktop" project within 60s (after the SDK's batch flush)
- "Sangeet Web" project shows ZERO desktop events — separation verified
- 4 dashboards render with real data after a day of dogfooding

### Risks
- **PostHog-Java is less mature than posthog-js**: API surface smaller, no auto-instrumentation. We're instrumenting everything manually anyway, so this is fine.
- **Network failures during capture** are handled by the SDK (background queue with retry); no user-facing impact.
- **First-launch consent flow**: making it obtrusive will annoy users; making it invisible may violate trust. Plan is opt-out (enabled by default) with a small one-line notice on first launch. Reconsider if any user objects.

---

## Open decisions (defer to execution)

These don't block the plan but need a call when implementing the relevant phase:

| Decision | Default if not revisited | When to decide |
|---|---|---|
| **PII masking strategy** for inputs that hold user text (composition title, sahitya lyrics) | Don't mask. Sangeet is creative content; user typed it knowingly. | Phase 4 (rrweb config) |
| **Replay retention** | 90 days via GCS lifecycle policy | Phase 5 |
| **Bug-report rate limit** | 5/hour/IP | Phase 5 |
| **Replay viewer auth** | HTTP Basic Auth with password in Secret Manager | Phase 6 |
| **Whether to also keep rrweb data when user closes the tab without reporting** | No — buffer dies with the tab (privacy default) | Phase 4 |
| **Streaming vs signed URL for replay viewer** | Stream through Cloud Run (simpler) | Phase 6 |
| **Desktop: include the heartbeat / hard-crash fallback** (lets us infer JVM hard crashes that bypass the exception handler) | Skip in Phase 9; add later if real hard crashes show up | Phase 9 (revisit after some prod data) |
| **Desktop: snapshot granularity in EventLogger** — full Composition every 30s vs Composition delta | Full snapshots (simpler; size is fine at expected scale) | Phase 8 |
| **Desktop: anonymous distinctId persistence** — random UUID in AppConfig, persists forever vs rotates every 90 days | Persist forever; if a user wants reset, they can clear AppConfig | Phase 10 |

---

## Cost projection

At expected scale (low traffic personal project, <100 active users/month across web + desktop, <50 bug reports/year, <20 crashes/year):

| Component | Free tier | Expected use | Cost |
|---|---|---|---|
| Cloud Monitoring (metric storage) | 150 MiB ingested/month free, 6 wk–24 mo retention | ~50 MiB/month | **$0** |
| Grafana Cloud Free (viewer only, reads from Cloud Monitoring) | 10K dashboard panels effectively unlimited at our scale | 5 dashboards | **$0** |
| PostHog Cloud project "Sangeet Web" | 1M events/month | ~10K events/month | **$0** |
| PostHog Cloud project "Sangeet Desktop" (same account) | 1M events/month (per-project) | ~5K events/month | **$0** |
| GCS bucket for replays + desktop reports | First 5GB free | ~70 reports (50 web + 20 desktop) × 1MB avg = 70MB | **$0** |
| Cloud Run egress for uploads | 1GB/month free (asia-south1 → world) | Negligible | **$0** |
| GitHub Issues API | 5000 req/hour | ~70 issues/year total | **$0** |
| Secret Manager | 6 secret versions free | 2 secrets (GitHub PAT, replay viewer password) | **$0** |
| GCP egress overall | 1GB free | Within | **$0** |
| **Total** | | | **$0/month at current scale** |

PostHog projects share the parent account's billing tier — the free 1M/month is *per-project*, so we genuinely get 2M events/month free across web + desktop without any account upgrade. (Verify when signing up: PostHog's pricing page says "per project" but always re-read at signup.)

The point of failure for the free-tier story is if a user gets stuck in a loop spamming the bug-report button — hence the rate limit in Phase 5.

---

## Implementation order

**Recommended sequential order**, each phase shippable as its own PR:

1. **Phase 1** — Backend metrics infrastructure (Micrometer + Cloud Monitoring; foundation; immediate visibility into Cloud Run)
2. **Phase 2** — Custom backend metrics (builds on 1; lots of user-facing insight)
3. **Phase 3** — Web frontend metrics with PostHog "Sangeet Web" project (independent; can interleave with backend if blocked)
4. **Phase 4** — rrweb client-side recording on web (no backend changes yet; can demo the buffer locally without sending anywhere)
5. **Phase 5** — Backend bug-report endpoint + GitHub Issue creation (serves both web and desktop)
6. **Phase 6** — Replay viewer (web replays only; desktop reports get viewed in your IDE)
7. **Phase 7** — Polish, docs, privacy notes for the web stack
8. **Phase 8** — Desktop rolling buffer + Report a Bug menu item (depends on Phase 5 being live)
9. **Phase 9** — Desktop auto-crash capture + next-launch recovery (builds on 8; reuses the uploader)
10. **Phase 10** — Desktop usage metrics with PostHog-Java "Sangeet Desktop" project (independent of 8/9; can interleave)

Phases 1-2 give immediate value (backend visibility) and are low-risk.
Phases 4-6 deliver web session replay end-to-end.
Phase 8 is the most-asked-for desktop feature.
Phase 9 catches the bugs the user can't manually report.
Phase 10 closes the loop on per-platform feature insight.

Rough effort estimate (small-PR sized chunks):
- Phases 1-2: 1 evening each
- Phase 3: 1 evening
- Phase 4: 1-2 evenings
- Phase 5: 2 evenings (auth + GitHub API + GCS)
- Phase 6: 1 evening
- Phase 7: 1 evening
- Phase 8: 2 evenings (EventLogger wrapper + dialog + uploader + offline fallback)
- Phase 9: 1-2 evenings (handler + sentinel files + recovery dialog)
- Phase 10: 1 evening

**Total: ~13-15 evenings.** Can be spread out — each phase is independently valuable. The web stack (Phases 1-7) and desktop stack (Phases 8-10) can also be developed in parallel after Phase 5 is live, if you want to alternate.

---

## What I'd want to revisit later (out of scope for this plan)

- **Web error tracking** (Sentry-style): currently the only signal for unhandled JS exceptions in the browser is the user filing a bug report. Adding Sentry frontend (free tier) would catch errors the user doesn't bother reporting. Could be a Plan 13.
- **Alerting**: not asked for. If the app gets serious users, alert on 5xx rate, p99 latency, replay-bug-report rate spikes, desktop crash-rate spikes.
- **Cost optimization for Cloud Run**: if metrics show the JVM startup time dominates request latency for cold starts, consider min-instances=1 (~$10/mo) or migrating to native-image with GraalVM.
- **User feedback loop**: when a bug is fixed, optionally email the user (if they provided one) saying "the issue you reported is fixed in the latest version (release X.Y.Z)" — closes the loop, builds trust. Needs email integration.
- **Desktop auto-update with version check**: PostHog data will show stale-version distribution; could add a "new version available" banner if too many users are on old releases.
- **Desktop bug-report dedup**: if 10 users hit the same crash, we get 10 GitHub Issues. Could group by stack-trace signature.
