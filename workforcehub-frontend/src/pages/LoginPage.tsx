import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from 'react-oidc-context'
import { buildOidcSettings } from '@/auth/oidcConfig'
import { UserManager } from 'oidc-client-ts'
import { getBackendAccessToken } from '@/auth/backendAuth'
import { Button } from '@/components/ui/Button'
import { FullPageSpinner } from '@/components/ui/Spinner'
import { Zap, ArrowRight, Shield, Globe2, GitBranch } from 'lucide-react'

// Google SVG icon
function GoogleIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
      <path d="M17.64 9.205c0-.639-.057-1.252-.164-1.841H9v3.481h4.844a4.14 4.14 0 0 1-1.796 2.716v2.259h2.908c1.702-1.567 2.684-3.875 2.684-6.615Z" fill="#4285F4" />
      <path d="M9 18c2.43 0 4.467-.806 5.956-2.18l-2.908-2.259c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 0 0 9 18Z" fill="#34A853" />
      <path d="M3.964 10.71A5.41 5.41 0 0 1 3.682 9c0-.593.102-1.17.282-1.71V4.958H.957A8.996 8.996 0 0 0 0 9c0 1.452.348 2.827.957 4.042l3.007-2.332Z" fill="#FBBC05" />
      <path d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 0 0 .957 4.958L3.964 7.29C4.672 5.163 6.656 3.58 9 3.58Z" fill="#EA4335" />
    </svg>
  )
}

function GitHubIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
      <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0 0 24 12c0-6.63-5.37-12-12-12Z" />
    </svg>
  )
}

export function LoginPage() {
  const auth = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    if (auth.isAuthenticated || getBackendAccessToken()) navigate('/dashboard', { replace: true })
  }, [auth.isAuthenticated, navigate])

  if (auth.isLoading) return <FullPageSpinner />

  const apiBase = import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8080'

  const loginWith = async (provider: 'google' | 'github' | 'default') => {
    if (provider === 'google') {
      // Server-side flow: backend exchanges code with client_secret, then redirects here with tokens
      window.location.href = `${apiBase}/auth/login/google`
      return
    }
    if (provider === 'default') {
      auth.signinRedirect()
      return
    }
    // GitHub: still use OIDC (via Keycloak broker if configured)
    const settings = buildOidcSettings('github')
    const mgr = new UserManager(settings)
    await mgr.signinRedirect()
  }

  const hasCustomIdp = !!import.meta.env.VITE_OIDC_AUTHORITY

  return (
    <div className="min-h-screen bg-surface-0 bg-grid-lines bg-grid flex">
      {/* Left panel — branding */}
      <div className="hidden lg:flex w-1/2 flex-col justify-between p-14 border-r border-surface-3 relative overflow-hidden">
        {/* Ambient glow */}
        <div className="absolute -top-32 -left-32 w-96 h-96 bg-brand/10 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute -bottom-32 right-0 w-80 h-80 bg-accent-teal/8 rounded-full blur-3xl pointer-events-none" />

        <div className="flex items-center gap-3 relative">
          <div className="w-10 h-10 rounded-xl bg-brand flex items-center justify-center shadow-glow-brand">
            <Zap size={20} className="text-white" />
          </div>
          <span className="text-lg font-semibold text-ink-bright">WorkforceHub</span>
        </div>

        <div className="relative space-y-6">
          <h1 className="font-display text-5xl text-ink-bright leading-tight">
            Workforce data,<br />
            <span className="italic text-brand-bright">at scale.</span>
          </h1>
          <p className="text-ink-dim text-lg leading-relaxed max-w-sm">
            Upload, process, and query your employee data across your entire organisation — securely and instantly.
          </p>

          <div className="space-y-3 pt-2">
            {[
              { icon: Shield, text: 'Enterprise-grade security with OAuth2 + JWT' },
              { icon: Globe2, text: 'Multi-tenant data isolation by design' },
              { icon: Zap, text: 'Bulk CSV/Excel ingestion via Kafka' },
            ].map(({ icon: Icon, text }) => (
              <div key={text} className="flex items-center gap-3 text-ink-dim">
                <div className="w-6 h-6 rounded-lg bg-surface-3 flex items-center justify-center flex-shrink-0">
                  <Icon size={13} className="text-brand" />
                </div>
                <span className="text-sm">{text}</span>
              </div>
            ))}
          </div>
        </div>

        <p className="text-xs text-ink-muted relative">© 2024 WorkforceHub. Enterprise SaaS platform.</p>
      </div>

      {/* Right panel — login */}
      <div className="flex-1 flex items-center justify-center p-8">
        <div className="w-full max-w-sm space-y-8 animate-fade-up">
          {/* Mobile logo */}
          <div className="flex items-center gap-2.5 lg:hidden">
            <div className="w-8 h-8 rounded-lg bg-brand flex items-center justify-center">
              <Zap size={16} className="text-white" />
            </div>
            <span className="font-semibold text-ink-bright">WorkforceHub</span>
          </div>

          <div>
            <h2 className="text-2xl font-display text-ink-bright mb-2">Sign in</h2>
            <p className="text-sm text-ink-muted">Choose your identity provider to continue</p>
          </div>

          <div className="space-y-3">
            {/* Google (server-side flow via backend — no client_id needed in frontend) */}
            {!hasCustomIdp && (
              <button
                onClick={() => loginWith('google')}
                className="w-full flex items-center gap-3 px-5 py-3.5 bg-surface-2 border border-surface-4 hover:border-surface-5 hover:bg-surface-3 rounded-xl text-sm font-medium text-ink transition-all duration-150 group"
              >
                <GoogleIcon />
                <span className="flex-1 text-left">Continue with Google</span>
                <ArrowRight size={15} className="text-ink-muted group-hover:text-ink group-hover:translate-x-0.5 transition-all" />
              </button>
            )}

            {/* GitHub (via IdP broker) */}
            {import.meta.env.VITE_OIDC_GITHUB_CLIENT_ID && (
              <button
                onClick={() => loginWith('github')}
                className="w-full flex items-center gap-3 px-5 py-3.5 bg-surface-2 border border-surface-4 hover:border-surface-5 hover:bg-surface-3 rounded-xl text-sm font-medium text-ink transition-all duration-150 group"
              >
                <GitHubIcon />
                <span className="flex-1 text-left">Continue with GitHub</span>
                <ArrowRight size={15} className="text-ink-muted group-hover:text-ink group-hover:translate-x-0.5 transition-all" />
              </button>
            )}

            {/* Custom IdP (Keycloak / Auth0) */}
            {hasCustomIdp && (
              <button
                onClick={() => loginWith('default')}
                className="w-full flex items-center gap-3 px-5 py-3.5 bg-brand/10 border border-brand/30 hover:bg-brand/20 hover:border-brand/50 rounded-xl text-sm font-medium text-brand-bright transition-all duration-150 group"
              >
                <Shield size={18} />
                <span className="flex-1 text-left">Continue with SSO</span>
                <ArrowRight size={15} className="group-hover:translate-x-0.5 transition-transform" />
              </button>
            )}

            {/* Fallback when no provider is available (backend Google is always available when Docker is up) */}
            {hasCustomIdp && !import.meta.env.VITE_OIDC_AUTHORITY && (
              <div className="bg-accent-amber/10 border border-accent-amber/30 rounded-xl p-4 text-xs text-accent-amber space-y-1">
                <p className="font-semibold">No OAuth2 provider configured</p>
                <p>Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET in the backend, or configure a custom IdP.</p>
              </div>
            )}
          </div>

          <p className="text-xs text-ink-muted text-center leading-relaxed">
            All authentication uses PKCE — your credentials never touch this app.
          </p>
        </div>
      </div>
    </div>
  )
}
