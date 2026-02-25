import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { ApiError } from '@/types'
import { getBackendAccessToken } from '@/auth/backendAuth'

// Token accessor — set by auth context on login (OIDC token; used when backend JWT not set)
let _getToken: (() => string | null) | null = null

export function registerTokenAccessor(fn: () => string | null) {
  _getToken = fn
}

const client = axios.create({
  baseURL: import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
  timeout: 30_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// ── Request interceptor — attach Bearer token (backend JWT preferred) ───────
client.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getBackendAccessToken() ?? _getToken?.()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ── Response interceptor — normalise errors ────────────────────────────────
client.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiError>) => {
    const status = error.response?.status

    if (status === 401) {
      // Token expired / invalid — OIDC automaticSilentRenew should handle this.
      // If it can't, redirect to login.
      window.dispatchEvent(new CustomEvent('auth:unauthorized'))
    }

    return Promise.reject(error)
  }
)

export default client
