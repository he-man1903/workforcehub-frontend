const STORAGE_KEY_ACCESS = 'workforcehub_access_token'
const STORAGE_KEY_REFRESH = 'workforcehub_refresh_token'

/**
 * Backend JWT tokens (from server-side OAuth or POST /auth/google). When set, API calls use these
 * so the gateway can validate them. Persisted in sessionStorage so refresh survives page reload.
 */
let accessToken: string | null = null
let refreshToken: string | null = null

function persist() {
  try {
    if (accessToken) sessionStorage.setItem(STORAGE_KEY_ACCESS, accessToken)
    else sessionStorage.removeItem(STORAGE_KEY_ACCESS)
    if (refreshToken) sessionStorage.setItem(STORAGE_KEY_REFRESH, refreshToken)
    else sessionStorage.removeItem(STORAGE_KEY_REFRESH)
  } catch {
    // ignore
  }
}

export function setBackendTokens(access: string, refresh: string) {
  accessToken = access
  refreshToken = refresh
  persist()
}

export function getBackendAccessToken(): string | null {
  if (accessToken) return accessToken
  try {
    accessToken = sessionStorage.getItem(STORAGE_KEY_ACCESS)
    return accessToken
  } catch {
    return null
  }
}

export function getBackendRefreshToken(): string | null {
  if (refreshToken) return refreshToken
  try {
    refreshToken = sessionStorage.getItem(STORAGE_KEY_REFRESH)
    return refreshToken
  } catch {
    return null
  }
}

export function clearBackendTokens() {
  accessToken = null
  refreshToken = null
  persist()
}

