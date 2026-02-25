import { useAuth } from 'react-oidc-context'
import { useQuery } from '@tanstack/react-query'
import type { AuthUser } from '@/types'
import { getBackendAccessToken } from '@/auth/backendAuth'
import { fetchMe } from '@/api/auth'

/**
 * Returns a typed AuthUser from either OIDC (Keycloak/GitHub) or backend-only session (Google server-side flow).
 * When only a backend JWT is present, fetches /auth/me to get user info.
 */
export function useAuthUser(): { user: AuthUser | null; token: string | null; isLoading: boolean } {
  const auth = useAuth()
  const backendToken = getBackendAccessToken()

  const { data: me, isLoading: meLoading } = useQuery({
    queryKey: ['auth', 'me', backendToken ?? ''],
    queryFn: fetchMe,
    enabled: !!backendToken && !auth.user,
    staleTime: 5 * 60 * 1000,
  })

  if (auth.isLoading && !backendToken) return { user: null, token: null, isLoading: true }
  if (backendToken && !auth.user) {
    if (meLoading || !me) return { user: null, token: backendToken, isLoading: true }
    const user: AuthUser = {
      id: me.id,
      email: me.email ?? '',
      name: me.email || 'User',
      tenantId: me.tenantId,
      role: me.role,
      avatarUrl: undefined,
    }
    return { user, token: backendToken, isLoading: false }
  }

  if (!auth.isAuthenticated || !auth.user) return { user: null, token: null, isLoading: false }

  const profile = auth.user.profile
  const token = auth.user.access_token
  const user: AuthUser = {
    id: profile.sub ?? '',
    email: (profile.email as string) ?? '',
    name: (profile.name as string) ?? (profile.email as string) ?? 'User',
    tenantId: (profile['tenantId'] as string) ?? (profile['tenant_id'] as string) ?? profile.sub ?? '',
    role: (profile['role'] as string) ?? 'USER',
    avatarUrl: (profile.picture as string) ?? undefined,
  }

  return { user, token, isLoading: false }
}
