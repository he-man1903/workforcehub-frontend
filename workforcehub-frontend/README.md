# WorkforceHub Frontend

Production-ready React frontend for the WorkforceHub SaaS platform.

**Stack:** React 18 · Vite · TypeScript · Tailwind CSS · React Query · OIDC (PKCE) · Axios

---

## Quick Start

```bash
cp .env.example .env       # Fill in your values (see below)
npm install
npm run dev                # http://localhost:5173
```

---

## Environment Variables

```env
# API Gateway
VITE_API_GATEWAY_URL=http://localhost:8080

# Google OAuth2 (PKCE)
VITE_OIDC_GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
VITE_OIDC_GOOGLE_AUTHORITY=https://accounts.google.com

# GitHub (via Keycloak broker or similar IdP)
VITE_OIDC_GITHUB_CLIENT_ID=your-github-app-client-id
VITE_OIDC_GITHUB_AUTHORITY=https://your-keycloak/realms/workforcehub

# Custom IdP (Keycloak / Auth0) — takes precedence
# VITE_OIDC_AUTHORITY=http://localhost:8180/realms/workforcehub
# VITE_OIDC_CLIENT_ID=workforcehub-frontend

VITE_APP_URL=http://localhost:5173
```

---

## OAuth2 Setup

### Google
1. Go to [Google Cloud Console](https://console.cloud.google.com) → APIs → Credentials
2. Create OAuth 2.0 Client ID → Web Application
3. Add `http://localhost:5173/auth/callback` to Authorized redirect URIs
4. Copy Client ID → `VITE_OIDC_GOOGLE_CLIENT_ID`

### GitHub (via Keycloak)
GitHub doesn't support OIDC natively. Use Keycloak with GitHub as an Identity Provider:
1. Set up Keycloak realm `workforcehub`
2. Add GitHub as an Identity Provider in Keycloak
3. Create a public client `workforcehub-frontend` with PKCE enabled
4. Set `VITE_OIDC_AUTHORITY=http://localhost:8180/realms/workforcehub`
5. Set `VITE_OIDC_CLIENT_ID=workforcehub-frontend`

### Auth0
1. Create a Single Page Application
2. Add `http://localhost:5173/auth/callback` to Allowed Callback URLs
3. Set `VITE_OIDC_AUTHORITY=https://YOUR_DOMAIN.auth0.com`
4. Set `VITE_OIDC_CLIENT_ID=your-auth0-client-id`

---

## Architecture

```
src/
├── api/
│   ├── axiosClient.ts     # Axios instance with JWT Bearer interceptor
│   ├── employees.ts       # Employee API calls
│   └── uploads.ts         # Upload API calls
├── auth/
│   ├── oidcConfig.ts      # OIDC provider configs (PKCE)
│   └── useAuthUser.ts     # Typed user hook
├── components/
│   ├── layout/            # Sidebar, AppLayout
│   ├── ui/                # Badge, Button, Card, Spinner, StatCard
│   ├── employees/         # EmployeeTable with pagination + filter
│   └── upload/            # FileUploadZone with drag-drop + progress
├── hooks/
│   └── useTokenSync.ts    # Wires OIDC token → Axios; handles 401 renew
├── pages/
│   ├── LoginPage.tsx      # OAuth2 provider selection
│   ├── DashboardPage.tsx  # Stats + quick upload
│   ├── EmployeesPage.tsx  # Full employee table
│   ├── UploadsPage.tsx    # Upload history + new upload
│   └── AuthCallbackPage.tsx # OIDC redirect handler
└── types/index.ts         # All TypeScript types
```

## Auth Flow

```
User → Login page → Clicks "Continue with Google"
     → Redirect to Google (PKCE code challenge)
     → Google → /auth/callback (code + state)
     → oidc-client-ts exchanges code for tokens (PKCE verify)
     → access_token stored in memory (not localStorage)
     → Axios interceptor attaches Bearer token on every request
     → Gateway validates JWT, injects X-Tenant-Id header
     → 401 response → silent renew via iframe → retry
```

## Build for Production

```bash
npm run build    # outputs to dist/
```

Set `VITE_APP_URL` to your production domain and register it as a redirect URI in your OAuth2 provider.
