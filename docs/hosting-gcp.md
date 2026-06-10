# Hosting Sangeet Notes Editor on GCP

This guide walks through deploying the web version of Sangeet Notes Editor to **sangeet-editor.in** on Google Cloud Platform.

## Architecture

```
sangeet-editor.in (GoDaddy domain)
├── Frontend: Firebase Hosting (static Elm app, global CDN)
│   └── sangeet-editor.in → public/index.html, elm.js, styles.css
└── API: Cloud Run (Scala JVM backend)
    └── api.sangeet-editor.in → port 28080 in the container
```

The web app stores compositions in the browser; the API is stateless and serves layout / editor / reference data.

## Estimated cost

| Component | Monthly cost |
|-----------|--------------|
| Firebase Hosting (1 GB transfer / 10 GB storage) | free |
| Cloud Run (scale to zero) | $0–5 |
| Cloud Run (one warm instance) | ~$10 |
| Container Registry | $0.10 / GB stored |
| **Total typical** | **$0–15** |

Scale-to-zero is fine for low-traffic personal use. One warm instance avoids JVM cold-start latency (10–20 s) on first request.

## Prerequisites

- A Google account
- The `gcloud` CLI installed and authenticated: `gcloud auth login`
- The `firebase` CLI installed: `npm install -g firebase-tools` then `firebase login`
- A registered domain (e.g., from GoDaddy)
- Docker installed (only for local image builds — Cloud Build can do this remotely too)

## Phase 1: GCP project setup

```bash
# Create the project
gcloud projects create sangeet-editor --name="Sangeet Notes Editor"
gcloud config set project sangeet-editor

# Link a billing account (required for Cloud Run, even within free tier)
gcloud beta billing accounts list
gcloud beta billing projects link sangeet-editor --billing-account=XXXXXX-XXXXXX-XXXXXX

# Enable required APIs
gcloud services enable run.googleapis.com \
                       artifactregistry.googleapis.com \
                       cloudbuild.googleapis.com \
                       containerregistry.googleapis.com

# Set the default region (Mumbai for India latency)
gcloud config set run/region asia-south1
gcloud config set artifacts/location asia-south1
```

## Phase 2: Backend — Cloud Run

### Build the assembly JAR

```bash
sbt sangeetServer/assembly
```

This produces `sangeet-server/target/scala-3.4.2/sangeet-server-assembly-*.jar`.

> Note: the `assembly` task may need the `sbt-assembly` plugin in `project/plugins.sbt`. Confirm it is present before running.

### Build and push the Docker image

The repo includes a `Dockerfile` at the root. Build and push with Cloud Build (so you don't need Docker locally):

```bash
gcloud builds submit --tag gcr.io/sangeet-editor/server
```

Or build locally and push:

```bash
docker build -t gcr.io/sangeet-editor/server .
docker push gcr.io/sangeet-editor/server
```

### Deploy to Cloud Run

```bash
gcloud run deploy sangeet-server \
  --image gcr.io/sangeet-editor/server \
  --region asia-south1 \
  --allow-unauthenticated \
  --port 28080 \
  --min-instances 0 \
  --max-instances 2 \
  --memory 512Mi \
  --cpu 1
```

Cloud Run returns a URL like `https://sangeet-server-xxxxx-as.a.run.app`. Test it:

```bash
curl https://sangeet-server-xxxxx-as.a.run.app/api/health
```

## Phase 3: Frontend — Firebase Hosting

### Initialize Firebase in the project

```bash
firebase init hosting
```

When prompted:
- Public directory: `sangeet-web/public`
- Configure as single-page app: **No** (Elm app handles routing client-side; configure if needed)
- Set up GitHub Actions deploys: **No** (we configure our own)
- Don't overwrite `index.html`

The repo includes `firebase.json` configured with API rewrites — Firebase will detect it.

### Build the Elm app

```bash
cd sangeet-web
./node_modules/.bin/elm make src/Main.elm --optimize --output=public/elm.js
```

### Deploy

```bash
firebase deploy --only hosting --project sangeet-editor
```

Firebase prints a hosting URL like `https://sangeet-editor.web.app`. Visit it to confirm the app loads.

## Phase 4: Custom domain (GoDaddy → GCP)

### Frontend domain (sangeet-editor.in)

1. In the Firebase Console → Hosting → Add custom domain → enter `sangeet-editor.in`
2. Firebase provides two A records — copy them
3. In GoDaddy DNS → add the A records for `@` (root)
4. Add a CNAME record: `www` → `sangeet-editor.in`
5. Wait 5–60 minutes for DNS propagation
6. Firebase auto-provisions a Let's Encrypt SSL certificate

### API subdomain (api.sangeet-editor.in)

```bash
gcloud run domain-mappings create \
  --service sangeet-server \
  --domain api.sangeet-editor.in \
  --region asia-south1
```

GCP returns one or more CNAME / A records. Add them to GoDaddy DNS for the `api` subdomain. Wait for propagation. Verify:

```bash
curl https://api.sangeet-editor.in/api/health
```

### Update Elm app's API base URL

The Elm app needs to know to call `api.sangeet-editor.in` in production instead of `localhost:28080`. Two options:

**A)** Hard-code via a build flag passed at `elm make` time.
**B)** Inject via a config script `public/config.js` loaded before `elm.js`:

```javascript
// public/config.js
window.SANGEET_API_BASE_URL = "https://api.sangeet-editor.in";
```

Then in `public/index.html`:

```html
<script src="config.js"></script>
<script src="elm.js"></script>
<script>
  var app = Elm.Main.init({
    node: document.getElementById('elm'),
    flags: { apiBaseUrl: window.SANGEET_API_BASE_URL || "http://localhost:28080" }
  });
</script>
```

For local dev, omit `config.js` — the flag defaults to `localhost:28080`.

## Phase 5: CI/CD for deployments

The repo includes `.github/workflows/deploy.yml` which:

1. Runs on push to `main` after CI passes
2. Builds the Elm app (`elm make --optimize`)
3. Builds the server JAR (`sbt sangeetServer/assembly`)
4. Builds and pushes the Docker image to GCR
5. Deploys to Cloud Run
6. Deploys to Firebase Hosting

### Required GitHub secrets

| Secret | How to obtain |
|--------|---------------|
| `GCP_SERVICE_ACCOUNT_KEY` | Create a service account in GCP IAM with roles: Cloud Run Admin, Storage Admin, Cloud Build Editor. Generate a JSON key and paste its contents here. |
| `FIREBASE_TOKEN` | Run `firebase login:ci` and copy the token. |
| `GCP_PROJECT_ID` | `sangeet-editor` |

Add these in GitHub → Settings → Secrets and variables → Actions.

## Phase 6: Monitoring

- **Cloud Run logs:** `gcloud run services logs read sangeet-server --region asia-south1`
- **Cloud Run metrics:** GCP Console → Cloud Run → sangeet-server → Metrics tab
- **Firebase hosting traffic:** Firebase Console → Hosting → Usage

For more detail, set up Cloud Monitoring alerts (out of scope for this guide).

## Troubleshooting

**Cloud Run cold starts are slow** — set `--min-instances 1` to keep one warm. Adds ~$10/month.

**Elm app can't reach API** — check CORS in `sangeet-server/CorsMiddleware.scala`. The default allows `*` which is fine for the public deployment.

**Custom domain SSL pending** — wait 24 hours. Firebase and Cloud Run both provision Let's Encrypt certs in the background.

**Image too large** — Cloud Run has a 32 GB image limit but pulls speed matters. Use a slim base image (the included `Dockerfile` uses `eclipse-temurin:17-jre-alpine` for this reason).

**JVM out of memory** — increase Cloud Run memory: `gcloud run services update sangeet-server --memory 1Gi --region asia-south1`.

## Cleanup (to stop charges)

```bash
# Delete Cloud Run service
gcloud run services delete sangeet-server --region asia-south1

# Delete Firebase hosting site (or just unbind the custom domain)
firebase hosting:disable --project sangeet-editor

# Delete the project entirely
gcloud projects delete sangeet-editor
```

---

For desktop releases (cross-platform installers), see `.github/workflows/release.yml` (auto-builds `.dmg`, `.msi`, `.deb` on tag push).
