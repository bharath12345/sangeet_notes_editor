# Grafana dashboards

Source-of-truth JSON for Grafana Cloud dashboards reading from GCP Cloud Monitoring. Plan 12 phase 7 was deferred at the time; this directory closes that gap.

## Files

- `sangeet-server-health.json` — JVM + Tapir HTTP metrics for the Cloud-Run-hosted sangeet-server. 8 panels: service health row (4 stats) + HTTP traffic row + JVM internals row.

## One-time setup

### 1. GCP service account (already done)

A read-only service account exists at `grafana-mon-reader@sangeet-editor.iam.gserviceaccount.com` with `roles/monitoring.viewer`. To rotate or recreate:

```bash
gcloud iam service-accounts create grafana-mon-reader \
  --display-name="Grafana Cloud Monitoring reader" \
  --description="Read-only access to Cloud Monitoring metrics for Grafana"

gcloud projects add-iam-policy-binding sangeet-editor \
  --member="serviceAccount:grafana-mon-reader@sangeet-editor.iam.gserviceaccount.com" \
  --role="roles/monitoring.viewer" --condition=None

gcloud iam service-accounts keys create ~/.sangeet-grafana-sa-key.json \
  --iam-account=grafana-mon-reader@sangeet-editor.iam.gserviceaccount.com
```

The JSON key file is **not** committed; it lives at `~/.sangeet-grafana-sa-key.json` locally and is pasted into Grafana once at data-source creation time.

### 2. Grafana data source (one-time, in Grafana UI)

- Grafana sidebar → **Connections → Add new connection**
- Search **"Google Cloud Monitoring"** → **Add new data source**
- **Name:** `Cloud Monitoring (Sangeet)`
- **Authentication type:** **Google JWT File**
- Drag-drop `~/.sangeet-grafana-sa-key.json` (or paste contents)
- **Save & test** — should report "1 project found"

## Importing a dashboard

- Grafana sidebar → **Dashboards** → top-right **New → Import**
- Either upload the `.json` file or paste its contents into the **"Import via dashboard JSON model"** textbox → **Load**
- Pick **Cloud Monitoring (Sangeet)** in the data-source dropdown → **Import**

**Watch out:** Don't paste into Dashboard Settings → JSON Model. That editor uses Grafana 11's stricter v2 schema and will reject these v1-format files with errors about missing `cursorSync` / `elements` / `layout` / `timeSettings` fields. The Import flow accepts v1 unchanged.

## Editing the JSON

After tweaking a dashboard in the Grafana UI, export it back via **Share → Export → Save to file → "Export for sharing externally" (toggle ON)** — that produces a JSON with `__inputs` so the next import knows to prompt for the data source. Drop the exported file into this directory, overwriting.

## Gotchas

### Counter metrics are pushed as GAUGE, not CUMULATIVE

Micrometer's Stackdriver registry pushes counters (like `tapir.request.total`) as `GAUGE` type, not `CUMULATIVE`. Cloud Monitoring's `ALIGN_RATE` aligner only works on `CUMULATIVE` / `DELTA` types — on a `GAUGE` it silently returns no data, which shows up as "No data" in the panel.

**Use `ALIGN_DELTA` instead** for rate-style queries on these counters. With a 60s alignment period, the value becomes "events in each 60-second window" — i.e., requests per minute. That's what the HTTP traffic panels in `sangeet-server-health.json` use.

If you ever change a panel and find rate queries returning empty, check the metric kind:

```bash
curl -s -H "Authorization: Bearer $(gcloud auth print-access-token)" \
  "https://monitoring.googleapis.com/v3/projects/sangeet-editor/metricDescriptors/custom.googleapis.com/tapir/request/total" \
  | jq '{metricKind, valueType}'
```

GAUGE → use ALIGN_DELTA. CUMULATIVE / DELTA → use ALIGN_RATE.
