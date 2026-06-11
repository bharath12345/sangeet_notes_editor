# Observability & Bug Reporting — Implementation Tracker

Companion to [`docs/plans/plan-12-observability-and-replay.md`](plans/plan-12-observability-and-replay.md). The plan is the design; this is the **living record** of what's actually deployed, what configuration exists in external services, what's still pending, and gotchas hit along the way. Updated after every meaningful change.

**Last updated:** 2026-06-11

---

## Quick status

| Phase | Status | Notes |
|---|---|---|
| 1. Backend metrics infrastructure (Micrometer → Cloud Monitoring) | 🟡 deployed, push not yet working | Descriptors registered; CreateTimeSeries calls not happening or silently failing. Debug PR in flight. |
| 2. Custom backend metrics (per-path/method/category) | ⬜ not started | Waits on Phase 1 being healthy. |
| 3. Web frontend metrics (PostHog "Sangeet Web") | ⬜ not started | |
| 4. Web session replay (rrweb rolling buffer + Report Bug) | ⬜ not started | |
| 5. Backend bug-report endpoint + GitHub Issue auto-create | ⬜ not started | Shared by web (Phase 4) and desktop (Phase 8). |
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

### Future GCP resources (planned, not yet created)

| Resource | Phase | Purpose |
|---|---|---|
| GCS bucket `sangeet-bug-reports` (asia-south1) | 5 | Stores rrweb replays + desktop bug-report JSON blobs |
| Bucket lifecycle policy (delete > 90d) | 5 | Auto-prune old reports |
| Cloud Run env var `GITHUB_TOKEN` (from Secret Manager) | 5 | Bug-report endpoint uses to file Issues |
| Secret Manager secret `github-issues-token` | 5 | Fine-grained PAT, scope: issues:write on bharath12345/sangeet_notes_editor |
| Secret Manager secret `replay-viewer-password` | 6 | HTTP Basic Auth password for the replay viewer |

### Non-GCP services

| Service | Plan / cost | Status |
|---|---|---|
| GitHub Pages | Free; hosts the Elm frontend | 🟢 live since Plan 11 |
| Grafana Cloud Free (viewer only, reading from Cloud Monitoring) | Free forever; dashboards only — no metric data stored there | ⬜ deferred until Phase 1 is healthy; signup happens later |
| PostHog Cloud project "Sangeet Web" | Free 1M events/month | ⬜ not created yet (Phase 3) |
| PostHog Cloud project "Sangeet Desktop" | Free 1M events/month (separate project from Web for clean separation per decision #8) | ⬜ not created yet (Phase 10) |

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
- ✅ Cloud Run deploy succeeds (revision `sangeet-server-00008-j6z` is live as of 2026-06-11 05:38 UTC)
- ✅ Live `https://sangeet-server-729103223940.asia-south1.run.app/metrics` returns 200
- ✅ Container starts cleanly: log says "Cloud Monitoring push: enabled (pushing to project sangeet-editor every 60s)"
- ✅ MetricDescriptor calls reach Cloud Monitoring: ~20 `custom.googleapis.com/jvm/*` metric descriptors registered (verified via Monitoring API)

### What's NOT working (the bug)
- ❌ `CreateTimeSeries` calls aren't happening (or are failing silently) — zero time series over the last 2 hours across all jvm metrics
- ❌ Cloud Monitoring Metrics Explorer shows the metric names in the picker but no data points to chart

### Root-cause hypothesis (being investigated)
The application uses `slf4j-api` transitively (via Tapir / cats / Micrometer) but has no SLF4J binding implementation. Result: every `LOG.error(...)` and `LOG.warn(...)` in the Micrometer Stackdriver registry goes to a NO-OP logger. Any push failure (auth issue, API rejection, time-interval validation error) is silently swallowed and we see nothing.

### Fix in progress
Branch `debug/slf4j-impl-for-stackdriver`:
- Add `org.slf4j:slf4j-simple:2.0.13` as a runtime dep
- slf4j-simple writes to stderr at INFO level by default — no config required
- After deploy, Cloud Run logs should show what the Stackdriver registry is actually doing during push attempts
- Once we see the real error, the actual fix becomes obvious

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

---

## Gotchas + lessons learned

### `micrometer-registry-stackdriver` brings in signed jars
Google Cloud Java client libraries are signed. Their `META-INF/SIGNINGC.{SF,RSA}` files end up in the fat jar. Once sbt-assembly merges classes from other jars, the signature no longer matches the contents, and the JVM refuses to load the jar at all with the misleading error `Could not find or load main class com.varpas.sangeet.server.Main`. **Fix:** discard `*.SF`, `*.RSA`, `*.DSA`, `*.EC` in `META-INF/` in the assembly merge strategy. Resolved in PR #26.

### Cloud Run CPU throttling kills background push threads
By default Cloud Run only allocates CPU during request handling. With `min-instances=0`, the instance stays alive ~5 minutes after the last request but CPU is throttled during the idle. Micrometer's push thread can't run, so metric pushes silently get skipped. **Fix:** `gcloud run services update sangeet-server --no-cpu-throttling`. Costs are unchanged when the instance scales to zero; only incurs while warm.

### Grafana Cloud Free's 14-day "trial" is a UX confusion
What looks like a 14-day Pro trial in the signup flow is exactly that — after 14 days the account auto-downgrades to the actual Free tier (which is permanent). What ALSO exists, separately, is the Free tier's 14-day data retention — metrics ingested into Grafana Cloud's Prometheus get aged out after 2 weeks. We sidestep both by using Cloud Monitoring as the storage layer and Grafana Cloud (later) only as a viewer reading from Cloud Monitoring as a data source.

### SLF4J no-op binding silently eats library error logs (suspected — under investigation)
Without an SLF4J impl on the classpath, any logger inside Micrometer / Stackdriver / google-cloud-monitoring SDKs throws away its output. Push failures end up invisible. Plan: add `slf4j-simple` to make these logs reach Cloud Run's stdout.

---

## Cost actuals

Tracked monthly. Goal: stay $0.

| Date | Item | Charge | Source |
|---|---|---|---|
| 2026-06 | (none yet — Phase 1 deployed mid-month, nothing else live) | $0 | n/a |

---

## Document update conventions

- Bump the "Last updated" date at the top whenever this file is edited
- Mark phases with their current status in the table above
- Add new `## Phase N — detailed status` sections as each phase enters in-progress
- Move resolved gotchas into the "Gotchas + lessons learned" section so future-me doesn't repeat them
- Reference PR numbers when describing fixes so the git log is one click away
