import { useEffect } from 'react'
import { useAuth } from 'react-oidc-context'
import { registerTokenAccessor } from '@/api/axiosClient'
import { clearBackendTokens } from '@/auth/backendAuth'

/**
 * Registers the OIDC access_token getter into the Axios client.
 * Must be called once inside a component that has access to AuthContext.
 */
export function useTokenSync() {
  const auth = useAuth()

  useEffect(() => {
    registerTokenAccessor(() => auth.user?.access_token ?? null)
  }, [auth.user?.access_token])

  // Listen for 401 events from Axios — trigger silent renew
  useEffect(() => {
    const handler = () => {
      auth.signinSilent().catch(() => {
        clearBackendTokens()
        auth.removeUser()
      })
    }
    window.addEventListener('auth:unauthorized', handler)
    return () => window.removeEventListener('auth:unauthorized', handler)
  }, [auth])
}
