import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from 'react-oidc-context'
import { FullPageSpinner } from '@/components/ui/Spinner'
import { setBackendTokens } from '@/auth/backendAuth'

const API_BASE = import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8080'

/**
 * Landing page for the OAuth2 redirect_uri.
 * react-oidc-context handles the code exchange automatically;
 * we then exchange Google id_token for backend JWT (if backend is up) and redirect.
 */
export function AuthCallbackPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const [backendExchangeDone, setBackendExchangeDone] = useState(false)
  const exchangeStarted = useRef(false)

  useEffect(() => {
    if (auth.isLoading) return

    if (!auth.isAuthenticated || !auth.user) {
      // Show error below if any; still redirect to login after a short delay so user can read it
      if (auth.error) {
        console.error('OIDC callback error:', auth.error)
      }
      const t = setTimeout(() => navigate('/login', { replace: true }), auth.error ? 8000 : 0)
      return () => clearTimeout(t)
    }

    const idToken = (auth.user as { id_token?: string }).id_token
    if (!idToken) {
      navigate('/dashboard', { replace: true })
      return
    }

    if (backendExchangeDone) {
      navigate('/dashboard', { replace: true })
      return
    }
    if (exchangeStarted.current) return
    exchangeStarted.current = true

    fetch(`${API_BASE}/auth/google`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ idToken }),
    })
      .then((res) => {
        if (res.ok) return res.json()
        throw new Error(`Backend auth failed: ${res.status}`)
      })
      .then((data: { accessToken: string; refreshToken: string }) => {
        setBackendTokens(data.accessToken, data.refreshToken)
        setBackendExchangeDone(true)
        navigate('/dashboard', { replace: true })
      })
      .catch((err) => {
        console.warn('Backend token exchange failed (is Docker up?); continuing with OIDC token.', err)
        setBackendExchangeDone(true)
        navigate('/dashboard', { replace: true })
      })
  }, [auth.isLoading, auth.isAuthenticated, auth.user, auth.error, backendExchangeDone, navigate])

  if (auth.error) {
    return (
      <div className="min-h-screen bg-surface-0 flex items-center justify-center p-6">
        <div className="max-w-md w-full bg-surface-2 border border-surface-4 rounded-xl p-6 space-y-4">
          <h2 className="text-lg font-semibold text-ink-bright">Sign-in problem</h2>
          <p className="text-sm text-ink-dim">
            The login callback failed. This often happens when the browser cannot complete the token
            exchange with Google (e.g. CORS or network). Ensure Docker is running so the backend can
            help with auth, and that your Google OAuth client has the correct redirect URIs.
          </p>
          <pre className="text-xs bg-surface-3 p-3 rounded-lg overflow-auto text-ink-dim">
            {auth.error.message}
          </pre>
          <p className="text-xs text-ink-muted">
            Redirecting to login in a few seconds, or <a href="/login" className="text-brand underline">go now</a>.
          </p>
        </div>
      </div>
    )
  }

  return <FullPageSpinner />
}
