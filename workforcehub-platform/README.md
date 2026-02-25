## WorkforceHub – Full Stack (Frontend + Platform)

Production-ready **full‑stack** WorkforceHub setup:

- **React frontend** (separate project, talks to this platform via gateway)
- **Java 21 / Spring Boot 3** microservices
- **Google OAuth 2.0** login with backend‑issued JWTs
- **Postgres, Redis, Kafka, Prometheus, Grafana** orchestrated by Docker Compose

This `workforcehub-platform` folder contains the **backend platform**. The React app usually lives in a sibling folder like `../workforcehub-frontend`.

---

## High‑level system architecture

```text
[ User Browser ]
       │
       │  HTTPS (React SPA)
       ▼
[ React Frontend (Vite, port 5173) ]
       │  REST / JSON, Authorization: Bearer <JWT>
       ▼
[ API Gateway (Spring Boot, port 8080) ]
       │
       ├──► Auth Service (port 8084)
       │       - Google OAuth2 (server-side)
       │       - Issues internal JWT access/refresh tokens
       │
       ├──► Upload Service (port 8081)
       │       - File ingestion (CSV/Excel)
       │
       ├──► Processing Service (port 8082)
       │       - Async processing via Kafka
       │
       └──► Query Service (port 8083)
               - Read/query APIs

Infra:
  - PostgreSQL (workforce DB)
  - Redis (OAuth state, cache)
  - Kafka + Zookeeper (async jobs)
  - Prometheus + Grafana (metrics / dashboards)
```

---

## Repository layout (backend)

```text
workforcehub-platform/
├── workforce-auth-service/        # Auth + Google OAuth2 → internal JWTs
├── workforce-gateway/             # API gateway (public entrypoint, JWT validation)
├── workforce-upload-service/      # File ingestion
├── workforce-processing-service/  # Async processing + Kafka
├── workforce-query-service/       # Query API
├── db/
│   └── init.sql                   # PostgreSQL schema & seed data
├── observability/
│   └── prometheus.yml             # Prometheus config
├── docker-compose.yml             # Full local stack (infra + all services)
└── .env.example                   # Example environment variables
```

### Core services & ports

| Service                         | Port (host) | Description                                      |
|---------------------------------|-------------|--------------------------------------------------|
| `workforce-gateway`             | 8080        | Public API gateway                               |
| `workforce-auth-service`        | 8084        | Auth API, Google OAuth2, Swagger UI              |
| `workforce-upload-service`      | 8081        | Upload workforce data                            |
| `workforce-processing-service`  | 8082        | Async processing + Kafka                         |
| `workforce-query-service`       | 8083        | Query employees / data                           |
| `postgres`                      | 5432        | PostgreSQL                                       |
| `redis`                         | 6379        | Redis (OAuth state, caches, etc.)               |
| `prometheus`                    | 9090        | Metrics / monitoring                             |
| `grafana`                       | 3000        | Dashboards (`admin` / `admin`)                  |

---

## Frontend architecture (overview)

> The frontend lives in a separate project, e.g. `../workforcehub-frontend`. This section describes how it integrates with this backend.

- **SPA & routing**
  - React SPA (often Vite) running at `http://localhost:5173`.
  - Uses React Router with routes such as:
    - `/login` – login screen, “Continue with Google” button
    - `/dashboard` – main app UI
    - `/auth/backend-callback` – receives backend JWTs after Google login
- **Auth integration**
  - “Continue with Google” button sends browser to the gateway:
    - `GET http://localhost:8080/auth/login/google`
  - After Google login, the backend redirects to:
    - `http://localhost:5173/auth/backend-callback?token=<ACCESS>&refresh=<REFRESH>`
  - Frontend stores these tokens (e.g. `sessionStorage`) and treats user as authenticated.
  - Protected routes use a `RequireAuth` component that checks for a valid backend access token.
- **API client**
  - Central Axios client with an interceptor that:
    - Reads the backend access token.
    - Adds `Authorization: Bearer <token>` to all API calls towards `http://localhost:8080`.

---

## Backend architecture (microservices)

### Gateway (`workforce-gateway`)

- Single public entrypoint: `http://localhost:8080`
- Validates JWTs on incoming requests:
  - If valid, extracts claims (e.g. email) and adds headers like `X_USER_EMAIL`
  - Forwards the request to the appropriate internal service
- Routes (simplified):
  - `/auth/**` → auth service
  - `/upload/**` → upload service
  - `/query/**` → query service

### Auth Service (`workforce-auth-service`)

- Responsible for:
  - Google OAuth 2.0 login
  - Exchanging authorization code for tokens with Google (server‑side, using `client_secret`)
  - Verifying `id_token` and logging the user into WorkforceHub
  - Issuing **internal** JWT access and refresh tokens
- Key endpoints:
  - `GET /auth/login/google`
    - Builds Google authorization URL
    - Stores `state` in Redis via `OAuth2StateStore`
    - Redirects browser to Google
  - `GET /auth/oauth2/callback`
    - Validates `state`
    - Calls Google token endpoint with `code`, `client_id`, `client_secret`
    - Verifies `id_token`
    - Creates / loads WorkforceHub user
    - Issues internal JWTs
    - Redirects browser to frontend:
      - `/auth/backend-callback?token=<ACCESS>&refresh=<REFRESH>`
  - `GET /auth/me`
    - Returns current user details based on the JWT

### Domain services

- **Upload Service**
  - Accepts CSV/Excel uploads
  - Stores upload jobs and raw rows in Postgres
  - Often produces events to Kafka for downstream processing
- **Processing Service**
  - Consumes Kafka events
  - Performs data normalization / enrichment
  - Writes normalized records into Postgres
- **Query Service**
  - Exposes read‑only APIs for searching/filtering workforce data
  - Reads from the same Postgres database and can use Redis for caching

### Infrastructure

- **PostgreSQL**
  - Main relational database
  - Schema defined in `db/init.sql`
- **Redis**
  - Stores OAuth state (anti‑CSRF for Google flow)
  - Can be extended for caching
- **Kafka + Zookeeper**
  - Message broker for decoupling uploads and processing
- **Prometheus & Grafana**
  - Services expose metrics via `/actuator/prometheus`
  - Prometheus scrapes metrics, Grafana visualizes dashboards

---

## Google OAuth 2.0 login flow (end‑to‑end)

1. **User clicks “Continue with Google”** in the React app.
2. Frontend redirects browser to the gateway:
   - `GET http://localhost:8080/auth/login/google`
3. Gateway forwards to the auth service, which:
   - Generates a Google OAuth URL with:
     - `client_id = GOOGLE_CLIENT_ID`
     - `redirect_uri = http://localhost:8080/auth/oauth2/callback`
     - `state` (stored in Redis)
   - Redirects browser to Google.
4. **User signs in on Google** and consents.
5. Google redirects back to:
   - `http://localhost:8080/auth/oauth2/callback?code=...&state=...`
6. Auth service:
   - Validates `state` via Redis.
   - Calls Google token endpoint with:
     - `code`, `client_id`, `client_secret`, `redirect_uri`
   - Verifies the returned `id_token` (signature, audience, expiry).
   - Logs in or creates the corresponding WorkforceHub user.
   - Issues **internal** JWT access & refresh tokens.
7. Auth service redirects browser to frontend:
   - `http://localhost:5173/auth/backend-callback?token=<ACCESS>&refresh=<REFRESH>`
8. Frontend:
   - Stores tokens (e.g. `sessionStorage`).
   - Marks the user as authenticated.
   - Navigates user to `/dashboard` and calls the gateway with:
     - `Authorization: Bearer <ACCESS_TOKEN>`

---

## Environment configuration

### 1. Backend `.env`

In `workforcehub-platform/`, create your `.env` from the example:

```bash
cp .env.example .env
# On Windows PowerShell:
# copy .env.example .env
```

Open `.env` and fill in at least:

- `GOOGLE_CLIENT_ID` – from your Google Cloud OAuth 2.0 **Web application** client
- `GOOGLE_CLIENT_SECRET` – from the same client
- `FRONTEND_URL` – usually `http://localhost:5173`
- `GATEWAY_PUBLIC_URL` – usually `http://localhost:8080`
- (Optional) `GOOGLE_ALLOWED_DOMAINS` – restrict login by domain (comma‑separated)
- (Optional) `CORS_ALLOWED_ORIGINS` – defaults to `http://localhost:5173`

These are consumed by `docker-compose.yml` and the Spring Boot services at startup.

### 2. Google Cloud Console configuration

In [Google Cloud Console](https://console.cloud.google.com):

1. Go to **APIs & Services → Credentials**.
2. Create (or edit) an **OAuth 2.0 Client ID** of type **Web application**.
3. Under **Authorized redirect URIs**, add **exactly**:

   ```text
   http://localhost:8080/auth/oauth2/callback
   ```

4. Under **Authorized JavaScript origins**, ensure your frontend origin is present:

   ```text
   http://localhost:5173
   ```

5. Copy the **Client ID** and **Client secret** into `.env` as `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`.

---

## Running the backend with Docker Compose (recommended)

From `workforcehub-platform/`:

```bash
docker compose up --build
```

This starts:

- Postgres, Redis, Kafka, Prometheus, Grafana
- Auth service, gateway, upload, processing, query services

Once everything is running:

- **Gateway (public API):** `http://localhost:8080`
- **Auth Swagger UI:** `http://localhost:8084/swagger-ui.html`
- **Prometheus:** `http://localhost:9090`
- **Grafana:** `http://localhost:3000` (`admin` / `admin`)

Your React frontend should be running separately (typically in `../workforcehub-frontend`) and configured to call the gateway at `http://localhost:8080`.

---

## Running services without Docker (optional)

If you prefer local dev without containers:

1. Start infra with Docker only:

   ```bash
   docker compose up postgres redis kafka -d
   ```

2. Run each service with the Gradle wrapper (example):

   ```bash
   # Auth service
   cd workforce-auth-service
   ../gradlew bootRun

   # Gateway
   cd workforce-gateway
   ../gradlew bootRun

   # Upload / Processing / Query similarly in their own terminals
   ```

Ensure your local configs (`application.yml`, env vars) match the database and Redis connection details used by Docker.

---

## Tech stack

- **Frontend**
  - React, TypeScript, Vite (SPA)
  - React Router for routing
  - Axios for API calls with JWT
- **Backend**
  - Java 21, Spring Boot 3
  - Spring Security, Spring Web, Spring Data JPA
  - JWT authentication
  - PostgreSQL, Redis, Kafka
  - Docker & Docker Compose
  - Prometheus & Grafana

# WorkforceHub Platform

Production-grade Java 21 / Spring Boot 3 microservice platform for workforce data management.

## Architecture

```
workforcehub-platform/
├── workforce-upload-service/      # Port 8081 — File ingestion
├── workforce-processing-service/  # Port 8082 — Async processing + Kafka
├── workforce-query-service/       # Port 8083 — Query API
├── db/
│   └── init.sql                   # PostgreSQL schema
└── docker-compose.yml             # Full local stack
```

## Services

| Service | Port | Description |
|---|---|---|
| workforce-upload-service | 8081 | Accepts CSV/Excel uploads, returns uploadId |
| workforce-processing-service | 8082 | Consumes Kafka events, batch inserts to DB |
| workforce-query-service | 8083 | Paginated queries for employees |

## Quick Start

### Google OAuth (required for login)

1. In [Google Cloud Console](https://console.cloud.google.com) create an **OAuth 2.0 Client ID** (Web application).
2. **Authorized redirect URIs**: add `http://localhost:8080/auth/oauth2/callback` (gateway URL).
3. **Authorized JavaScript origins**: add `http://localhost:5173` (frontend).
4. **One-time env setup** (in this folder, next to `docker-compose.yml`):
   ```bash
   copy env.local.example .env
   ```
   Then open `.env` and replace `REPLACE_WITH_YOUR_CLIENT_ID` and `REPLACE_WITH_YOUR_CLIENT_SECRET` with your real values from the Google OAuth client.

### Option 1: Docker Compose (recommended)

```bash
docker-compose up --build
```

### Option 2: Run locally

1. Start PostgreSQL and Kafka (or use docker-compose for infra only):

```bash
docker-compose up postgres kafka -d
```

2. Run each service:

```bash
# Terminal 1
cd workforce-upload-service
../gradlew bootRun

# Terminal 2
cd workforce-processing-service
../gradlew bootRun

# Terminal 3
cd workforce-query-service
../gradlew bootRun
```

## API Endpoints

### Upload Service (port 8081)
- `POST /api/v1/uploads` — Upload CSV or Excel file
- `GET  /api/v1/uploads/{id}` — Get upload job status
- `GET  /api/v1/uploads` — List all upload jobs (paginated)
- `GET  /swagger-ui.html` — Swagger UI

### Query Service (port 8083)
- `GET /api/v1/employees/{id}` — Get employee by ID
- `GET /api/v1/employees?status=ACTIVE` — List employees with filter
- `GET /swagger-ui.html` — Swagger UI

## Health Checks

All services expose:
- `GET /actuator/health`
- `GET /actuator/metrics`
- `GET /actuator/prometheus`

## Database Schema

See `db/init.sql` for the full schema including:
- `upload_jobs` — Upload job tracking with soft delete
- `employees` — Employee records with indexing
- `upload_job_rows` — Per-row audit trail with JSONB raw data

## Tech Stack

- Java 21
- Spring Boot 3.3.2
- Spring Data JPA
- Spring Kafka
- PostgreSQL 16
- Gradle 8.8
- Springdoc OpenAPI (Swagger UI)
- Logstash Logback Encoder (structured JSON logs)
- Lombok

