import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { setBackendTokens } from '@/auth/backendAuth'
import { FullPageSpinner } from '@/components/ui/Spinner'

/**
 * Receives token and refresh from the backend OAuth2 redirect (after Google sign-in).
 * Stores them and redirects to dashboard.
 */
export function BackendCallbackPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()

  useEffect(() => {
    const token = searchParams.get('token')
    const refresh = searchParams.get('refresh')

    if (token && refresh) {
      setBackendTokens(token, refresh)
      navigate('/dashboard', { replace: true })
    } else {
      navigate('/login?error=missing_tokens', { replace: true })
    }
  }, [searchParams, navigate])

  return <FullPageSpinner />
}

