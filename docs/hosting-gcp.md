# Hosting Sangeet Notes Editor on GCP

End-to-end reference for deploying the web backend to **Google Cloud Run** and (eventually) the frontend to a static host. Every command in this guide has been run in production — when something failed, the fix is recorded inline so you don't have to debug it again.

---

## Current deployment state

| Component | Status | URL / identifier |
|---|---|---|
| GCP project | live | `sangeet-editor` |
| Region | live | `asia-south1` (Mumbai) |
| Artifact Registry repo | live | `asia-south1-docker.pkg.dev/sangeet-editor/sangeet` |
| Container image | live | `asia-south1-docker.pkg.dev/sangeet-editor/sangeet/server:latest` |
| Cloud Run service | live | `https://sangeet-server-729103223940.asia-south1.run.app` |
| Swagger UI | live | `https://sangeet-server-729103223940.asia-south1.run.app/docs/` |
| Root redirect | live | `/` → 302 → `/docs/` |
| Frontend hosting | live | GitHub Pages (`.github/workflows/deploy-pages.yml`) |
| Custom domain | pending | (`sangeet-editor.in`) |
| CI/CD: frontend auto-deploy | live | runs on push to `main` touching `sangeet-web/**` |
| CI/CD: backend auto-deploy | live | runs on push to `main` touching `sangeet-server/**`, `sangeet-core/**`, `build.sbt`, `project/**`, `Dockerfile` |

---

## Architecture

```
sangeet-editor.in  (future, GoDaddy domain)
├── Frontend: static SPA (Elm 0.19)
│   └── public/index.html + elm.js + styles.css  → free static host
└── API: Cloud Run (Scala 3 / Tapir / http4s)
    └── api.sangeet-editor.in  →  port 28080 inside container
        Container image: eclipse-temurin:17-jre + sangeet-server-assembly.jar
```

The web app stores compositions in the browser. The API is **stateless** — it serves layout / editor / reference data, never persists anything. This is why Cloud Run's scale-to-zero works fine: no warm state to lose.

---

## Estimated cost

| Component | Free tier | Typical monthly cost |
|---|---|---|
| Cloud Run (scale-to-zero, low traffic) | 2 M req/month, 360 K GB-s memory, 180 K vCPU-s | **$0** |
| Cloud Run (one warm instance, no scale-to-zero) | — | ~$10–15 |
| Artifact Registry | 0.5 GB storage free | ~$0.10/GB beyond |
| Cloud Build | 120 build-min/day | $0 at our usage |
| Egress (Mumbai → world) | 1 GB/month free | ~$0.12/GB beyond |
| **Total expected for personal use** | | **$0** |

The free tier is real and generous, but it's per-region — don't accidentally deploy across multiple regions.

---

## Prerequisites

- A Google account (we used `bharath12345@gmail.com`)
- Billing enabled on the GCP project (required even within free tier)
- `gcloud` CLI installed and authenticated

Install gcloud on macOS:
```bash
brew install --cask google-cloud-sdk
```

Authenticate (interactive, one-time):
```bash
gcloud auth login
```

---

## Phase 1 — Project + APIs (one-time)

```bash
# Pin the active project
gcloud config set project sangeet-editor

# Enable the three required APIs
gcloud services enable run.googleapis.com \
                       cloudbuild.googleapis.com \
                       artifactregistry.googleapis.com

# Verify
gcloud services list --enabled \
  --filter="name:(run.googleapis.com OR cloudbuild.googleapis.com OR artifactregistry.googleapis.com)"
```

You may see this warning after `set project`:
```
WARNING: Your active project does not match the quota project in your local Application Default Credentials file.
```
That only affects programmatic SDK usage (not the gcloud CLI itself). Fix it later if/when you write client code:
```bash
gcloud auth application-default set-quota-project sangeet-editor
```

---

## Phase 2 — Artifact Registry repo (one-time)

```bash
gcloud artifacts repositories create sangeet \
    --repository-format=docker \
    --location=asia-south1 \
    --description="Sangeet Notes Editor container images"
```

This creates a Docker registry at:
```
asia-south1-docker.pkg.dev/sangeet-editor/sangeet
```

The free tier is 0.5 GB. Each image is ~250 MB (Temurin JRE base + 40 MB JAR), so we can keep ~2 images comfortably. Use `gcloud artifacts docker images delete` to prune old versions when storage grows.

> **Why Artifact Registry, not Container Registry (gcr.io)?**
> GCR is deprecated; Google is sunsetting it. New projects must use Artifact Registry. The path format is `<region>-docker.pkg.dev/<project>/<repo>/<image>:<tag>`.

---

## Phase 3 — Build the assembly JAR

```bash
sbt sangeetServer/assembly
```

Output: `sangeet-server/target/scala-3.4.2/sangeet-server-assembly-0.2.0.jar` (~40 MB).

> **Critical: `build.sbt` has an explicit assembly merge strategy for `sangeetServer`.**
> sbt-assembly's default strategy discards `META-INF/maven/**`, which crashes Tapir SwaggerUI at startup (it reads `META-INF/maven/org.webjars/swagger-ui/pom.properties` to detect the bundled webjar version). The merge strategy in `build.sbt` whitelists that path:
> ```scala
> assembly / assemblyMergeStrategy := {
>   case PathList("META-INF", "maven", "org.webjars", "swagger-ui", _*) => MergeStrategy.singleOrError
>   case PathList("META-INF", "MANIFEST.MF")                            => MergeStrategy.discard
>   case PathList("META-INF", "services", _*)                           => MergeStrategy.concat
>   case PathList("META-INF", "versions", _*)                           => MergeStrategy.first
>   case x if x.endsWith("module-info.class")                           => MergeStrategy.discard
>   case _                                                              => MergeStrategy.first
> },
> ```
> Symptom if you remove this: `ExceptionInInitializerError: META-INF resources are missing` on first request to `/docs/`. Server appears to start (port binds, `Sangeet Server starting...` prints), but every Swagger request 500s.

Verify the swagger-ui resources are inside the jar:
```bash
unzip -l sangeet-server/target/scala-3.4.2/sangeet-server-assembly-0.2.0.jar \
  | grep 'swagger-ui/pom.properties'
```
Should print one line (`META-INF/maven/org.webjars/swagger-ui/pom.properties`).

---

## Phase 4 — Build and push the Docker image

### Step 4a — Stage a minimal build context

This is the **single most important non-obvious step**. Skipping it will cause the Cloud Build to fail with `COPY failed: file not found`.

> **Why staging is required:**
> `gcloud builds submit` uploads the current directory to Cloud Build. If a `.gcloudignore` file exists it's used; otherwise gcloud auto-generates one from `.gitignore`. Our `.gitignore` excludes `target/` and `*.jar` — exactly the files the Dockerfile needs. The Docker build then fails because the assembly JAR isn't in the upload.
>
> Alternative: write a `.gcloudignore` that re-includes the JAR. The negation rules are fiddly. Staging a clean directory is faster and self-documenting.

```bash
# Wipe + recreate staging dir
rm -rf /tmp/sangeet-build
mkdir -p /tmp/sangeet-build/sangeet-server/target/scala-3.4.2

# Copy only what the Dockerfile references
cp Dockerfile /tmp/sangeet-build/
cp sangeet-server/target/scala-3.4.2/sangeet-server-assembly-*.jar \
   /tmp/sangeet-build/sangeet-server/target/scala-3.4.2/
```

### Step 4b — Submit the build

```bash
gcloud builds submit /tmp/sangeet-build \
    --tag asia-south1-docker.pkg.dev/sangeet-editor/sangeet/server:latest \
    --region asia-south1
```

Cloud Build:
1. Tars the staging dir (~40 MB) and uploads to a temp GCS bucket.
2. Runs `docker build` on a Cloud Build worker.
3. Pushes the resulting image to Artifact Registry.

Typical duration: **30–60 seconds**.

> **About the Dockerfile base image:**
> Use `eclipse-temurin:17-jre` (multi-arch). **Don't use** `eclipse-temurin:17-jre-alpine` — it's amd64-only and Docker build will fail on Apple Silicon and any arm64 host. Cloud Build itself runs amd64, but local `docker build` for testing will break.

---

## Phase 5 — Deploy to Cloud Run

```bash
gcloud run deploy sangeet-server \
    --image asia-south1-docker.pkg.dev/sangeet-editor/sangeet/server:latest \
    --region asia-south1 \
    --platform managed \
    --allow-unauthenticated \
    --memory 512Mi \
    --cpu 1 \
    --port 28080 \
    --min-instances 0 \
    --max-instances 2 \
    --timeout 60
```

### Flag rationale

| Flag | Value | Why |
|---|---|---|
| `--allow-unauthenticated` | yes | Public web app — no auth headers from browser. Tighten later if you add auth. |
| `--memory` | `512Mi` | Matches JVM `-Xmx400m` from Dockerfile with headroom for metaspace + native. |
| `--cpu` | `1` | Default; one vCPU is plenty for the workload. |
| `--port` | `28080` | Matches `EXPOSE 28080` in Dockerfile and the server's bind port. |
| `--min-instances` | `0` | Scale-to-zero when idle. **This is the lever that keeps us in free tier.** Cold start is ~1–2 s. |
| `--max-instances` | `2` | Guardrail against runaway cost from a traffic spike. Free tier covers 2 M req/month anyway. |
| `--timeout` | `60` | Per-request timeout. Default is 300 s — no endpoint of ours needs that. |

Deploy takes ~30–60 seconds. Output ends with:
```
Service URL: https://sangeet-server-729103223940.asia-south1.run.app
```

That URL is permanent for the service (only changes if you delete and recreate the service or change project).

---

## Phase 6 — Verify

```bash
URL="https://sangeet-server-729103223940.asia-south1.run.app"

curl -s -o /dev/null -w "/health         HTTP %{http_code}\n" "$URL/health"
curl -s -o /dev/null -w "/docs/          HTTP %{http_code}\n" "$URL/docs/"
curl -s -o /dev/null -w "/docs/docs.yaml HTTP %{http_code}\n" "$URL/docs/docs.yaml"
curl -s -o /dev/null -w "/api/v1/raags   HTTP %{http_code}\n" "$URL/api/v1/raags"
curl -s -o /dev/null -w "/api/v1/taals   HTTP %{http_code}\n" "$URL/api/v1/taals"
```

All five should be `200`. If `/docs/` returns 500 or you see a class-init error in logs, see Troubleshooting → "Swagger UI 500".

---

## Subsequent deploys (the quick path)

After the one-time setup above, redeploying a code change is:

```bash
# 1. Build the JAR
sbt sangeetServer/assembly

# 2. Stage and submit
rm -rf /tmp/sangeet-build
mkdir -p /tmp/sangeet-build/sangeet-server/target/scala-3.4.2
cp Dockerfile /tmp/sangeet-build/
cp sangeet-server/target/scala-3.4.2/sangeet-server-assembly-*.jar \
   /tmp/sangeet-build/sangeet-server/target/scala-3.4.2/

gcloud builds submit /tmp/sangeet-build \
    --tag asia-south1-docker.pkg.dev/sangeet-editor/sangeet/server:latest \
    --region asia-south1

# 3. Deploy the new image (Cloud Run creates a new revision and shifts traffic)
gcloud run deploy sangeet-server \
    --image asia-south1-docker.pkg.dev/sangeet-editor/sangeet/server:latest \
    --region asia-south1
```

The `:latest` tag means each deploy overwrites. To support rollbacks, use a version tag instead:
```bash
TAG=$(git rev-parse --short HEAD)
gcloud builds submit /tmp/sangeet-build --tag "...sangeet/server:${TAG}" --region asia-south1
gcloud run deploy sangeet-server --image "...sangeet/server:${TAG}" --region asia-south1
```

To roll back: deploy a previous tag the same way, or via console: Cloud Run → service → Revisions → "Manage Traffic" → 100% to older revision.

---

## Phase 7 — Static frontend hosting (GitHub Pages)

The Elm app (`sangeet-web/public/`) is a static SPA served from GitHub Pages. The workflow at `.github/workflows/deploy-pages.yml` rebuilds and republishes on every push to `main` that touches `sangeet-web/**`.

**Site URL:** `https://bharath12345.github.io/sangeet_notes_editor/`

### How the API base URL is chosen

`sangeet-web/public/index.html` picks the API URL by hostname:
```js
function getApiBaseUrl() {
  var h = window.location.hostname;
  if (h === 'localhost' || h === '127.0.0.1' || h === '') {
    return 'http://localhost:28080/api/v1';
  }
  return 'https://sangeet-server-729103223940.asia-south1.run.app/api/v1';
}
```

Same bundle works in dev (localhost backend) and prod (Cloud Run). When the custom domain `api.sangeet-editor.in` goes live, update this one string.

### One-time GitHub setup

In the repo on github.com:
1. **Settings → Pages → Source → "GitHub Actions"**. Without this, the workflow can run but won't publish.
2. The first deploy creates the `github-pages` environment automatically.

### Manual deploy (if needed)

```bash
cd sangeet-web
./node_modules/.bin/elm make src/Main.elm --optimize --output=public/elm.js
# Then push to main — the workflow handles the rest.
```

To force a deploy without code changes: Actions tab → "Deploy Frontend (GitHub Pages)" → Run workflow.

### CORS

The Elm app calls Cloud Run from a different origin. `sangeet-server/src/main/scala/com/varpas/sangeet/server/CorsMiddleware.scala` sets `Access-Control-Allow-Origin: *`, which is fine for a public read-only API. If you ever lock CORS down to a specific origin, update it to the Pages URL (or the custom domain once mapped).

---

## Phase 8 (pending) — Custom domain

When `sangeet-editor.in` (GoDaddy) is wired up:

### Frontend domain
Depends on chosen host — Firebase / Cloudflare / Netlify all have a custom-domain flow that issues A/AAAA records you paste into GoDaddy DNS.

### API subdomain (`api.sangeet-editor.in`)
```bash
gcloud run domain-mappings create \
  --service sangeet-server \
  --domain api.sangeet-editor.in \
  --region asia-south1
```
GCP returns CNAME/A records to add to GoDaddy DNS. Wait 5–60 min for propagation, then:
```bash
curl https://api.sangeet-editor.in/health
```
Should return 200. Let's Encrypt SSL is auto-provisioned within 24 h.

---

## Phase 9 — CI/CD backend auto-deploy

The workflow at `.github/workflows/deploy-backend.yml` rebuilds and redeploys `sangeet-server` to Cloud Run on every push to `main` that touches the backend source, build files, or Dockerfile. End-to-end: ~5 min per deploy.

### How it works
1. Checkout, set up JDK 17 + sbt
2. `sbt sangeetServer/assembly` → produces the fat JAR
3. Authenticate to GCP via the `GCP_SA_KEY` GitHub secret (JSON key for the `github-deployer` service account)
4. Stage Dockerfile + JAR into `/tmp/sangeet-build/` (same gitignore workaround as the manual path — see Phase 4)
5. `gcloud builds submit` with the commit SHA as the image tag
6. `gcloud artifacts docker tags add` to also tag it `:latest`
7. `gcloud run deploy` with the SHA-tagged image
8. Smoke-test `/health`, `/docs/`, `/api/v1/raags` — fails the workflow if any aren't 200

Tagging by SHA means rollback is just deploying an older SHA:
```bash
gcloud run deploy sangeet-server \
  --image asia-south1-docker.pkg.dev/sangeet-editor/sangeet/server:<OLD_SHA> \
  --region asia-south1
```

### Service account setup (one-time, already done)

```bash
SA_EMAIL="github-deployer@sangeet-editor.iam.gserviceaccount.com"

gcloud iam service-accounts create github-deployer \
    --display-name="GitHub Actions deployer" \
    --description="Used by .github/workflows/deploy-backend.yml"

for ROLE in \
    roles/run.admin \
    roles/cloudbuild.builds.editor \
    roles/artifactregistry.writer \
    roles/storage.admin \
    roles/iam.serviceAccountUser ; do
  gcloud projects add-iam-policy-binding sangeet-editor \
      --member="serviceAccount:$SA_EMAIL" \
      --role="$ROLE" \
      --condition=None
done
```

### Why each role
| Role | Reason |
|---|---|
| `roles/run.admin` | Deploy new Cloud Run revisions. |
| `roles/cloudbuild.builds.editor` | Submit builds to Cloud Build. |
| `roles/artifactregistry.writer` | Push the built image to the Artifact Registry repo. |
| `roles/storage.admin` | Cloud Build uploads source tarballs to a `<PROJECT>_cloudbuild` GCS bucket — the submitter needs write access to that bucket. |
| `roles/iam.serviceAccountUser` | Cloud Run revisions run as the Compute Engine default SA by default; the deployer needs `actAs` permission on it. |

### Key rotation

```bash
# Create a new key and upload it
KEY_PATH=$(mktemp)
gcloud iam service-accounts keys create "$KEY_PATH" \
    --iam-account=github-deployer@sangeet-editor.iam.gserviceaccount.com
chmod 600 "$KEY_PATH"
gh secret set GCP_SA_KEY < "$KEY_PATH"
shred -u "$KEY_PATH"

# Then disable the old key (find its KEY_ID with `gcloud iam service-accounts keys list ...`)
gcloud iam service-accounts keys disable <OLD_KEY_ID> \
    --iam-account=github-deployer@sangeet-editor.iam.gserviceaccount.com
# After verifying the new key works, delete the old one
gcloud iam service-accounts keys delete <OLD_KEY_ID> \
    --iam-account=github-deployer@sangeet-editor.iam.gserviceaccount.com
```

### If the key leaks

Disable it immediately, then rotate:
```bash
gcloud iam service-accounts keys disable <KEY_ID> \
    --iam-account=github-deployer@sangeet-editor.iam.gserviceaccount.com
```
A disabled key stops working in seconds — much faster than deletion's propagation. Then create + upload a new key as above. Audit `gcloud logging read` for the SA in the leak window to check for unauthorized activity.

### Upgrade path: Workload Identity Federation

The SA-key approach has a long-lived credential. The Google-recommended alternative is Workload Identity Federation: GitHub's OIDC token is exchanged for short-lived (~1h) GCP credentials per run; no static secret in GitHub. ~15 min more setup. Switching later means rewriting the auth step in `deploy-backend.yml` and adding a Workload Identity Pool + Provider on the GCP side.

---

## Troubleshooting

### `gcloud builds submit` fails with `COPY failed: file not found`
The build context didn't include the assembly JAR. Cause: `.gitignore` excludes `target/` and `*.jar`, and gcloud auto-derives `.gcloudignore` from it. **Fix:** use the staging-dir approach in Phase 4a above.

### Swagger UI returns 500 (`ExceptionInInitializerError: META-INF resources are missing`)
sbt-assembly's default merge strategy discarded `META-INF/maven/**`. **Fix:** the explicit `assemblyMergeStrategy` in `build.sbt` (see Phase 3 note). Re-run `sbt sangeetServer/assembly` after editing.

### Local `docker build` fails with `no matching manifest for linux/arm64`
You're on Apple Silicon and the base image is amd64-only. **Fix:** Dockerfile must use `eclipse-temurin:17-jre`, not `eclipse-temurin:17-jre-alpine`.

### Cold start latency feels too slow
First request after idle takes ~1–2 s. Tolerable for personal use. If unacceptable: `--min-instances 1` keeps one warm — but adds ~$10/month and breaks the free tier.

### Out of memory on Cloud Run
JVM `-Xmx400m` + Cloud Run `512Mi` is tight. Increase both:
```bash
# Edit Dockerfile ENTRYPOINT: -Xmx800m
gcloud run services update sangeet-server --memory 1Gi --region asia-south1
```

### CORS errors from the browser
Audit `sangeet-server/src/main/scala/com/varpas/sangeet/server/CorsMiddleware.scala`. The frontend's origin must be in the allow list. `*` is acceptable for a public read-only API.

### "Your active project does not match the quota project" warning
Cosmetic for gcloud CLI usage. Fix only if you write SDK-based code:
```bash
gcloud auth application-default set-quota-project sangeet-editor
```

### Adding a root redirect (`/` → `/docs/`)
Tapir's `endpoint.get` with no path inputs matches **every** GET, not just `/`. If you naively add a Tapir root endpoint, all your existing routes will start returning the redirect. Instead, add the root as a plain http4s route and combine with the Tapir routes via `<+>`:
```scala
private val rootRedirectRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
  case req if req.method == Method.GET && req.uri.path.segments.isEmpty =>
    IO.pure(Response[IO](Status.Found).withHeaders(Location(Uri.unsafeFromString("/docs/"))))
}
// ...
val combined = rootRedirectRoute <+> tapirRoutes
```
Needs `import cats.syntax.semigroupk._` for `<+>`. No new dependency required — `http4s-ember-server` brings everything in.

### Need to see what's running
```bash
gcloud run services list --region asia-south1
gcloud run revisions list --service sangeet-server --region asia-south1
gcloud run services logs read sangeet-server --region asia-south1 --limit 50
```

### Need to inspect the live image
```bash
gcloud artifacts docker images list asia-south1-docker.pkg.dev/sangeet-editor/sangeet
gcloud artifacts docker images describe \
    asia-south1-docker.pkg.dev/sangeet-editor/sangeet/server:latest
```

---

## Cost monitoring

Set a budget alert before traffic grows:

1. GCP Console → Billing → Budgets & alerts → Create budget
2. Scope: project `sangeet-editor`
3. Amount: e.g. ₹100 / $1
4. Alert thresholds: 50%, 90%, 100%
5. Email notification to your account

This is a belt-and-braces safeguard — if Google ever changes the free tier or you accidentally deploy something expensive, you find out before the bill.

---

## Cleanup (stop all charges)

```bash
# Delete the Cloud Run service
gcloud run services delete sangeet-server --region asia-south1

# Delete the Artifact Registry repo (also deletes all images in it)
gcloud artifacts repositories delete sangeet --location asia-south1

# Disable billing on the project (keeps project + IAM, stops all charges)
gcloud beta billing projects unlink sangeet-editor

# Nuclear option: delete the whole project (30-day soft delete window)
gcloud projects delete sangeet-editor
```

---

## Reference — what we actually ran

For the record, this is the exact command sequence used for the initial deploy on 2026-06-10:

```bash
# Phase 1
gcloud config set project sangeet-editor
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com

# Phase 2
gcloud artifacts repositories create sangeet \
    --repository-format=docker --location=asia-south1 \
    --description="Sangeet Notes Editor container images"

# Phase 3
sbt sangeetServer/assembly

# Phase 4
rm -rf /tmp/sangeet-build
mkdir -p /tmp/sangeet-build/sangeet-server/target/scala-3.4.2
cp Dockerfile /tmp/sangeet-build/
cp sangeet-server/target/scala-3.4.2/sangeet-server-assembly-0.2.0.jar \
   /tmp/sangeet-build/sangeet-server/target/scala-3.4.2/
gcloud builds submit /tmp/sangeet-build \
    --tag asia-south1-docker.pkg.dev/sangeet-editor/sangeet/server:latest \
    --region asia-south1

# Phase 5
gcloud run deploy sangeet-server \
    --image asia-south1-docker.pkg.dev/sangeet-editor/sangeet/server:latest \
    --region asia-south1 --platform managed --allow-unauthenticated \
    --memory 512Mi --cpu 1 --port 28080 \
    --min-instances 0 --max-instances 2 --timeout 60
```
